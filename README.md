

# Clarity - Backend Code Challenge

## Run

A .jar is provided directly in the .tgz package so the application can be executed right away. For

In this page, the command `logparser` will refer to the main executable, equivalent to:

`java -jar target/connections-log-parser-0.0.1.jar`

(A convenience script has been included in the tgz file called also `logparser` in the package for Linux shells).

The executable has two working modes, corresponding with the two goals in the exercise: `parse` and `follow`:

```
Usage: logparser <mode=parse|follow> <log file path> [options]

Note all options must be preceded with double hyphen '--' and must be separated from their value by an equals sign '=' without any space, eg. --uniqueNames=true
Note also that the mandatory parameters mode and logfile must be written in the command line always as the first two parameters, and the options must follow later.

Examples:
./logparser follow /tmp/input.log --targetHost=Zyrell --sourceHost=Dariya --statsWindow=P1H
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


* Log file path:

A file with lines in this format:

1565647204351 Aadvik Matina
1565647205599 Keimy Dmetri
1565647212986 Tyreonna Rehgan
1565647228897 Heera Eron
1565647246869 Jeremyah Morrigan
1565647247170 Khiem Tailee
```

## Build

Java 11 and Maven 3 is required to build the application (Tested with maven 3.6.0)

[Lombok](https://projectlombok.org/) is used in this project, so when using an IDE to review the source code please install it first in case you haven't already (in IntelliJ Idea it is just a matter of [adding a plugin](https://projectlombok.org/setup/intellij))

Then as usual
```
mvn -U clean package
```




I used this p

For this task, I would normally use (eg. Spark, or Kafka File Connector + Kafka + Spark, or Apache Druid, etc.), but this would be an over

Anyway, I still built a small using, just as a reference implementation to compare my solution to XXX


Tables times


## Design

Even if the @SpringBoot annotation (so no @EnableAutoConfiguration) is not used

There are some utilities from SpringBoot project used: maven plugin (shade plugin can be used for that, but you have it at the same price using springboot)


 * Using just @ComponentScan instead of @SpringBoot as AutoConfiguration is not really worth for this CommandLineRunner.
 * There are not many Spring Beans to autoconfigure and this way we save some initialization time.
 *
 * Anyway the maven parent project of this project is SpringBoot as it is convenient for the embedded maven shade plugin (jar creation)
 * and dependency versions compatibilities. It is also useful for testing.


Flux has a very rich / expressive [][]

a Flux is basically a Publisher, quite similar to Java 8 Streams in the sense that it has ...

This is specially useful for the second part of the exercise, when the application has to react to some event (a new line written in the watched log file). Using the Java 8 Stream API is not so straightforward, as it is the 'client' of the Streams who *pulls* for new content, requiring some kind of blocking. 

For more information, it is a tricky concept to get

SpringBoot used for, but not for AutoConfiguration


library

At the beginning SpringBoot application, but just for a command line not.. so





### Module: connections-log-parser

### Module: files-reactive

This is a library that makes all the work to read lines of a file

I also thought






### Alternatives considered

#### Kafka

Requires a kafka cluster

#### Spark

They are very different fwks, but they both can process huge amount of dataa . can read, I don't know this at practical level and as I assumed t, I didn't explore this option much


#### Quarkus+GraalVM. In the end I didn't have much time to explore this option. 



### Results


200Mb 18M lines-->
9.73user 0.43system 0:06.56elapsed 154%CPU (0avgtext+0avgdata 536820maxresident)k
4.67user 0.24system 0:01.87elapsed 262%CPU (0avgtext+0avgdata 176104maxresident)k

28Gb (~ 1000M lines)

4.62user 0.23system 0:02.30elapsed 210%CPU (0avgtext+0avgdata 182716maxresident)k



2.8Gb (~ 100M lines)
28Gb (~ 1000M lines)

Note the elapsed time is measured from outside the application, so it includes all Java Virtual Machine initialization time, and Spring initialization time.

splits==0
37.15user 1.37system 0:34.87elapsed 110%CPU (0avgtext+0avgdata 526028maxresident)k

splits==1 (that means using the split and parallel login, but only one part and one thread anyway)
42.42user 1.66system 0:41.40elapsed 106%CPU (0avgtext+0avgdata 435800maxresident)k

splits==2
45.94user 1.55system 0:22.74elapsed 208%CPU (0avgtext+0avgdata 588328maxresident)k

splits==3
66.98user 1.85system 0:29.94elapsed 229%CPU (0avgtext+0avgdata 889068maxresident)k

splits=0
************** Elapsed: 361394
336.30user 18.15system 6:03.18elapsed 97%CPU (0avgtext+0avgdata 590188maxresident)k

What is worse, with a file of 28Gb I surprisingly an OutOfMemory error.



splits=2

Probably the wrong approach, but it was worth to try.

Flux disappointed, Stream slightly better

My hypothesis SSD

In a file (around with 100 million lines 

splitting the file in 2

However in smaller files, splitting the filw was actually slower

/home/rmartinez/tmp/ClarityTemp/input-file-100010000.txt



### Modules

#### file-lines-streamer

It is a library, so it tries to minimize dependencies used, and of course it does not use any big framework such as Spring. Actually the only dependencies are reactor-core and sl4j


**An optimization could do a binary search, to try to find , but no time to do this on**





## Modes

### Binary Search

This 


### Solution comparison

Startup time
10k lines file
100k lines file
1M lines file


## Testing


## Requirements

The project has been built using the following versions:

- Maven 3.
- JDK 12 





## Try CommonMark

You can try CommonMark here.  This dingus is powered by [commonmark.js](https://github.com/jgm/commonmark.js), the JavaScript reference implementation.

1. item one
2. item two
   - sublist
   - sublist

this is a code block
	block
	block





