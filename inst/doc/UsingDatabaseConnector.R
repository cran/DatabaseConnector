## ---- echo = FALSE, message = FALSE--------------------------------------
library(DatabaseConnector)
knitr::opts_chunk$set(
  cache=FALSE,
  comment = "#>",
  error = FALSE,
  tidy = FALSE)

## ----eval=FALSE----------------------------------------------------------
#  ?jdbcDrivers

## ----eval=FALSE----------------------------------------------------------
#  conn <- connect(dbms = "postgresql",
#                  server = "localhost/postgres",
#                  user = "joe",
#                  password = "secret",
#                  schema = "cdm")

## ----echo=FALSE----------------------------------------------------------
writeLines("Connecting using PostgreSQL driver")

## ----eval=FALSE----------------------------------------------------------
#  disconnect(conn)

## ----eval=FALSE----------------------------------------------------------
#  conn <- connect(dbms = "postgresql",
#                  connectionString = "jdbc:postgresql://localhost:5432/postgres",
#                  user = "joe",
#                  password = "secret",
#                  schema = "cdm")

## ----echo=FALSE----------------------------------------------------------
writeLines("Connecting using PostgreSQL driver")

## ----eval=FALSE----------------------------------------------------------
#  details <- createConnectionDetails(dbms = "postgresql",
#                                     server = "localhost/postgres",
#                                     user = "joe",
#                                     password = "secret",
#                                     schema = "cdm")
#  conn <- connect(details)

## ----echo=FALSE----------------------------------------------------------
writeLines("Connecting using PostgreSQL driver")

## ----eval=FALSE----------------------------------------------------------
#  details <- createConnectionDetails(dbms = "netezza",
#                                     server = "myserver.com/mainDb",
#                                     user = "joe",
#                                     password = "secret",
#                                     schema = "cdm",
#                                     pathToDriver = "c:/temp")
#  conn <- connect(details)

## ----echo=FALSE----------------------------------------------------------
writeLines("Connecting using Netezza driver")

## ----eval=FALSE----------------------------------------------------------
#  querySql(conn, "SELECT TOP 3 * FROM person")

## ----echo=FALSE----------------------------------------------------------
data.frame(PERSON_ID = c(1,2,3), GENDER_CONCEPT_ID = c(8507, 8507, 8507), YEAR_OF_BIRTH = c(1975, 1976, 1977))

## ----eval=FALSE----------------------------------------------------------
#  executeSql(conn, "TRUNCATE TABLE foo; DROP TABLE foo; CREATE TABLE foo (bar INT);")

## ----eval=FALSE----------------------------------------------------------
#  x <- querySql.ffdf(conn, "SELECT * FROM person")

## ----eval=FALSE----------------------------------------------------------
#  data(mtcars)
#  insertTable(conn, "mtcars", mtcars, createTable = TRUE)

## ----eval=FALSE----------------------------------------------------------
#  conn <- dbConnect(DatabaseConnectorDriver(),
#                    dbms = "postgresql",
#                    server = "localhost/postgres",
#                    user = "joe",
#                    password = "secret",
#                    schema = "cdm")

## ----echo=FALSE----------------------------------------------------------
writeLines("Connecting using PostgreSQL driver")

## ----eval=FALSE----------------------------------------------------------
#  dbIsValid(conn)

## ----echo=FALSE----------------------------------------------------------
TRUE

## ----eval=FALSE----------------------------------------------------------
#  res <- dbSendQuery(conn, "SELECT TOP 3 * FROM person")
#  dbFetch(res)

## ----echo=FALSE----------------------------------------------------------
data.frame(PERSON_ID = c(1,2,3), GENDER_CONCEPT_ID = c(8507, 8507, 8507), YEAR_OF_BIRTH = c(1975, 1976, 1977))

## ----eval=FALSE----------------------------------------------------------
#  dbHasCompleted(res)

## ----echo=FALSE----------------------------------------------------------
TRUE

## ----eval=FALSE----------------------------------------------------------
#  dbClearResult(res)
#  dbDisconnect(res)

