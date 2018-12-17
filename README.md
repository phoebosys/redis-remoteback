# Redis-Remoteback: A Redis Remote Backup System

Redis-Remoteback is a Redis Remote Backup System for disaster recovery. It has two modules connected by kafka.
1. proxy module: It is a redis proxy, a redis client connect to proxy module, and proxy module connect to redis server, 
as while proxy clone redis messages to senders.
A sender can be a log sender which write message to log, or a kafka sender which send message to a kafka topic.
2. replay module: It works on a remote distance, receive redis messages from kafka, replay messages to back redis.


# Proxy Module Condigure
Use config.toml configure proxy module details:
* `bindip` and `bindport` is the listen IP address and Port.
* `connip` and `connport` is the local redis server IP address and Port.
* `message` select redis message type, it has 3 choice, `ROW` means proxy do not analyze redis protol, looking it as String;
`STRING` means proxy analyze redis protocol and turn it to string command, such as `get key1`; 
`PROTOCOL` means proxy analyze redis protocol and directly use multi-line  format, such as `*2\n$3\nget\n$4\nkey1\n`.
* `worker` select what technique was used, kotlin `coroutine`, RxJava `rx` or simple `thread`.
* `rsptimeout` set the timeout when waiting redis reply, it is used only for `STRING` or `PROTOCOL` message.
* `sender`s config sender, `class` is the implementation class. `LogRxSender` for test and monitor redis protocol, 
it's `logger` and `level` match `log4j2.xml`'s config.
`KafkaSender` set how to connect kafka 2.0 server. Because of read commands of redis are useless for remote backup, so 
we can use `filter` to point out what commands will be send to kafka. The default config are all write commands of redis 5.0.2. 
`topic` describe the kafka topic. the others are kafka config, the `.` in key should be replaced with `-`, for example, 
`bootstrap-servers` means `bootstrap.servers` of kafka.

# Reply Module Condigure
TODO continue...

# Run
1. Download, install and start redis. https://redis.io/
2. Download, install and start kafka(with zookeeper). http://kafka.apache.org/
3. Config and start proxy module, replay module. The main class of proxy is `ys.phoebos.redis.proxy.Main.kt`.
TODO continue...
