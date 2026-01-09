# 📖 Kafka Study
## CLI 실습
### 🔍 환경
- UTM에 Ubuntu 22.04 마운트 
- confluent-kafka 7.8.1 tar download
- tar -xvf [해당 버전 tar]
- .bashrc에 ```export CONFLUENT_HOME=/home/master/confluent``` 홈 경로 설정
- PATH 설정 ```export PATH=.:$PATH:$CONFLUENT_HOME/bin``` 후 ```source ~./bashrc```
### 🔍 서버 설정
- home 경로에 zookeeper 실행 스크립트 및 kafka 실행 스크립트 작성
- zookeeper_start.sh : ```$CONFLUENT_HOME/bin/zookeeper-server-start $CONFLUENT_HOME/etc/kafka/zookeeper.properties```  kafka_start.sh : ```$CONFLUENT_HOME/bin/kafka-server-start $CONFLUENT_HOME/etc/kafka/server.properties```
- 기존의 로그 경로를 /tmp 아래에서 아래 사진의 경로로 변경한다. ```cd $CONFLUENT_HOME/etc/kafka``` 경로의 server.properties 들어가서 바꾼다.
- ```cd $CONFLUENT_HOME/etc/kafka``` 에 zookeeper.properties에 들어가서도 해당 로그 경로를 home 디렉토리로 변경한다.
  - 이유는 vm 재부팅시 /tmp 하위는 데이터가 날아갈 수 있어서 이다. 
- <img width="776" height="319" alt="스크린샷 2025-08-31 오후 4 02 11" src="https://github.com/user-attachments/assets/24049e68-b51b-48c5-b042-c6453b2c82e7" />

#### confluent vs apache-kafka
- 자세한 더 알아봐야겠지만 일단 confluent kafka는 etc 밑에 해당 라이브러리의 파일이 있는 반면 apache-kafka는 config 디렉토리 밑에 파일이 있다.
- 또한 confluent-kafka는 커뮤니티 버전은 제외하고 다른 버전에서 ksqldb를 지원한다.

### ⛄️ Topic 생성 및 정보 확인

|주요 인자|설명|
|-------|-------|
|--bootstrap-server|Topic을 생성할 Kafka Broker 서버 주소:Port <br>--bootstrap-server localhost:9092|
|--create|--topic: 기술된 topic 명으로 topic 신규 생성 <br> --partitions: Topic의 파티션 수 <br> --replication-factor: replication 개수|
|--list|브로커에 있는 Topic들의 리스트|
|--describe|--topic: 기술된 topic명으로 상세 정보 표시|

### 📚 명령어 요약
- Topic 생성 : ```kafka-topics --bootstrap-server [host]:[port] --create --topic [topic name]```
- Topic 생성(파티션 수 설정) : ```kafka-topics --bootstrap-server [host]:[port] --create --topic [topic name] --partitions [count]```
- Topic 생성(replica-factor 포함) : ``` server [host]:[port] --create --topic [topic name] --partitions [count] --replication-factor [count]```
- Topic 삭제 : ```kafka-topics --bootstrap-server [host]:[port] --delete --topic [topic name]```
- Topic 상세 : ```kafka-topics --boostrap-server [host]:[port] --describe --topic [topic name]```
- 브로커의 topic list 조회 : ```kafka-topics --boostrap-server [host]:[port] --list```

### 🐶 Kafka Cli 에서 메세지 쓰고 읽기
#### Produce
- ```kafka-console-producer --bootstrap-server [host]:[port] --topic [topic name]``` 을 치면 > 표시가 나오고 메세지를 produce 할 수 있다.
- aaa, bbb, ccc 등 메세지를 produce 해보자
- 참고로 Producer가 메세지를 broker에 넣는 과정에서는 send() -> Serializer(byte code로 변환) -> Partitioner(어떤 파티션으로 갈지 매핑) 하는 과정을 거친다.
#### Consume
- ```kafka-console-consumer --bootstrap-server [host]:[port] --topic [topic name]``` 을 치면 consume 할 수 있다.
- 🚨 그런데 메세지를 consume하지 않는다. 왜 그러지?
- 🫸 Kafka의 consumer는 auto.offset.reset 기능을 가지고 있다. 이것이 무엇이냐? >>> 바로 Consumer가 Topic에 처음 접근하여 메세지를 가져올때 가장 처음(오래된) 메세지부터 가져올 것인지 아니면 가장 최근의 메세지를 가져올 것인지 판단하는 기능이다.
- Default는 lastest로 되어있어 먼저 produce한 메세지들은 가지고 오지 않는 것이다
- 이럴때는 ```kafka-console-consumer --bootstrap-server [host]:[port] --topic [topic name] --from-beginning``` 이렇게 치면 전의 메세지 즉 가장 오래된 메세지부터 가져올 수 있게된다.

### 🗝️ Key 값을 가지지 않는 메세지 전송
- 메세지는 producer를 통해 전송 시 Partitioner를 통해서 어떤 파티션으로 갈지 라우팅 됨.
- Key 값을 가지지 않는 경우: Round robin, sticky partition등의 파티션 전략등이 선택되어 파티션 별로 메세지 전송됨.
  - Round robin : 메세지 배치를 순차적으로 다른 파티션으로 전송함 - 구버전
  - sticky partition 이란? 특정 파티션으로 전송되는 하나의 배치에 메세지를 빠르게 먼저 채워서 보내는 방식
