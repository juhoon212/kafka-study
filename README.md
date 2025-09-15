# 📖 kafka-study
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

## 🛍️ Consumer 
### Consumer Group과 Consumer
- 모든 Consumer들은 단 하나의 Consumer Group에 소속되어야 하며, Consumer Group은 1개 이상의 Consumer를 가질 수 있다.
- 파티션의 레코드들은 **단 하나의 Consumer에만** 할당
- Consumer Group내에 Consumer 변화가 있을 시 마다 파티션과 Consumer의 조합을 변경하는 Rebalancing 발생
- 보통은 파티션 하나마다 컨슈머 하나씩을 둔다.
- 모든 Consumer들은 고유한 그룹 아이디를 가지는 컨슈머 그룹에 소속됨.
- 서로 다른 컨슈머 그룹들에 속한 컨슈머들은 다른 컨슈머 그룹이 구독한 파티션을 구독해도 된다. (단, 같은 컨슈머 그룹의 컨슈머는 동일한 파티션을 구독할 수 없다.)
- kafka consumer group 생성 명령어 ```kafka-console-consumer --bootstrap-server [host]:[port] --group [group_id] --topic [topic name] --property print.key=true --property print.value=true --property print.partition=true```
