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