- Topic이 복수 개의 파티션을 가질때 메세지 순서가 보장되지 않음.
#### 분배 전략
- RR(round robin) : kafka 2.4버전 이전 기본 파티션 분배 전략 - 메세지 배치를 순차적으로 **다른** 파티션으로 전송함 ex) batch size가 다 차면 전송 설정(batch.size)
  - 메세지가 배치 데이터를 빨리 채우지 못하면서 전송이 늦어짐, 배치를 다 채우지 못하고 전송하면서 전송 성능이 떨어짐 
- Sticky Partitioning - kafka 2.4 버전부터 기본 파티션 분배 전략
  - 특정 파티션으로 전송되는 하나의 배치에 메세지를 빠르게 먼저 채워서 보내는 방식
  - 일정시간 동안 특정 파티션에 메세지를 sticky하게 쭉 모은다.
  - 배치가 꽉 차거나 linger.ms 가 지나면 그때 새로운 파티션을 선택해서 다시 sticky하게 쭉 모은다.
  - 배치 사이즈를 크게 키울수 있어서 성능 향상 
### 🔐 Key 값을 가지는 매세지 전송
- 특정 Key 값을 가지는 메세지는 특정 파티션으로 고정되어 전송된다.
#### ‼️ 알아둘 점!
- 카프카는 하나의 파티션 내에서만 메세지 순서를 보장한다.
#### Kafka cli로 키 값 있는 메세지 produce && consume
- Produce : ```kafka-console-producer --bootstrap-server localhost:9092 --topic test-topic \ --property key.separator=: --property parse.key=true```
- Consume : ```kafka-console-consumer --bootstrap-server localhost:9092 --topic test-topic \ --property print.key=true --property print.value=true --from-beginning```
## 여러개의 파티션을 가지는 메세지 전송
### 🍇 Topic 생성
```kafka-topics --bootstrap-server [host]:[port] --create --topic [topic name] --partitions [파티션 수]```
### 👉 파티션별로 consume 하는 것을 보여주는 명령어
```kafka-topics --bootstrap-server [host]:[port] --topic [topic name] \ --from-beginning --property print.partition=true```
<img width="369" height="746" alt="스크린샷 2025-09-04 오후 11 17 34" src="https://github.com/user-attachments/assets/50eb31f1-c619-42c7-8a8f-9f5a7bf95bce" />

### 🍎 키값을 가지는 메세지의 경우
```kafka-console-consumer --bootstrap-server [host]:[port] --topic [topic name] \ --property print.key=true --property print.value=true \ --property print.partition=true```
- 해당 명령어 사용시 메세지가 어디쪽 파티션에서 소비되었는지 나온다.

## 🏭 Producer
### acks설정에 따른 send 방식
- 해당 Topic의 파티션의 리더 브로커에게만 메세지를 보냄
### acks 0
- 리더 브로커가 메세지 A를 정상적으로 받았는지에 대한 ack 메세지를 받지 않고 다음 메세지인 메세지 B를 바로 전송
- 메세지가 제대로 전송되었는지 브로커로 부터 확인을 받지 않기 떄문에 메세지가 브로커에 기록되지 않더라도 재 전송하지 않음
- 메세지 손실의 우려가 가장 크지만 가장 빠르게 전송할 수 있음
### acks 1
- 리더 브로커가 메세지 A를 정상적으로 받았다는 ack 메세지를 받은 후에 다음 메세지인 메세지 B를 전송
  - 만약 오류 메세지를 브로커로 부터 받으면 메세지 A를 재전송
- 메세지 A가 모든 replication에 완벽하게 복사되었는지 여부는 확인하지 않고 B를 전송
- 만약 리더가 메세지를 복제 중에 다운될 경우 다음 리더가 될 브로커에는 메세지가 없을 수 있기 때문에 소실할 우려가 있음
### acks all, -1(default)
- 리더 브로커가 메세지 A를 정상적으로 받은 뒤 min.insync.replicas 개수 만큼의 replication에 복제를 수행한 뒤에 보내는 Ack 메세지를 받은 후 다음 메세지인 메세지 B를 바로 전송.
  - 만약 오류 메세지를 브로커로 부터 받으면 메세지 A를 재전송
