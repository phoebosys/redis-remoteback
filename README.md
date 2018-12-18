# Redis-RemoteBack: A Redis Remote Backup System

Redis-RemoteBack is a Redis Remote Backup System for disaster recovery. It has two modules connected by Kafka.
1. proxy module: It is a Redis proxy, a Redis client connect to the proxy module, and the proxy module connect to a Redis server, 
meanwhile proxy clone Redis messages to senders.
A sender can be a log sender which write messages to the log or a Kafka sender which send messages to the Kafka.
2. replay module: It works on a remote distance, receives Redis messages from Kafka, replays messages to back Redis.


# Proxy Module Configure
Use `config.toml` configure proxy module details:
* `bindip` and `bindport` is the listening IP address and Port of the proxy module.
* `connip` and `connport` is the IP address and Port of local Redis server.
* `message` select Redis message type, it has 3 choice, `ROW` means proxy do not analyze Redis protocol, looking it as String;
`STRING` means proxy analyze Redis protocol and turn it to string command, such as `get key1`; 
`PROTOCOL` means proxy analyze Redis protocol and directly use multi-line  format, such as `*2\n$3\nget\n$4\nkey1\n`.
* `worker` select what technique was used, `coroutine` of Kotlin, `rx` means RxJava, or simple java `thread`.
* `rsptimeout` set the timeout when waiting for Redis reply, it is used only for `STRING` or `PROTOCOL` message.
* `sender`s config sender, `class` is the implementation class. `LogRxSender` for test and monitor Redis protocol, 
it's `logger` and `level` match `log4j2.xml`'s config.
`KafkaSender` set how to connect Kafka 2.0 server. Because of readonly commands of Redis are useless for remote backup, so 
we can use the `filter` to point out what commands will be sent to Kafka. The default config is all writing commands of Redis 5.0. 
`topic` describes the Kafka topic. the others are Kafka config, the `.` in keys should be replaced with `-`, for example, 
`bootstrap-servers` means `bootstrap.servers` of Kafka config.

# Reply Module Configure
TODO continue...

# Run
1. Download, install and start Redis. https://redis.io/
2. Download, install and start Kafka(with zookeeper). http://kafka.apache.org/
3. Config and start the proxy module, the replay module. The main class of proxy is `ys.phoebos.redis.proxy.Main.kt`.
TODO continue...
