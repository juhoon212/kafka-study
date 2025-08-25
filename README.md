# 📖 kafka-study
## 🔍 환경
- UTM에 Ubuntu 22.04 마운트 
- confluent-kafka 7.8.1 tar download
- tar -xvf [해당 버전 tar]
- .bashrc에 ```export CONFLUENT_HOME=/home/master/confluent``` 홈 경로 설정
- PATH 설정 ```export PATH=.:$PATH:$CONFLUENT_HOME/bin``` 후 ```source ~./bashrc```
- home 경로에 zookeeper 실행 스크립트 및 kafka 실행 스크립트 작성
- zookeeper_start.sh : ```$CONFLUENT_HOME/bin/zookeeper-server-start $CONFLUENT_HOME/etc/kafka/zookeeper.properties```  kafka_start.sh : ```$CONFLUENT_HOME/bin/kafka-server-start $CONFLUENT_HOME/etc/kafka/server.properties```