- 메세지 A가 모든 replicator에 완벽하게 복사되었는지의 여부까지 확인후에 메세지 B를 전송
- 메세지 손실이 되지 않도록 모든 장애 상황을 감안한 전송 모드이지만 ack를 상대적으로 오래 기다려야 하므로 전송속도가 느림
### 메세지 배치 전송의 이해
- send() -> Serializer(byte code로 변환) -> Partitioner(어떤 파티션으로 갈지 매핑) -> RecordAccumulator(메세지 배치) -> Sender(배치 전송)
- sender 스레드는 별도의 스레드로 sender 스레드가 브로커에게 메세지를 전송하기전에 recordAccumulator에서 메세지를 배치 단위로 읽어서 보낸다.
- kafka producer 객체의 send() 메소드는 호출 시마다 하나의 producerRecord를 입력하지만 바로 전송되지 않고 내부 메모리(recordAccumulator)에서
단일 메세지를 토픽 파티션에 따라 recordBatch 단위로 묶은 뒤 전송됨.
- 메세지들은 producer client의 내부 매모리에 여러개의 batch들로 buffer.memory 설정 사이즈 만큼 보관될 수 있으며 여러 개의 batch들로 한꺼번에 전송될 수 있음.
### record accumulator
- recordAccumulator는 파티셔너에 의해서 메세지 배치가 전송이 될 토픽과 파티션에 따라 저장되는 kafka producer 메모리 영역
- sender 스레드는 recordAccumulator에 누적된 메세지 배치를 꺼내서 브로커로 전송함
- kafka producer의 main thread는 send() 메소드를 호출하고 record accumulator에 데이터 저장하고 sender 스레드는 별개로 데이터를 브로커로 전송
### 옵션
- linger.ms : sender thread로 메세지를 보내기 전 배치로 메세지를 만들어서 보내기 위한 최대 대기 시간 
(쉽게 말하면 sender 스레드가 배치를 가져가기 전 이만큼만 기다려~ 하는것)
  - sender 스레드는 기본적으로 전송할 준비가 되어 있으면 record accumulator 에서 1개의 배치를 가져갈 수도, 여러개의 배치를 가져갈 수도 있음
  - batch에 메세지가 다 차지 않아도 가져갈 수 있음.
  - linger.ms를 0보다 크게 설정하여 sender 스레드가 하나의 record batch를 가져갈 때 일정 시간 대기하여 record batch에 메세지를 보다 많이 채울 수 있도록 적용
  - ❓ linger.ms에 대한 고찰
    - linger.ms를 반드시 0 보다 크게 설정할 필요는 없음
    - producer와 broker 간의 전송이 매우 빠르고 producer에서 메세지를 적절한 record accumulator에 누적된다면 0이 되어도 무방함.
    - 전반적인 producer와 broker 간의 네트워크 속도가 느리거나 producer에서 메세지를 보내는 속도가 느린 경우에는 0보다 크게 설정하는 것이 좋음.
    - 보통 20ms 이하로 설정 권장
    - <img width="1493" height="713" alt="스크린샷 2025-11-10 오후 11 10 19" src="https://github.com/user-attachments/assets/d46b2e03-cf28-4dc9-9d2f-f9a3c139a765" />

- buffer.memory : record accumulator의 전체 메모리 사이즈
- batch.size : 배치 하나의 최대 크기
- max.inflight.requests.per.connection: connection 당 최대 가져갈 수 있는 batch 개수
  - 브로커 서버의 응답없이 producer의 sender 스레드가 한번에 보낼 수 있는 메세지 배치의 개수
  - 기본값 5
  - ❗️producer 메세지 전송 순서와 broker 메세지 저장 순서 고찰
    - 가령 메세지 A, B 가 있다고 가정 (A가 B보다 먼저 생성된 메세지 배치)
    - max.in.flight.requests.per.connection = 2(> 1) 에서 A, B 2개의 배치 메세지를 전송 시 B는 성공적으로
    기록 되었으나 A의 경우 write 되지 않고 ack 전송이 되지 않는 fail 상황인 경우 producer는 A를 재 전송하여 성공적으로
    기록되며 producer의 원래 메세지 순서와는 다르게 broker에 저장될 수 있음.(같은 파티션인 경우)
    - 이러한 상황을 해결하기 위해서 enable.idempotence=true 설정을 통해서 producer의 메세지 전송 순서와 broker의 메세지 저장 순서를 동일하게 보장할 수 있음.
- delivery.timeout.ms : 메세지 전송 제한 시간(retry 포함)
  - ❗️ producer record가 record accumulator 에 저장되지 못하는 경우
    - record accumulator에 메세지를 저장할 수 있는 공간이 부족한 경우
    - max.block.ms 시간동안 record accumulator에 메세지를 저장할 수 없으면 send() 메소드는 예외를 발생시킴
- request.timeout.ms : 브로커로 부터 응답을 기다리는 최대 시간
  - 브로커로 부터 응답이 없으면 재전송
  - delivery.timeout.ms 보다 작게 설정해야함.
- retry.backoff.ms : 재전송 주기 시간
- ‼️ 필수! -> delivery.timeout.ms >= linger.ms + request.timeout.ms
- retries : 재전송 시도 횟수
  - 굉장히 크게 설정
  - 어차피 delivery.timeout.ms 시간 내에 재전송 시도가 끝나지 않으면 예외 발생
  - 보통 retries는 무한대값으로 설정, delivery.timeout.ms(120000 default, 2분)를 조정하는 것을 권장
- 📖 ex) retries = 10, request.timeout.ms=10000ms, retry.backoff.ms=30ms 라고 하면 request.timeout.ms 기다린 후 재 전송하기 전 30ms를 더 기다린 후
  재전송 시도, 이와 같은 방식으로 10회 시도하고 더 이상 retry 시도 x
  - 만약 10회 내에 delivery.timeout.ms 시간이 지나면 예외 발생하고 더 이상 retry를 진행하지 않음.
