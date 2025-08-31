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
