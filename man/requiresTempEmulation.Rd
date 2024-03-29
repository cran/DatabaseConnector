% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/Sql.R
\name{requiresTempEmulation}
\alias{requiresTempEmulation}
\title{Does the DBMS require temp table emulation?}
\usage{
requiresTempEmulation(dbms)
}
\arguments{
\item{dbms}{The type of DBMS running on the server. See \code{\link[=connect]{connect()}} or \code{\link[=createConnectionDetails]{createConnectionDetails()}} for
valid values.}
}
\value{
TRUE if the DBMS requires temp table emulation, FALSE otherwise.
}
\description{
Does the DBMS require temp table emulation?
}
\examples{
requiresTempEmulation("postgresql")
requiresTempEmulation("oracle")

}