- enable.idempotence=true
  - producer는 브로커로 부터 ack를 받은 다음에 다음 메세지를 전송하되, producer id와 메세지 seq를 header에 저장하여 전송
  - 메세지 seq는 메세지의 고유 seq 번호. 0부터 시작하여 순차적으로 증가
  - 브로커에서 메세지 seq가 중복될 경우 이를 메세지 로그에 기록하지 않고 ack만 전송
  - 브로커는 producer가 보낸 메세지의 seq가 브로커가 가지고 있는 메세지의 seq보다 1만큼 큰 경우에만 브로커에 저장
  - producer 설정
    - enable.idempotence=true
    - acks=all
    - retries>0
    - max.inflight.requests.per.connection=5(기본값) between 1
      - 단, 1로 설정시 병렬 전송이 불가능하여 전체적인 전송 성능이 떨어짐.
  - 메세지 전송 순서 유지
    - 배치 B0, B1, B2 가 함께 전송됬다고 가정(max.inflight.requests.per.connection=3)
    - 브로커는 메세지 배치를 처리 시 write된 배치의 마지막 메세지 seq+1 이 아닌 배치 메세지가 올 경우
    OutOfOrderException을 생성하여 producer에게 전송
      - ex) B0 -> seq 0~10 B1 -> seq 11~20 B2 -> seq 21~30 인데 브로커에 도착한 seq이 11이 아닌 21이 온 경우
      OutOfOrderException 발생
### 전송 전략
- 최대 한번 전송(at most once) : 메세지를 한번만 전송, 전송 실패시 재전송하지 않음
  - acks=0, retries=0
- 적어도 한번 전송(at least once) : 메세지를 한번 이상 전송, 전송 실패시 재전송
  - acks=1 or all, retries>0
- 정확히 한번 전송(exactly once) : 메세지를 정확히 한번 전송, 중복 전송되지 않음
  - acks=all, retries>0, enable.idempotence=true
  - transaction 기반 전송: consumer -> process -> producer(주로 kafka stream api 사용)


## 🛍️ Consumer 
### Consumer Group과 Consumer
- 모든 Consumer들은 단 하나의 Consumer Group에 소속되어야 하며, Consumer Group은 1개 이상의 Consumer를 가질 수 있다.
- 파티션의 레코드들은 **단 하나의 Consumer에만** 할당
- Consumer Group내에 Consumer 변화가 있을 시 마다 파티션과 Consumer의 조합을 변경하는 Rebalancing 발생
- 보통은 파티션 하나마다 컨슈머 하나씩을 둔다.
- 모든 Consumer들은 고유한 그룹 아이디를 가지는 컨슈머 그룹에 소속됨.
- 서로 다른 컨슈머 그룹들에 속한 컨슈머들은 다른 컨슈머 그룹이 구독한 파티션을 구독해도 된다. (단, 같은 컨슈머 그룹의 컨슈머는 동일한 파티션을 구독할 수 없다.)
- kafka consumer group 생성 명령어 ```kafka-console-consumer --bootstrap-server [host]:[port] --group [group_id] --topic [topic name] --property print.key=true --property print.value=true --property print.partition=true```
- consumer groups 조회
  - ```kafka-consumer-groups --boostrap-server [host]:[port] --list```
  - ```kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group [group name]``` -> 상세정보 조회
- <img width="1221" height="204" alt="스크린샷 2025-09-15 오후 10 00 22" src="https://github.com/user-attachments/assets/c7bbe501-9d87-4d67-8160-7bf0e7bd08dd" />
- 위의 사진의 LAG 란 컨슈머 그룹이 소비하지 않은 토픽의 파티션에 쌓인 메세지의 갯수를 뜻한다.
### Consumer 그룹 삭제
- ```kafka-consumer-groups --bootstrap-server [host]:[port] --delete --group [group_name]``` - 단! consumer가 모두 내려져 있는 상태에서 그룹을 삭제할 수 있다.
### 🍎 Consumer 개요
- Consumer는 poll() 메소드를 이용하여 주기적으로 브로커의 토픽 파티션에서 메세지를 가져옴. 
  - 메세지를 성공적으로 가져왔으면 commit을 통해서 __consumer_offset에 다음에 읽을 offset 위치를 기재함.
- KafkaConsumer는 fetcher, ConsumerClientNetwork 등의 주요 내부 객체와 별도의 heart beat thread를 생성
- Fetch, ConsumerClientNetwork 객체는 broker의 토픽 파티션에서 메세지를 fetch 및 poll 수행
- Heart beat thread는 consumer의 정상적인 활동을 group coordinator(브로커)에 보고하는 역할을 수행(group coordinator는 주어진 시간동안 heart beat을 받지 못하면 consumer 들의 rebalance를 명령)
### KafkaConsumer의 주요 구성 요소와 poll() 메소드
- ```ConsumerRecords<K,V> consumerRecords = consumer.poll(Duration.ofMillis(1000));```
- 브로커나 Consumer 내부 Queue에 데이터가 있다면 바로 데이터를 반환
- 그렇지 않을 경우에는 1000ms 동안 데이터 fetch를 브로커에 계속 수행하고 결과 반환
- Fetcher : 브로커로 부터 메세지를 fetch하는 역할
  - LinkedQueue에 데이터가 있을 경우 Fetcher는 데이터를 가져오고 반환하며 poll() 수행 완료
  - LinkedQueue에 데이터가 없을 경우 fetcher는 브로커에 fetch 요청을 보냄 - ConsumerClientNetwork 객체를 통해서
- ConsumerClientNetwork : 브로커와 네트워크 통신을 담당하는 역할
  - ConsumerClientNetwork는 비동기로 계속 브로커의 메세지를 가져와서 LinkedQueue에 저장
