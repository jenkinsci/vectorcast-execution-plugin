Changelog
===

### 0.61
Release date: _18 Dec 2019_

- Update to strip whitespace when generating QA XML file ready for VC 2020 release

### 0.60
Release date: _2 Oct 2019_

- Add support for new Manage/QA API and VCAST\_RPTS\_SELF\_CONTAINED

### 0.59
Release date: _13 Sept 2019_

- Update for VECTORCAST\_DIR rollback

### 0.58
Release date: _11 Sept 2019_

- Update for different drive than workspace

### 0.57
Release date: _6 Sept 2019_

- Updates for duplicate results in multijob

### 0.56
Release date: _5 Sept 2019_

- Revert changes from 0.43 and 0.44 to remove need for
  VECTORCAST\_DIR. VECTORCAST\_DIR will need to be defined when
  running Jenkins for now.

### 0.55
Release date: _4 Sept 2019_

- Add support for new VCAST\_RPTS\_SELF\_CONTAINED option added in
  VC19 SP2
- Add support for using Manage API to generate XML reports if
  available
- Correct variable expression on Linux platforms
- Updated scripts to handle compound only in compound case

### 0.54
Release date: _28 Aug 2019_

- Updates for F+FC coverage and updating database pathing

### 0.53
Release date: _20 Aug 2019_

- Updates for changed to aggregate coverage report metrics heading
  change

### 0.52
Release date: _13 Aug 2019_

- Additional debug logging

### 0.51
Release date: _8 Aug 2019_

- Updates for scripts not handling Ada operator "+"

### 0.50
Release date: _11 Jul 2019_

- Update exception handling when using DataAPI

### 0.49
Release date: _1 Jul 2019_

- Updated to support VectorCAST 2019 SP1

### 0.48
Release date: _26 Jun 2019_

- Update to get complete display name for C++ functions
- Update to fix characters that need escaping in XML attributes

### 0.47
Release date: _18 Jun 2019_

- Fix for disabled environments
- Fix for printing non UTF-8 compliant failure code

### 0.46
Release date: _30 May 2019_

- Updates for using external CSS/images

### 0.45
Release date: _10 May 2019_

- Removed xUnit dependency and converted to JUnit.

### 0.44
Release date: _1 May 2019_

- Added better legacy support for VectorCAST installations that do not
  have their executables on the system PATH

### 0.43
Release date: _26 Apr 2019_

- Removed all uses of the environment variable VECTORCAST\_DIR. From
  now on it is assumed that VectorCAST executables are on the system
  PATH. Legacy support is still maintained for older versions of
  VectorCAST.
- Additional cleaning up of old files

### 0.42
Release date: _25 Apr 2019_

- Updates for corner cases, verbose out issue, and cleaning up
  previous build's files
- Problem when function coverage enabled, but not function call
- Function coverage format incorrect in XML causing plugin to throw an
  error
- Added catch for additional licensing errors
- Added catch for all (E) Line: errors in the console log

### 0.41
Release date: _12 Apr 2019_

- Fix for function and basis path coverage when using VectorCAST 2019

### 0.40
Release date: _10 Apr 2019_

- Update to fix auto job updates (where path to Manage project was
  being removed)

### 0.39
Release date: _19 Mar 2019_

- Update to make the management report generate for a cover project

### 0.38
Release date: _23 Jan 2019_

- Fix for spurious newline characters in report title in XML for
  Jenkins with VectorCAST 2019

### 0.37
Release date: _10 Jan 2019_

- Corrected missing " that may affect running multi-job on Linux
- Added support for generating reports using VectorCAST 2019

### 0.36
Release date: _27 Sept 2018_

- Support overlapping version 17 Manage projects
- Updates to support long directory paths in VectorCAST/Manage
  reporting

### 0.35
Release date: _15 May 2018_

- Support newer versions of xUnit plugin

### 0.34
Release date: _10 May 2018_

- Support MultiJob plugin up to 0.29 and later, 0.30 onwards

### 0.33
Release date: _18 Jan 2018_

- Don't create intermediate CSV file for bad test case management
  report

- Raise post-groovy alert for bad test case management report

### 0.32
Release date: _15 Jan 2018_

- Improve support for unit without coverage, avoiding corrupt xml
  files

### 0.31
Release date: _13 Dec 2017_

- Removed spurious " in Linux single job
- Corrected link from xUnit graph to report

### 0.30
Release date: _5 Dec 2017_

- Correct regression with report naming for archived artifacts with
  shorter names
- Added environment variable (VCAST\_VC\_SCRIPTS) to provide optional
    source of vc\_scripts

### 0.29
Release date: _27 Nov 2017_

- Improve support for long Manage project names, environment names and
  compiler names

### 0.28
Release date: _2 Nov 2017_

- Correct regresssion with windows variable names being used in Unix
  script

### 0.27
Release date: _2 Nov 2017_

- Correct regression with missing space in commands for single job

### 0.26 (1 Nov 2017

- Option to set the name of the single job or multi job (name is
  pre-pended to sub-job in the case of multi-jobs)
- Option to configure (at creation/update time) the node to run the
    single job or top-level multi-job on
- Allow license retries for single jobs
- Update summary/detailed text written by the groovy scripts

### 0.25
Release date: _26 Oct 2017_

- Update to retry functionality to support jobs created with earlier
  plugin versions

### 0.24
Release date: _25 Oct 2017_

- Update to store and use job details when auto-updating
- Added (optional) ability to retry a command if it fails due to
  unavailable license

### 0.23
Release date: _17 Oct 2017_

- Added a job that can be used to update an existing multi-job

### 0.22 (26 Sept 2017

- Added support for new version of VectorCAST Manage that uses 2
  levels instead of 4

### 0.21
Release date: _24 Jul 2017_

- Improved groovy script to mark failing builds as failed rather than
  unstable

### 0.20
Release date: _18 Jul 2017_

- Allow conversion script to accept report that has a missing or
  incomplete Function Coverage column

### 0.19
Release date: _23 Jun 2017_

- Added --force option to use of --release-locks
- Added option to use either HTML or TEXT format for the build
  description

### 0.18
Release date: _20 Mar 2017_

- Add execution report link to all test cases
- Added update to pulling in both the full report and incremental
  build report into the job build description
- Added update to pulling in both the full report and incremental
  build report into the job build description

### 0.17
Release date: _17 Mar 2017_

- Always display the VectorCAST menu and leave permission
  checking/reporting to Jenkins

### 0.16
Release date: _15 Mar 2017_

- Corrected processing checking if BUILD\_URL has been set

### 0.15
Release date: _2 Jan 2017_

- Corrected processing to support function and function call coverage

### 0.14
Release date: _16 Dec 2016_

- Corrected typos in Diagnostics job and pattern for files to copy

### 0.13
Release date: _14 Dec 2016_

- Add support for spaces in paths

### 0.12
Release date: _9 Dec 2016_

- Add support to keep or clean the working directory

### 0.11
Release date: _7 Dec 2016_

- Support added for multi-job with SCM and for calculating correctly
  aggregated coverage for the top-level display in the VectorCAST
  coverage plugin

### 0.10
Release date: _23 Nov 2016_

- Initial release (no support for using SCM with multi-job)

