# @file CtasHack.R
#
# Copyright 2021 Observational Health Data Sciences and Informatics
#
# This file is part of DatabaseConnector
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


mergeTempTables <- function(connection, tableName, sqlFieldNames, sourceNames, distribution, tempEmulationSchema) {
  unionString <- paste("\nUNION ALL\nSELECT ", sqlFieldNames, " FROM ", sep = "")
  valueString <- paste(sourceNames, collapse = unionString)
  sql <- paste(distribution,
               "\n",
               "SELECT ",
               sqlFieldNames,
               " INTO ",
               tableName,
               " FROM ",
               valueString,
               ";",
               sep = "")
  sql <- SqlRender::translate(sql, targetDialect = connection@dbms, tempEmulationSchema = tempEmulationSchema)
  executeSql(connection, sql, progressBar = FALSE, reportOverallTime = FALSE)
  
  # Drop source tables:
  for (sourceName in sourceNames) {
    sql <- paste("DROP TABLE", sourceName)
    sql <- SqlRender::translate(sql, targetDialect = connection@dbms, tempEmulationSchema = tempEmulationSchema)
    executeSql(connection, sql, progressBar = FALSE, reportOverallTime = FALSE)
  }
}

toStrings <- function(data, sqlDataTypes) {
  intIdx <- (sqlDataTypes == "INT")
  if (nrow(data) == 1) {
    result <- sapply(data, as.character)
    if (any(intIdx)) {
      result[intIdx] <- sapply(data[, intIdx], format, scientific = FALSE)
    }
    result <- paste("'", gsub("'", "''", result), "'", sep = "")
    result[is.na(data)] <- "NULL"
    return(as.data.frame(t(result), stringsAsFactors = FALSE))
  } else {
    result <- sapply(data, as.character)
    if (any(intIdx)) {
      result[ ,intIdx] <- sapply(data[ ,intIdx], format, scientific = FALSE)
    }
    result <- apply(result, FUN = function(x) paste("'", gsub("'", "''", x), "'", sep = ""), MARGIN = 2)
    result[is.na(data)] <- "NULL"
    return(result)
  }
}

castValues <- function(values, sqlDataTypes) {
  paste0("CAST(", values, " AS ", sqlDataTypes, ")")
}

formatRow <- function(data, aliases = c(), castValues, sqlDataTypes) {
  if (castValues) {
    data <- castValues(data, sqlDataTypes)
  }
  return(paste(data, aliases, collapse = ","))
}

ctasHack <- function(connection, sqlTableName, tempTable, sqlFieldNames, sqlDataTypes, data, progressBar, tempEmulationSchema) {
  if (connection@dbms == "hive") {
    batchSize <- 750
  } else {
    batchSize <- 1000
  }
  mergeSize <- 300
  
  if (any(tolower(names(data)) == "subject_id")) {
    distribution <- "--HINT DISTRIBUTE_ON_KEY(SUBJECT_ID)\n"
  } else if (any(tolower(names(data)) == "person_id")) {
    distribution <- "--HINT DISTRIBUTE_ON_KEY(PERSON_ID)\n"
  } else {
    distribution <- ""
  }
  
  # Insert data in batches in temp tables using CTAS:
  if (progressBar) {
    pb <- txtProgressBar(style = 3)
  }
  tempNames <- c()
  for (start in seq(1, nrow(data), by = batchSize)) {
    if (progressBar) {
      setTxtProgressBar(pb, start/nrow(data))
    }
    if (length(tempNames) == mergeSize) {
      mergedName <- paste("#", paste(sample(letters, 20, replace = TRUE), collapse = ""), sep = "")
      mergeTempTables(connection, mergedName, sqlFieldNames, tempNames, distribution, tempEmulationSchema)
      tempNames <- c(mergedName)
    }
    end <- min(start + batchSize - 1, nrow(data))
    batch <- toStrings(data[start:end, , drop = FALSE], sqlDataTypes)
    
    varAliases <- strsplit(sqlFieldNames, ",")[[1]]
    # First line gets type information:
    valueString <- formatRow(batch[1, , drop = FALSE], varAliases, castValues = TRUE, sqlDataTypes = sqlDataTypes)
    if (end > start) {
      # Other lines only get type information if BigQuery:
      valueString <- paste(c(valueString, apply(batch[2:nrow(batch), , drop = FALSE],
                                                MARGIN = 1,
                                                FUN = formatRow,
                                                aliases = varAliases,
                                                castValues = attr(connection, "dbms") %in% c("bigquery", "hive"),
                                                sqlDataTypes = sqlDataTypes)), 
                           collapse = "\nUNION ALL\nSELECT ")
    }
    tempName <- paste("#", paste(sample(letters, 20, replace = TRUE), collapse = ""), sep = "")
    tempNames <- c(tempNames, tempName)
    sql <- paste(distribution,
                 "WITH data (",
                 sqlFieldNames,
                 ") AS (SELECT ",
                 valueString,
                 " ) SELECT ",
                 sqlFieldNames,
                 " INTO ",
                 tempName,
                 " FROM data;",
                 sep = "")
    sql <- SqlRender::translate(sql, targetDialect = connection@dbms, tempEmulationSchema = tempEmulationSchema)
    executeSql(connection, sql, progressBar = FALSE, reportOverallTime = FALSE)
  }
  if (progressBar) {
    setTxtProgressBar(pb, 1)
    close(pb)
  }
  mergeTempTables(connection, sqlTableName, sqlFieldNames, tempNames, distribution, tempEmulationSchema)
}