### 옵션
- fetch.min.bytes: Fetcher가 record들을 읽어들이는 최소 bytes 크기, 브로커는 지정된 옵션 이상의 새로운 메세지가 쌓일때까지 전송을 하지 않음. 기본 1byte
- fetch.max.wait.ms: 브로커에 fetch.min.bytes 이상의 메세지가 쌓일 때까지 최대 대기 시간, 기본 500ms
- fetch.max.bytes: Fetcher가 한번에 가져올 수 있는 최대 데이터 bytes, 기본은 50MB
- max.partition.fetch.bytes: Fetcher가 한번에 가져올 수 있는 파티션 별 최대 데이터 bytes, 기본 1MB
- max.poll.records: Fetcher가 한번에 가져올 수 있는 최대 레코드 수, 기본 500
#### Consumer Fetcher 관련 주요 설정 파라미터 이해
- options
  - fetch.min.bytes=16384 (16KB) 
  - fetch.max.wait.ms=500
  - fetch.max.bytes=52428800 (50MB)
  - max.partition.fetch.bytes=1024168 (1MB)
  - max.poll.records=500(개)
- KafkaConsumer.poll(1000) 으로 수행 시
  - 가져올 데이터가 1건도 없으면 poll() 인자 시간만큼 대기 후 return
  - 가져와야 할 데이터가 많을 경우 max.partition.fetch.bytes로 배치 크기 설정. 그렇지 않을 경우 fetch.min.bytes로 배치 크기 설정
  - 가장 최신의 offset 데이터를 가져오고 있다면 fetch.min.bytes만큼 가져오고 return하고 fetch.min.bytes 만큼 쌓이지 않는다면 fetch.max.wait.ms 만큼 대기 후 return
  - 오랜 과거 offset 데이터를 가져온다면 최대 max.partition.fetch.bytes 만큼 가져오고 return
  - max.partition.fetch.bytes에 도달하지 못하여도 가장 최신의 offset에 도달하면 반환
  - 토픽에 파티션이 많아도 가져오는 데이터량은 fetch.max.bytes로 제한
  - Fetcher가 LinkedQueue에서 가져오는 레코드의 개수는 max.poll.records로 제한
- 결론: 기본적으로 batch size는 fetch.min.bytes로 설정되며, 가져올 데이터가 많을 경우 혹은  오랜과거 offset 데이터를 가져온다면 max.partition.fetch.bytes로 배치 크기가 설정됨.
  - fetch.min.bytes를 16KB로 설정하면 throughput이 향상 트래픽이 높으면 fetch.min.bytes를 크게 설정하는 것이 좋음.
  - 주의! fetch.min.bytes를 너무 크게 설정하면 메세지 지연이 발생할 수 있음.
  - 8~64KB 사이로 설정 권장
### 여러개의 파티션에 한개의 컨슈머 그룹 즉 여러 컨슈머가 속해 있을 경우
- 파티션이 3개 있다고 가정
- 컨슈머 그룹에서 컨슈머 1개 -> 3개로 늘어갈 경우
  - 리밸런싱을 통해 join group -> sync group -> assignment -> heartbeat -> poll() 메소드 수행
  - 파티션 3개를 컨슈머 3개가 나누어 가짐.
  - 컨슈머 1개가 죽을 경우 -> 리밸런싱 -> 파티션 3개를 컨슈머 2개가 나누어 가짐.
### __consumer_offsets 토픽 읽기
- consumer가 읽어드린 offset 위치를 저장하는 내부 토픽
- 1. consumer.config용 config 파일을 생성
   - ```echo "exclude.internal.topics=false" > consumer_temp.config``` 
- 2. __consumer_offsets 토픽을 읽기
   - ```kafka-console-consumer --consumer.config /home/master/consumer_temp.config --boostrap-server [host]:[port] --topic __consumer_offsets --formatter "kafka.coordinator.group.GroupMetadataManager\$OffsetsMessageFormatter"``` 
- 동일 consumer group으로 consumer가 새롭게 접속할 시 __consumer_offsets에 있는 offset 정보를 기반으로 메세지를 가져오기 때문에 earliest로 설정하여도 0번 오프셋부터 읽어 들이지 않음
- Consumer group의 consumer가 모두 종료되어도 consumer group이 읽어들인 offset 정보는 7일동안 __consumer_offsets에 저장되어 있음(offsets.retention.minutes)
- 해당 topic이 삭제되고 재 생성될 경우에는 해당 topic에 대한 consumer group의 offset 정보는 0으로 기록됨.
  - 따라서 topic에 쌓여있는 메세지를 처음부터 읽어 들이려면 topic을 삭제하고 재 생성해야함.
### consumer rebalancing
- consumer group내에 새로운 consumer가 추가되거나 기존 consumer가 종료 될때, 또는 topic에 새로운 파티션이 추가될 때 broker의 group coordinator는 consumer group내의 consumer들에게 파티션을 재할당하는 rebalancing을 수행하도록 지시
  1. consumer group내의 consumer가 브로커에 최초 접속 요청 시 group coordinator 생성
  2. 동일 group_id로 여러 개의 consumer로 broker의 group coordinator로 접속
  3. 가장 빨리 그룹에 join 요청을 한 consumer에게 consumer group 내의 leader consumer로 지정
  4. leader로 지정된 consumer는 파티션 할당 전략에 따라 consumer들에게 파티션 할당
  5. leader consumer는 최종 할당된 파티션 정보를 group coordinator에게 전달 
  6. 정보 전달 성공을 공유한 뒤 개별 consumer들은 할당된 파티션에서 메세지를 읽음
