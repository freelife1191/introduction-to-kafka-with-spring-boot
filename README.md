# Introduction to Kafka with Spring Boot

This repository contains the code to support the [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?referralCode=15118530CA63AD1AF16D) online course.

The application code is for a message driven service which utilises Kafka and Spring Boot 3.

The code in this repository is used to help students learn how to produce and consume messages in Java using Spring Kafka and Spring Boot 3.
The course provides step by step instructions and detailed explanations for students to build up the code themselves.
This repository provides the code to support the course broken down section by section using branches, allowing students to
compare and contrast the code they create with the lesson code in the branches.

As you work through the course, please feel free to fork this repository to your own GitHub repo. Most lectures contain links
to source code changes. If you encounter a problem you can compare your code to the lecture code. See the [How to Compare Branches](https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki#how-to-compare-branches) section in the Wiki.

## Introduction to Kafka with Spring Boot Course Wiki
Plenty of useful information about your Introduction to Kafka with Spring Boot course can be found in the [Wiki](https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki).

## Getting Your Development Environment Setup
### Recommended Versions
| Recommended                | Reference                                                             | Notes                                                                                                                                                                                                                                                          |
|----------------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Apache Kafka 3.3 or higher | [Download](https://kafka.apache.org/downloads)                        |                                                                                                                                                                                    |
| Oracle Java 17 JDK         | [Download](https://www.oracle.com/java/technologies/downloads/#java17) | Java 17 or higher. We recommend using the most recent LTS (Long-Term Support) release                                                                                                                                                                          |
| IntelliJ 2022 or higher    | [Download](https://www.jetbrains.com/idea/download/)                  | Ultimate Edition recommended. Students can get a free 120 trial license, courtesy of the Spring Framework Guru, [here](https://github.com/springframeworkguru/spring5webapp/wiki/Which-IDE-to-Use%3F#how-do-i-get-the-free-120-day-trial-to-intellij-ultimate) |
| Maven 3.6 or higher        | [Download](https://maven.apache.org/download.cgi)                     | [Installation Instructions](https://maven.apache.org/install.html)                                                                                                                                                                                             |                                                                                                                 | **Note:** Use Version 5 or higher if using Java 11                                                                                                                                                                     |
| Git 2.39 or higher         | [Download](https://git-scm.com/downloads)                             |                                                                                                                                                                                                                                                                | 
| Git GUI Clients            | [Download](https://git-scm.com/downloads/guis)                        | Not required. But can be helpful if new to Git. SourceTree is a good option for Mac and Windows users.                                                                                                                                                         |

## Connect with the team at Lydtech Consulting
* Visit us at [lydtechconsulting.com](https://www.lydtechconsulting.com/)
* Visit our [LinkedIn](https://www.linkedin.com/company/lydtech-consulting) page

# 5. Coding Kafka with Spirng Boot

## 1. Section Introduction
---

- Service Overview
    - Repeatable pattern
    - Common EDA implementation
- Tech List
    - Spring Boot 3
    - Spring Kafka
    - Java 17
    - Maven 3.6+
    - Lombok
    - Kafka 3.3+
        - Kraft O, Zookeeper X


## 2. Creating The Project - Spring Initializr
---

https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot


## 3. The Consumer
---

- Add a consumer to the Dispatch Service
- Spring annotation
    - `@KafkaListener`
    - `@Component`
    - `@Service`
- Application properties - Deserialization
- Unit tests
    - JUnit, Mockito
- Command line invocation

Console Producer -`order.created`-> Dispatch

구현한 Consumer 서버를 구동시키고 카프카 테스트 메세지 전송
```bash
$ bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092

> test message
```

Consumer 메세지 응답
```bash
2023-11-23T22:01:58.204+09:00  INFO 62170 --- [merClient-0-C-1] d.l.d.handler.OrderCreatedHandler        : Received message: payload: test-message
2023-11-23T22:02:07.685+09:00  INFO 62170 --- [merClient-0-C-1] d.l.d.handler.OrderCreatedHandler        : Received message: payload: test-message2
```

### Recap
summing up

- Added a consumer with `@KafkaListener`
- Specified the deserialization configuration
- verified with unit tests
- Ran the end to end flow


## 4. JSON Deserializer
---

- Change the Order Created event type to JSON
- Create a POJO to represent the event
- Update deserialization configuration in application.properties
- Send a JSON event from the command line


Console producer -`order.created`(JSON)-> Dispatch

카프카 JSON 메세지 전송
```bash
$ bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092

>{"orderId": "7c4d32e9-4999-434b-953a-9467f09b023f","item":"item-1"}
```

Consumer JSON 메세지 응답
```bash
2023-11-23T23:06:37.135+09:00  INFO 62978 --- [merClient-0-C-1] d.l.d.handler.OrderCreatedHandler        : Received message: payload: OrderCreated(orderId=7c4d32e9-4999-434b-953a-9467f09b023f, item=item-1)
```

### Recap
summing up

- Added a class of type 'OrderCreated' to represent the event
- Updated the deserialization configuration
- Ran the end to end flow


## 5. Deserializer Error Handling
---

- Demonstrate deserialization errors
- Handling deserialization errors
- Use Spring Kafka's ErrorHandlingDeserializer

Console producer -`order.created`(Invalid JSON)-> Dispatch

잘못된 형식의 메세지를 전송하여 Consumer 역직렬화시 오류를 발생시킴
```bash
bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092

>{"orderId":"123","item":"invalid-1"}
>{"orderId": "7c4d32e9-4999-434b-953a-9467f09b023f","item":"item-2"}
>{"orderId":"123","item":"invalid-2"}
```

조치 후 Consumer에서 잘못된 형식의 메세지는 읽기에 실패하지만 다음 메세지를 문제없이 계속 받을 수 있음
### Recap
Summing up

- Demonstrated a deserialization error
- Updated the deserialization configuration
- Observed invalid event handling


## 6. Spring Bean Configuration
---

- Define configuration programmatically
  - KafkaListenerContainerFactory
  - ConsumerFactory
- Spring annotations
  - `@Configuration`
  - `@Bean`
  - `@ComponentScan`
  - `@Value`


카프카 메세지 전송 테스트
```bash
bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092

>{"orderId": "7c4d32e9-4999-434b-953a-9467f09b023f","item":"item-3"}
```

Consumer 서버 로그
```bash
2023-11-24T17:18:17.661+09:00  INFO 69907 --- [merClient-0-C-1] d.l.d.handler.OrderCreatedHandler        : Received message: payload: OrderCreated(orderId=7c4d32e9-4999-434b-953a-9467f09b023f, item=item-3)
```

### Recap
Summing up

- Defined a Spring configuration class
- Configured our Kafka listener
- Verified via the command line


## 7. Create The Topics
---

자동 Topic 생성과 수동 Topic 생성 비교

- Automatic topic creation
  - Useful for local development & testing

![](attachements/section5/20231124174057.png)


### 자동 Topic 설정 기본값
- Broker config: `auto.create.topics`
  - Default: `true`
- Consumer config: `allow.auto.create.topics`
  - Default: `true`

### 권장 설정
- Manual topic creation (명령줄 도구를 통해 생성된 topic만 허용)
  - Best practice for Production (and remote environments)
  - 인증된 항목만 ACL을 사용하여 보호됨


## 8. Produce
---

- Send an outbound JSON event **OrderDispatched**
- Spring beans
  - `KafkaTemplate`
  - `ProducerFactory`

Console producer -`order.created`-> Dispatch -`order.dispatched`->

- Synchronous vs asynchronous send
- Unhappy path unit tests


### Rcap
Summing up

- DispatchService updated to send an OrderDispatched JSON event
- Used KafkaTemplate to send the event
- Added exception handling in the handler


## 9. Consume Using CLI
---

Consume the Outbound Message on the Command Line

- Run the end to end flow
- Send an inbound event with the console-producer
- Consume the outbound event with the console-consumer

Console producer -`order.created`-> Dispatch -`order.dispatched`-> Console consumer

카프카 서버 시작
```bash
$ bin/kafka-server-start.sh config/kraft/server.properties
```

카프카 Consumer 콘솔 실행 및 응답 확인
```bash
$ bin/kafka-console-consumer.sh --topic order.dispatched --bootstrap-server localhost:9092

{"orderId":"7c4d32e9-4999-434b-953a-9467f09b023f"}
```

스프링 부트 서버 기동 메세지 응답 확인
```bash
Received message: payload: OrderCreated(orderId=7c4d32e9-4999-434b-953a-9467f09b023f, item=item-4)
```

카프카 Producer 콘솔 실행 및 메세지 전송
```bash
$ bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092

> {"orderId": "7c4d32e9-4999-434b-953a-9467f09b023f","item":"item-4"}
```

### Recap
Summing up

- Used the command line tools to produce and consume events
- Proved the full end to end flow works successfully


# 6. Assignment - Tracking Service

## Dispatch Tracking Service
---
- Dispatch Service
  - https://github.com/freelife1191/introduction-to-kafka-with-spring-boot/tree/07-assignment-consume-and-produce
- Traking Service
  - https://github.com/freelife1191/introduction-to-kafka-with-spring-boot-tracking-service/tree/01-assignment-consume-and-produce

이 과제에서는 'Tracking'이라는 새 서비스를 만들려고 합니다.

_추적 서비스는 dispatch.tracking_ 주제 의 이벤트를 사용하여 상태를 확인 하고 디스패치의 현재 상태를 반영하는 이벤트를 생성합니다.

![](attachements/section6/20231124230535.png)

### Dispatch Service
Dispatch Service to emit a DispatchPreparing event on a new topic named _dispatch.tracking_

The payload for the DispatchPreparing event should look like:

`orderId: UUID`


### Tracking Service
Create a new service named TrackingService.  Follow the same steps used to create the Dispatch Service.

The Tracking Service should consume events from the _dispatch.tracking_ topic and emit a TrackingStatusUpdated event on a new topic named _tracking.status_.

The payload for the TrackingStatusUpdated event should look like:

1. orderId: UUID
2. status: Status

The enum should only contain a single value 'PREPARING' at this time


### Testing
Changes to both services should have unit test coverage.

### 질문 정답
1. Did you first generate a skeletal project for the Tracking service which you then built out, and if so, how did you generate it?
  - Spring Initializr could be used to generate the skeletal project as a starting point for the development of the Tracking service

2. What Spring Kafka annotation did you use on the listen method on the Tracking service handler class for the DispatchPreparing event?
  - Spring Kafka's KafkaListener annotation should be added to mark the listen method as the target for the DispatchPreparing event

3. What class did you configure for the ConsumerFactory ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG and why?
  - By configuring the deserializer as the ErrorHandlingDeserializer, it allows Spring Kafka to cleanly handle errors arising from deserializing invalid JSON messages

4. What Spring Kafka class did you use to send out the TrackingStatusUpdated event?
  - Spring Kafka's KafkaTemplate provides send methods to produce messages to Kafka


# 7. Spring Boot Integration Test

## Section Introduction

- Integration Test Overview
- Implement an integration test

### Unit Test
- Unit Tests are good
  - Small
  - Quick / easy to code
  - Fast feedback
- Prove units of code

### Proving code unit integrations
- Integration Testing
  - Test flows
  - Application context
  - In-memory instances (DB, broker)
  - Fast feedback
  - Verify 3rd Party Integrations (e.g Kafka)

## Integration Test
Using Spring Boot Test for Integration testing

![](attachements/section7/20231218220721.png)

### Spring Boot Test

- Build a Spring Boot Test
  - `@SpringBootTest`
  - `@ActiveProfiles`
  - `@DirtiesContext`
  - `@Autowired`
  - `@EmbeddedKafka`

### Test Helpers & Profile

- JUnit & Awaitility
- `application-test.properties`
- `spring.embedded.kafka.brokers`

## Recap
Summing up

- Built an integration test with Spring Boot Test
- Used the embedded Kafka Broker
- Used Kafka Template to send messages
- Verified with two test consumers



## Section Introduction
---
## Consumer Groups
Parallel Processing

### In this section...
- Throughput
- Fault Tolerance
- Scaling
- Heartbeating
- Rebalancing

## Why Consumer Groups?
- Isolation or Parallelism
- Distinct Processing Logic
  - ![](attachements/section8/20231230203926.png)
  - Separate event streams
  - ![](attachements/section8/20231230203957.png)
- Throughput
  - Single Consumer
  - ![](attachements/section8/20231230204040.png)
  - Multiplie Consumers
  - Parallel Processing
  - ![](attachements/section8/20231230204135.png)
- Throughput & Scaling
  - ![](attachements/section8/20231230204604.png)
  - Max active consumers = num of partitions
  - ![](attachements/section8/20231230204734.png)
- Fault Tolerance
  - Heartbeat
  - Poll Interval
  - ![](attachements/section8/20231230204819.png)
  - ![](attachements/section8/20231230204842.png)
- Fault Tolerance & Rebalancing
  - New Consumer
  - Dead Consumer
  - Lost Consumer
  - ![](attachements/section8/20231230204914.png)
  - ![](attachements/section8/20231230204931.png)
- Rebalancing
  - Consumer Scaling
    - Starting/Stopping Consumers
  - Dead Consumer
  - Lost Consumer
  - ![](attachements/section8/20231230204944.png)
### Summing up
- Distinct & Parallel Processing
- Fault Tolerance
- Scaling
- Rebalancing
  - Heartbeat
  - Polling Interval
  - Rebalancing Strategy
  - Pause in Processing


# 8. Multiple Instance & Consumer Groups

## Introduction to Consumer Group Exercises
---
Demonstrating the impact of Consumer Groups

- Shared Consumer Group
- Consumer Failover
- Duplicate Consumption

## Shared Consumer Group
---
Multiple consumers in the consumer group

### in this module...
- Run 2 instances of the application
- Unique application Id for each
  ![](attachements/section8/20231230214134.png)

- Shared group Id
- Single partition

```bash
bin/kafka-console-producer.sh --topic order.created --bootstrap-server localhost:9092
>{"orderId": "7c4d32e9-4999-434b-953a-9467f09b023f","item":"item-5"}
```

## Recap
Summing up

- Demonstrated shared consumer group behaviour
- Only one instance in a consumer group received the event


## Demo Consumer Failover
---
Fault tolerance with consumer failover

### in this module...
- Kill the assigned consumer
- Observe consumer group rebalance

![](attachements/section8/20231230220332.png)

## Recap
Summing up

- Demonstrated Consumer failover behaviour
- The second consumer instance is assigned the partition

## Duplicate Consumption
---
Multiple consumers in different consumer groups

### In this module...
- Run 2 instances of the application
- Different group Ids
- Both consumers assigned partition

![](attachements/section8/20231230221239.png)

## Recap
Summing up

- Demonstrated separate consumer group behaviour
- 별도의 소비자 그룹 동작 시연
- The instance in each consumer group received the event
-   각 소비자 그룹의 인스턴스가 이벤트를 수신했다

## Section Recap
---
## Consumer Group
Summing up

- Consumer groups are required for applications that need all of the messages from a topic
- 주제의 모든 메시지가 필요한 애플리케이션에는 소비자 그룹이 필요하다