

# Clarity - Backend Code Challenge

## Run

A .jar is provided directly in the .tgz package so the application can be executed right away.

A convenience script has been also included in the tgz file called also `logparser` for Linux shells, that is the equivalent to:

`java -jar connections-log-parser-0.0.1.jar`


The executable has two working modes, corresponding with the two goals in the exercise: `parse` and `follow`:

```
Usage: logparser <mode=parse|follow> <log file path> [options]

Note all options must be preceded with double hyphen '--' and must be separated from their value by an equals sign '=' without any space, eg. --uniqueNames=true
Note also that the mandatory parameters mode and logfile must be written in the command line always as the first two parameters, and the options must follow later.

Examples:
./logparser follow /tmp/input.log --targetHost=Zyrell --sourceHost=Dariya --statsWindow=PT1H
./logparser parse /tmp/input.log --initTimestamp=10000995 --endTimestamp=10000000000 --targetHost=Zyrell
./logparser parse /tmp/input.log --initDateTime=2019-01-01T00:00:00Z --endDateTime=2019-09-01T00:00:00Z --targetHost=Zyrell

* Mode: parse
    Shows all sourceHosts connected to a given --targetHost between an --initDateTime and an --endDateTime

    --uniqueHosts=<true|false>: Defaults to false. When true, only a list of unique hosts connected to the specified targetHost is shown. This is slower and requires more memory, specially if there are a huge number of different hosts. When false a list of sourceHosts and timestamps are shown.
    --presearchTimestamp=<true|false>. Experimental. It showed very good results during the tests so it defaults to true.
    --splits=n: Experimental. When n>0 the log file is split in n slices and, by experience n>3 doesn't provide much benefit, but n==2 could reduce parsing time in big files. Defaults to 0. n==1 means using parallel logic but not actually

    --targetHost=<hostName> [Mandatory] The target host
    --initDateTime | --initTimestamp: [Mandatory]  unix timestamp or ISO 8601 Zoned Date time
    --endDateTime  | --endTimestamp:  [Mandatory]


* Mode: follow
    Opens a file and keeps watching for *new* lines added, showing stats every --statsWindow seconds.
    If the file doesn't exist a new empty file will be created.

    --statsWindow=<ISO Period>: Optional, defines the window to collect stats. Defaults to 10 seconds.
    --sourceHost=<host name>: Optional. If present the stats will show all target hosts connected from this sourceHost in the specified window
    --targetHost=<host name>: Optional. If present the stats will show all source hosts connected to this targetHost in the specified window

```

## Build

Java 11 and Maven 3 is required to build the application (Tested with maven 3.6.0)

[Lombok](https://projectlombok.org/) is used in this project, so when using an IDE to review the source code please install it first in case you haven't already (in IntelliJ Idea it is just a matter of [adding a plugin](https://projectlombok.org/setup/intellij))

then, as usual:
```
mvn -U clean package
```

## Design

### Alternatives Considered

* **Spark**: Even if I am not an expert in Spark, it seems the natural choice for this task. It allows processing of huge files efficiently. However I supposed the exercise objective was to evaluate my programming style, and a Spark job would have not showing it.  
* **Kafka**: Similar to Spark, it has connectors to watch changes on a file then processing them (now using even KSQL some of the objectives of the exercise could have been fulfilled without even programming). I discarded it for the same reason as Spark. 


### Structure

The code compiles to an assembled (über) jar that can be executed from the command line. The executable is a Spring Boot Application, although the code is organized in two different modules:

* **files-reactive**: A library that can "convert" Files to a [Project Reactor](https://projectreactor.io/)'s [Flux](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html) of lines, for full files or just fragments of files.  
* **connections-log-parser**: The SpringBoot application that uses the files-reactive library. 

The main pom.xml file builds the two modules.

Note: I would have liked to provide more unit tests and javadoc in the modules, and in a normal enterprise application I would have done so. But I didn't really have more time for the project
so I hope the tests and javadoc already included are enough as a sample.

### Results

(Tests performed in a mid-range laptop)

#### presearchTimestamp

Using `--presearchTimestamp` can *dramatically* reduce execution times, assuming the timestamp range requested is small compared with the size of the file.

As the time spent on the presearch itself is negligible, the feature is activated by default although it can be deactivated with the flag `--presearchTimestamp==false` (for small files, or for big files when all the file must be read anyway)

|presearchTimestamp?| # Lines | size | Time | 
|---|---|---|---|
|false|2 Million|200Mb|0:06.56s|
|true|2 Million|200Mb|**0:01.87s**|
|false|1000 Million|28Gb|6:21.85s|
|true|1000 Million|28Gb|**0:02.30s**|

Of course in this examples the timestamp range was very small, so they execution times could gain the most benefit from activating the feature

#### split

On the contrary, using `--split` to process the file using several threads in parallel showed disappointed results. The theory was that on *SSD disks* with almost random access there was no penalty open multiple pointers to the same file in different locations,
but the tests showed the contrary: 

|split?| # Lines | size | Time | Resources | 
|---|---|---|---|---|
|No Split  |100 Million|2.8Gb|0:35.39s|37.45user 1.51system 110%CPU (514324maxresident)k|
|--split==2|100 Million|2.8Gb|0:20.7s|41.54user 1.16system 206%CPU (632016maxresident)k|
|--split==3|100 Million|2.8Gb|0:28.04s|70.39user 1.42system 256%CPU (635712maxresident)k|
|No Split  |1000 Million|28Gb|6:21.85m|359.10user 16.86system 98%CPU (523132maxresident)k|
|--split==2|1000 Million|28Gb|4:02.39m|448.01user 16.80system 188%CPU (574780maxresident)k|
|--split==3|1000 Million|28Gb|4:06.19m|564.46user 19.40system 240%CPU (645716maxresident)k|

However, as far as I know, Spark (as I said I am not an expert) can process files in parallel in an scalable way so most probably I am missing something and would need to continue investigating.

In smaller files, the parallel version was even worse than the sequential, but that was expected.

With big/huge files, using --split=2 could improve times up to a 40%, but using a number of splits > 3 only increased resources without any benefit. 