#### consumer group status
|상태| 설명                                                    |
|----|-------------------------------------------------------|
|empty| consumer group에 속한 consumer가 없음                       |
|PreparingRebalance| consumer group에 속한 consumer가 있으나 아직 파티션이 할당되지 않음      |
|CompletingRebalance| consumer group에 속한 consumer가 파티션 할당을 완료하고 메세지를 읽기 시작함 |
|Stable| consumer group에 속한 consumer가 파티션 할당을 완료하고 메세지를 읽고 안정됨 |
### consumer static group membership
- 많은 consumer를 가지는 consumer group에서 rebalance가 발생하면 모든 consumer들이 rebalance를 수행하므로 많은 시간이 소모되고 대량 데이터 처리 시
Lag가 더 길어질 수 있음 
- 유지보수 차원의 consumer restart도 rebalance를 초래하므로 불필요한 rebalance를 발생 시키지 않을 방법 대두
- consumer group내의 consumer들에게 고정된 id 부여
- consumer 별로 consumer group 최초 조인 시 할당된 파티션을 그대로 유지하고 consumer가 shutdown 되어도 session.timeout.ms 내에 재 기동되면 rebalance가 수행되지 않고, 기존 파티션이 재 할당됨.
<img width="1039" height="669" alt="스크린샷 2025-10-17 오후 9 51 34" src="https://github.com/user-attachments/assets/d5220bdc-e5ec-4265-95d6-ad2830a19fbc" />
- 그림에서 consumer #3 이 종료 되더라도 rebalance가 발생하지 않고, partition #3은 다른 consumer에게 재 할당되지 않음
- consumer #3 session.timeout.ms 내에 다시 기동되면 partition #3는 consumer #3에게 다시 할당됨.
- consumer #3가 session.timeout.ms 시간 내에 기동되지 않으면 rebalance가 발생하고 partition #3는 consumer 다른 컨슈머에게 재 할당됨.

### heartbeat와 poll() 관련 주요 consumer 파라미터

| consumer 파라미터명        | 기본값(ms)    | 설명                                                                                                                               |
|-----------------------|------------|----------------------------------------------------------------------------------------------------------------------------------|
| heartbeat.interval.ms | 3000(3초)   | heartbeat 스레드가 heart beat을 보내는 간격, session.timeout.ms 보다 낮게 설정되어야 함. session.timeout.ms의 1/3 보다 낮게 설정 권장                         |
| session.timeout.ms    | 45000(45초) | 브로커가 consumer로 heart beat을 기다리는 최대 시간, 브로커는 이 시간동안 heart beat을 consumer로 부터 받지 못하면 해당 consumer를 group에서 제외 및 rebalanacing 하도록 지시 |
| max.poll.interval.ms  | 300000(5분) | 이전 poll() 호출 후 다음 호출 poll() 까지 브로커가 기다리는 시간, 해당 시간동안 호출이 consumer로 부터 이뤄지지 않으면 해당 consumer는 문제가 있다고 판단하고 브로커는 rebalance 함. |
### Consumer rebalancing Protocol
- 버전에 따라 상이
#### Eager 모드
- conumser가 기본으로 설정하는 모드
- 기존 consumer들의 모든 파티션 할당을 취소하고 잠시 메세지를 읽지 않음. 이후 새롭게 consumer에 파티션을 다시 할당 받고 다시 메세지를 읽음.
- 할당이 한번에 일어남
- 모든 consumer가 잠시 메세지를 읽지 않는 시간으로 인해 Lag가 상대적으로 크게 발생할 가능성 존재
#### Cooperative 모드
- rebalance 수행 시 기존 consumer들의 모든 파티션 할당을 취소하지 않고 대상이 되는 consumer들에 대해서 파티션에 따라 점진적으로 consumer를 할당하면서 rebalance를 수행
- 전체 consumer가 메세지 읽기를 중지하지 않으며 개별 consumer가 협력적으로 영향을 받는 파티션만 rebalance로 재분배.
- 많은 consumer를 가지는 consumer group에서 rebalance 시간이 오래 걸릴 시에 활용도 높음!
### Consumer 할당 전략

