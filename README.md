## Introduction

For this task, I would normally use (eg. Spark, or Kafka File Connector + Kafka + Spark, or Apache Druid, etc.), but this would be an over

Anyway, I still built a small using, just as a reference implementation to compare my solution to XXX

[Lombok](https://projectlombok.org/) is used in this project, so when using an IDE to review the source code please install it first in case you haven't already (in IntelliJ Idea it is just a matter of [adding a plugin](https://projectlombok.org/setup/intellij))

## Build


Java 11 is required, and maven (tested with )

## Run

A quick script is provided for convenience in case this in Linux machines 


2 modes:

`log-parser-exercise [--unique] <HostName> <InitDateTime> <EndDateTime>`
 
`log-parser-exercise --follow [--window=P1H] <HostName>`

initDateTime and endDateTime must be in ISO
window must be, it is an optional parameter, defaults to 1 hour

- --unique: implies storing in memory the hosts already seen, so in case of many different hosts the memory consumption will increase. If this option is given, the order of hosts is not ensured

`java -jar target/log-parser-spring-boot-0.0.1.jar [--follow] [Host] [initDateTime] `

When using the "--follow"

#### Examples:

- Get all connections to 

`log-parser-exercise `

- Get all connections to 

`log-parser-exercise --follow `



## Design

Flux has a very rich / expressive [][]

a Flux is basically a Publisher, quite similar to Java 8 Streams in the sense that it has ...

For more information, it is a tricky concept to get

SpringBoot used for, but not for AutoConfiguration

### Modules

#### file-lines-streamer

#### 

**An optimization could do a binary search, to try to find , but no time to do this on**


### Alternatives considered

#### Kafka or Spark: They are very different fwks, but they both can process huge amount of dataa . can read, I don't know this at practical level and as I assumed t, I didn't explore this option much

#### Quarkus+GraalVM. In the end I didn't have much time to explore this option. 

#### Spring/SpringBoot - started but not needed 

#### Reactive Framework - Using reactive would have lead a, in this case probably the 

### Solution 1: Spring Boot - Very readable and using well known technologies

Finally I chose *not* to use SpringBoot (even if it would have been useful and one can use [CommandLineRunner](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/CommandLineRunner.html),
just because this is a small command line utility and I thought having a short startup time was more important than the benefits of SpringBoot in this particular case.

Netty is used as...  Netty is actually not needed in this simple example 


Note the command line parameters for the SpringBoot application have a double slash `--`, instead of a single slash `-` 

### Solution 2: Quarkus + GraalVM + Project Reactor


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





