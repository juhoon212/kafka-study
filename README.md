# 📖 CLI 실습
## 🔍 환경
- UTM에 Ubuntu 22.04 마운트 
- confluent-kafka 7.8.1 tar download
- tar -xvf [해당 버전 tar]
- .bashrc에 ```export CONFLUENT_HOME=/home/master/confluent``` 홈 경로 설정
- PATH 설정 ```export PATH=.:$PATH:$CONFLUENT_HOME/bin``` 후 ```source ~./bashrc```
## 🔍 서버 설정
- home 경로에 zookeeper 실행 스크립트 및 kafka 실행 스크립트 작성
- zookeeper_start.sh : ```$CONFLUENT_HOME/bin/zookeeper-server-start $CONFLUENT_HOME/etc/kafka/zookeeper.properties```  kafka_start.sh : ```$CONFLUENT_HOME/bin/kafka-server-start $CONFLUENT_HOME/etc/kafka/server.properties```
- 기존의 로그 경로를 /tmp 아래에서 아래 사진의 경로로 변경한다. ```cd $CONFLUENT_HOME/etc/kafka``` 경로의 server.properties 들어가서 바꾼다.
- ```cd $CONFLUENT_HOME/etc/kafka``` 에 zookeeper.properties에 들어가서도 해당 로그 경로를 home 디렉토리로 변경한다.
  - 이유는 vm 재부팅시 /tmp 하위는 데이터가 날아갈 수 있어서 이다. 
- <img width="776" height="319" alt="스크린샷 2025-08-31 오후 4 02 11" src="https://github.com/user-attachments/assets/24049e68-b51b-48c5-b042-c6453b2c82e7" />

### confluent vs apache-kafka
- 자세한 더 알아봐야겠지만 일단 confluent kafka는 etc 밑에 해당 라이브러리의 파일이 있는 반면 apache-kafka는 config 디렉토리 밑에 파일이 있다.
- 또한 confluent-kafka는 커뮤니티 버전은 제외하고 다른 버전에서 ksqldb를 지원한다.

## ⛄️ Topic 생성 및 정보 확인

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

## 🐶 Kafka Cli 에서 메세지 쓰고 읽기
### Produce
- ```kafka-console-producer --bootstrap-server [host]:[port] --topic [topic name]``` 을 치면 > 표시가 나오고 메세지를 produce 할 수 있다.
- aaa, bbb, ccc 등 메세지를 produce 해보자
- 참고로 Producer가 메세지를 broker에 넣는 과정에서는 send() -> Serializer(byte code로 변환) -> Partitioner(어떤 파티션으로 갈지 매핑) 하는 과정을 거친다.
### Consume
- ```kafka-console-consumer --bootstrap-server [host]:[port] --topic [topic name]``` 을 치면 consume 할 수 있다.
- 🚨 그런데 메세지를 consume하지 않는다. 왜 그러지?
- 🫸 Kafka의 consumer는 auto.offset.reset 기능을 가지고 있다. 이것이 무엇이냐? >>> 바로 Consumer가 Topic에 처음 접근하여 메세지를 가져올때 가장 처음(오래된) 메세지부터 가져올 것인지 아니면 가장 최근의 메세지를 가져올 것인지 판단하는 기능이다.
- Default는 lastest로 되어있어 먼저 produce한 메세지들은 가지고 오지 않는 것이다
- 이럴때는 ```kafka-console-consumer --bootstrap-server [host]:[port] --topic [topic name] --from-beginning``` 이렇게 치면 전의 메세지 즉 가장 오래된 메세지부터 가져올 수 있게된다.

## 🗝️ Key 값을 가지지 않는 메세지 전송
- 메세지는 producer를 통해 전송 시 Partitioner를 통해서 어떤 파티션으로 갈지 라우팅 됨.
- Key 값을 가지지 않는 경우: Round robin, sticky partition등의 파티션 전략등이 선택되어 파티션 별로 메세지 전송됨.
  - Round robin : 메세지 배치를 순차적으로 다른 파티션으로 전송함 - 구버전
  - sticky partition 이란? 특정 파티션으로 전송되는 하나의 배치에 메세지를 빠르게 먼저 채워서 보내는 방식
- Topic이 복수 개의 파티션을 가질때 메세지 순서가 보장되지 않음.
### 분배 전략
- RR(round robin) : kafka 2.4버전 이전 기본 파티션 분배 전략 - 메세지 배치를 순차적으로 **다른** 파티션으로 전송함 ex) batch size가 다 차면 전송 설정(batch.size)
  - 메세지가 배치 데이터를 빨리 채우지 못하면서 전송이 늦어짐, 배치를 다 채우지 못하고 전송하면서 전송 성능이 떨어짐 
- Sticky Partitioning - kafka 2.4 버전부터 기본 파티션 분배 전략
  - 특정 파티션으로 전송되는 하나의 배치에 메세지를 빠르게 먼저 채워서 보내는 방식
  - 일정시간 동안 특정 파티션에 메세지를 sticky하게 쭉 모은다.
  - 배치가 꽉 차거나 linger.ms 가 지나면 그때 새로운 파티션을 선택해서 다시 sticky하게 쭉 모은다.
  - 배치 사이즈를 크게 키울수 있어서 성능 향상 
## 🔐 Key 값을 가지는 매세지 전송
- 특정 Key 값을 가지는 메세지는 특정 파티션으로 고정되어 전송된다.
### ‼️ 알아둘 점!
- 카프카는 하나의 파티션 내에서만 메세지 순서를 보장한다.
### Kafka cli로 키 값 있는 메세지 produce && consume
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