| 파티션 할당 전략                     |내용|
|-------------------------------|----|
| Range 할당 전략                   |- 서로 다른 2개 이상의 토픽을 consumer들이 subscribe 할 경우 토픽별 동일한 파티션을 특정 consumer에게 할당하는 전략<br> - 여러 토픽들에서 동일한 키값으로 되어 있는 파티션은 특정 Consumer에 할당하여 <b style="color: pink">해당 consumer가 여러토픽의 동일 키값으로 데이터 처리를 용이하게</b> 할 수 있도록 지원 </br> - rebalancing 시에도 토픽들의 파티션과 consumer들을 균등하게 매핑하게 하므로 rebalance 이전의 파티션과 consumer들의 매핑이 변경되기 쉬움.
| Round Robin 할당 전략             |파티션 별로 consumer들이 <b style="color: skyblue">균등하게 부하를 분배</b>할 수 있도록 여러 토픽들의 파티션들을 consumer들에게 순차적인 round robin 방식으로 할당|                
| Sticky 할당 전략                  |<br>최초에 할당된 <b style="color: lightgreen">파티션과 consumer 매핑을 rebalance 수행되어도 가급적 그대로 유지</b> 할 수 있도록 지원하는 전략</br> 하지만 위에 써있는 Eager protocol 기반이므로 rebalance시 모든 consumer의 파티션 매핑이 해제된 후 다시 매핑됨.|
| Cooperative(협력적) Sticky 할당 전략 |최초에 할당된 파티션과 Consumer 매핑을 rebalance 수행되어도 가급적 그대로 유지할 수 있도록 지원 + Cooperative Protocol 기반으로 <b style="color: orange">Rebalanace시 모든 Consumer의 파티션 매핑이 해제되지 않고 rebalance 연관된 파티션과 consumer만 재 매핑됨</b> <br> Kafka가 지속적으로 발전시키는 중이라고 함|
#### Round-Robin 할당 전략 vs Range 할당 전략
##### 1️⃣ CASE 1
- <img width="1014" height="751" alt="스크린샷 2025-10-22 오후 9 58 29" src="https://github.com/user-attachments/assets/f3164905-e94f-415a-88ed-0ebde435c519" />
- <img width="1051" height="814" alt="스크린샷 2025-10-22 오후 10 03 00" src="https://github.com/user-attachments/assets/6ef98c3c-b117-4b04-ab70-0ae92f0b86bb" />
- 두 그림 모두 동일하지만 case 2로 가면 매핑이 달라진다.
- RoundRobin 전략은 순차적으로 partition들을 consumer에 할당하기 때문에 Topic A 에서 Partition #1 -> Consumer #1, Partition #2 -> Consumer #2 이런식으로 순차적으로 할당되게 된다.
- Range 전략은 Topic A의 Partition #1 -> Consumer #1 로 매핑됬다면 Topic B의 Partition #1 도 Consumer #1 로 동일한 번호의 파티션은 동일한 consumer로 매핑된다.
##### 2️⃣ Case 2
- <img width="974" height="741" alt="스크린샷 2025-10-22 오후 10 05 03" src="https://github.com/user-attachments/assets/7f545099-cf12-49bc-880c-ca9f927597b4" />
- Round Robin은 Topic A에서 Partition #1 이 Consumer #1에 할당 -> Partition #2이 순차적으로 Consumer #2에 할당 -> Partition #3 -> 그 다음인 Consumer #1 에 할당 이런식으로 순차적으로 할당됨
- <img width="1027" height="740" alt="스크린샷 2025-10-22 오후 10 07 14" src="https://github.com/user-attachments/assets/0af247be-262d-4368-96be-6ac73feb9580" />
- 이번에는 Topic A 의 Partition #1은 Consumer #1에 할당, Topic B의 Partition #1은 Topic A의 Partition #1을 보고 같은 Consumer에 할당 이런식으로 같은 consumer를 계속 가리킴.
- 다른 Topic의 파티션이지만 같은 키를 가지는 파티션들은 같은 consumer에 매핑되도록 유도
##### Cooperative Sticky 할당 전략
- 테스트 결과 rebalance 시에 파티션과 consumer 매핑이 거의 유지됨. 하지만 100% 보장되지는 않음
### 🐻 Consumer Offset Commit
- Consumer는 subscribe() 를 호출하여 읽어 들이려는 토픽을 등록
- Consumer는 poll() 메소드를 이용하여 주기적으로 브로커의 토픽 파티션에 메세지를 가져옴
- 메세지를 성공적으로 가져왔으면 commit을 통해서 __consumer_offsets에 다음에 읽을 offset 위치를 기재함.
#### Offset Commit 방식
- __consumer_offsets에는 consumer group이 특정 topic의 파티션별로 읽기 commit한 offset 정보를 가짐. 특정 파티션을 어느 consumer가 commit했는지 정보를 가지지 않음.
#### 중복 읽기 상황
- consumer가 poll() 메소드를 통해서 메세지를 가져오고 commit() 메소드를 호출하여 offset을 commit하기 전에 consumer가 비정상 종료될 경우
- 다시 기동 혹은 rebalancing된 consumer는 commit 되지 않은 offset 위치부터 메세지를 다시 읽어 들이므로 중복 읽기가 발생할 수 있음.
- ⭐️ consumer는 poll() 할때 record를 기준으로 읽지 않고 batch 단위로 읽어드림.
#### 읽기 누락 상황
- consumer가 poll() 메소드를 통해서 메세지를 가져오고 commit() 메소드를 호출하여 offset을 commit한 후에 consumer가 비정상 종료될 경우
- 즉 poll()과 거의 동시에 commit()이 호출된 경우 commit된 offset 위치부터 메세지를 다시 읽어 들이므로 읽기 누락이 발생할 수 있음.
### Consumer의 auto commit
- Consumer의 파라미터로 auto.enable.commit=true 인 경우 읽어온 메세지를 브로커에 바로 commit 적용하지 않고, auto.commit.interval.ms(기본 5초) 에 정해진 주기마다 consumer가 자동으로 commit 수행
- Consumer가 읽어온 메세지보다 브로커의 commit이 오래되었으므로 consumer의 장애/재기동 및 rebalance 후 브로커에서 이미 읽어온 메세지를 다시 읽어와서 중복처리 될 수 있음.
- ex) 첫번째 poll() : 2초 0,1번 record 읽음 -> 두번째 poll() : 4초 2,3번 record 읽음 -> 세번째 poll() : 6초에 이제 3번까지 읽었다는 commit을 브로커에 보냄
### Consumer의 동기/비동기 commit
| Sync                                                                                                                                                                                                                                                                                                        | Async                                                                                                                                                                                                                                                                                             |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| - consumer 객체의 commitSync() 메소드를 사용<br/> - 메시지 배치를 poll()을 통해서 읽어오고 해당 메세지들의 마지막 offset을 브로커에 commit<br/> - 브로커에 commit 적용이 성공적으로 될때까지 블로킹 적용<br/> - Commit적용 완료 후에 다시 메세지 읽어옴<br/> - 브로커에 commit 적용이 실패할 경우 다시 commit 요청<br/> commit 실패 시 무한정 재시도 하는것이 아니라 request.timeout.ms 동안 retry.backoff.ms 간격으로 재시도 | - Consumer객체의 commitAync() 메소드 사용<br/> - 메세지 배치를 poll()을 통해서 읽어오고 해당 메세지들의 마지막 offset을 브로커에 commit적용 요청하지만 브로커에 commit 적용이 성공적으로 되었음을 기다리지 않고 계속 메세지를 읽어옴.<br/> - 브로커에 commit 적용이 실패해도 다시 commit 시도 안함. 때문에 consumer 장애 또는 rebalance 시 한번 읽은 메세지를 다시 중복해서 가져 올 수 있음. <br/> - 동기 방식 대비 더 빠른 수행 가능  |

### Consumer 에서 Topic의 특정 파티션만 할당하기
- Consumer 에게 여러개의 파티션이 있는 Topic 에서 특정 파티션만 할당 가능. 배치 처리 시 특정 key 레벨의 파티션을 특정 Consumer에게 할당하여 처리할 경우 적용
- KafkaConsumer의 assign() 메소드에 TopicPartition 객체로 특정 파티션을 인자로 입력하여 할당
### Consumer에서 특정 offset 위치부터 메세지 읽기
- Consumer에게 특정 offset 위치부터 메세지를 읽도록 지시 가능
- seek() 메소드에 TopicPartition 객체와 offset 위치를 인자로 입력하여 지시
- seek() 메소드 호출 시 주의점!
  - poll() 메소드 호출 전에 seek() 메소드를 호출해야함.
  - commit() 을 하지말고 읽어야함
  - 기존 group_id로 consumer를 기동할 경우 commit된 offset 위치부터 메세지를 읽어오기 때문에 seek() 메소드로 지정한 offset 위치부터 읽어오지 않음.
  - 따라서 group_id를 변경하거나 consumer group을 삭제하고 재 생성한 후에 seek() 메소드를 호출해야함.
- seek() 의 목적은 원래 일하던 consumer group은 rebalancing 하게 냅두고 해당 컨슈머에서 다른 group_id를 할당하여 디버깅 목적 용도로 많이 사용된다.
## 📘 Kafka Config
### Broker와 Topic 레벨 Config
- Broker에서 설정할 수 있는 config는 상당히 많다. Broker 레벨에서의 config는 재기동을 해야 반영되는 static config이고 topic config는 동적으로 사용이 가능하다.
- topic config는 broker config의 설정을 override 시킬 수 있다.
### Config 사용하기
- Config 값 확인: ```kafka-configs --bootstrap-server [host]:[port] --entity-type [brokers/topics] --entity-name [broker id/topic name] --all --describe```
- Config 값 설정: ```kafka-configs --bootstrap-server [host]:[port] --entity-type [brokers/topics] --entity-name [broker id/topic name] --alter --add-config property명=value```
- Config 값 unset: ```kafka-configs --bootstrap-server [host]:[port] --entity-type [brokers/topics] --entity-name [broker id/topic name] --alter --delete-config property명```
## 🗂️ Kafka log 확인
### kafka-dump-log
- 명령어 : ```kafka-dump-log --deep-iteration --files [log 파일 위치] --print-data-log```
- 여기서 log 파일 위치는 카프카 설정에서 생성한 로그 파일 위치의 찾고 싶은 토픽 name 경로로 들어가면 ~.log 로 찾을 수 있다.

## Internal IP + External IP 바인딩 설정
- 카프카 브로커가 내부망에 위치하고 외부망에서 접근하는 경우(도커 컨테이너 환경)
- External IP로 접근하게 하려면 해당 컨테이너의 host 네트워크 IP를 EXTERNAL_ADVERTISED_LISTENERS에 설정
- 내부망 + local host 에서 접근하는 경우 INTERNAL_ADVERTISED_LISTENERS에 내부망 IP를 설정
- 이 때 만약 망연계 장비가 외부망과 내부망 사이에 존재할 경우 EXTERNAL_ADVERTISED_LISTENERS에 망연계 장비의 IP를 설정
- kafka container 의 host에 있는 spring service 에서 접근할 시에는 망연계 장비의 ip가 아니라 host의 ip를 명시해줘야함.
- 이떄 host는 9092가 아니라 다른 포트 즉 19092 같은 포트를 매핑해줘야함.
  - ex) 
  - ``` 
    ---docker-compose.yml---
    ports:
    - "19092:19092"
    - "9092:9092"
    ```
- 예시
  - 망연계 장비 ip : 192.168.30.89
  - 내부망의 kafka container host ip : 192.168.10.89
  - 이때 kafka를 docker에서 운영한다면 docker-compose.yml 에서 ADVERTISED_LISTENERS를 다음과 같이 설정
  ```
  docker-compose.yaml
    KAFKA_ADVERTISED_LISTENERS: INTERNAL://192.168.10.89:19092,EXTERNAL://192.168.30.89:9092