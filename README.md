# CookIM - 一个基于akka的分布式websocket聊天程序

![CookIM logo](docs/cookim.png)

- [中文文档](README_CN.md)
- [English document](README.md)

---

### 目录

1. [演示](#演示)
    1. [PC演示](#PC演示)
    1. [手机演示](#手机演示)
    1. [演示地址](#演示地址)
1. [以Docker方式启动单节点CookIM](#以docker方式启动单节点cookim)
    1. [获取镜像](#获取镜像)
    1. [运行容器](#运行容器)
    1. [调试容器](#调试容器)
    1. [停止容器](#停止容器)
1. [以Docker-Compose方式启动CookIM集群](#以docker-compose方式启动cookim集群)
    1. [启动集群](#启动集群)
    1. [增加节点](#增加节点)
    1. [停止集群](#停止集群)
1. [手动安装前准备](#手动安装前准备)
    1. [安装Java8+](#安装java8)
    1. [安装Scala2.11+](#安装scala211)
    1. [安装SBT0.13+](#安装sbt013)
    1. [安装Node5+](#安装node5)
    1. [安装MongoDB3+](#安装mongodb3)
1. [运行](#运行)
    1. [获取源代码](#获取源代码)
    1. [下载node依赖包](#下载node依赖包)
    1. [开启mongoDB服务](#开启mongodb服务)
    1. [下载sbt的jar依赖包](#下载sbt的jar依赖包)
    1. [使用预打包的libs运行程序](#使用预打包的libs运行程序)
    1. [启动CookIM服务](#启动cookim服务)
    1. [打开浏览器，访问以下网址8080](#打开浏览器访问以下网址8080)
    1. [启动另一个CookIM服务](#启动另一个cookim服务)
    1. [打开浏览器，访问以下网址8081](#打开浏览器访问以下网址8081)
1. [架构](#架构)
    1. [整体服务架构](#整体服务架构)
    1. [akka stream websocket graph](#akkastreamwebsocketgraph)
    1. [MongoDB数据库说明](#mongodb数据库说明)
    1. [消息类型](#消息类型)

---
[返回目录](#目录)

###演示

#### PC演示

![screen snapshot](docs/screen.png) 

#### 手机演示

![screen snapshot](docs/screen2.png)


#### 演示地址
[https://im.cookeem.com](https://im.cookeem.com)

---
[返回目录](#目录)

1. [运行在Docker](#运行在docker)
    1. [获取镜像](#获取镜像)
    2. [运行容器](#运行容器)
    
    
### 以Docker方式启动单节点CookIM
  
---

#### 获取镜像

```sh
$ sudo docker pull cookeem/cookim
```
---
[返回目录](#目录)

#### 运行容器

```sh
$ sudo docker run -d -p 8080:8080 cookeem/cookim
```

浏览器访问：
> http://localhost:8080

如果想修改HTTP端口为18080，可以使用如下命令：

```sh
$ sudo docker run -d -p 18080:8080 cookeem/cookim
```

---

[返回目录](#目录)

#### 调试容器

以下命令可以获取容器ID
```
$ sudo docker ps
       CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
       9c353289cf37        cookeem/cookim      "/root/cookim/entry.s"   4 seconds ago       Up 2 seconds        0.0.0.0:8080->8080/tcp   stoic_borg
```

运行以下命令进入容器进行调试
```sh
$ sudo docker exec -ti #CONTAINER ID# /bin/bash
```

---

[返回目录](#目录)

#### 停止容器

以下命令停止容器
```sh
$ sudo docker stop #CONTAINER ID#
$ sudo docker rm #CONTAINER ID#
```

---

[返回目录](#目录)

### 以Docker-Compose方式启动CookIM集群

#### 启动集群

进入CookIM所在目录，运行以下命令，以docker-compose方式启动CookIM集群，该集群启动了三个容器：mongodb、cookim1、cookim2
```sh
$ sudo docker-compose up -d
Creating docker_mongodb_1
Creating docker_cookim1_1
Creating docker_cookim2_1
```

成功启动集群后，浏览器分别访问以下网址，对应不同的CookIM服务
> http://localhost:8080
> http://localhost:8081

---

[返回目录](#目录)

#### 增加节点

可以通过修改docker-compose.yml文件增加CookIM服务节点，例如增加第三个节点：

```yaml
      cookim3:
        image: cookeem/cookim-cluster
        volumes:
         - /tmp:/root/cookim/upload
        environment:
          HOST_NAME: "cookim3"
          WEB_PORT: "8080"
          AKKA_PORT: "2551"
          SEED_NODES: "cookim1:2551"
        ports:
         - "8082:8080"
        depends_on:
         - mongodb
         - cookim1
```
---

[返回目录](#目录)

#### 停止集群
```sh
$ sudo docker-compose stop
$ sudo docker-compose rm
```
---

[返回目录](#目录)

### 手动安装前准备

---

[返回目录](#目录)

#### 安装Java8+

下载jdk8二进制文件，下载链接位于：

```sh
http://www.oracle.com/technetwork/java/javase/downloads/index.html
```

选择相应的版本，二进制文件地址例如：

```sh
$ wget http://download.oracle.com/otn-pub/java/jdk/8u111-b14/jdk-8u111-linux-x64.tar.gz
```

把二进制文件放到对应的目录，并解压二进制文件：
```sh
$ tar zxvf jdk-8u111-linux-x64.tar.gz
```

设置全局环境变量，在文件末尾增加以下配置：
```sh
$ sudo vi /etc/profile
export JAVA_HOME=<Your java binary directory>
export CLASSPATH=$JAVA_HOME/lib/tools.jar
export PATH=$JAVA_HOME/bin:$PATH
```

重新打开一个终端，让环境变量生效，检查java安装是否正确：
```sh
$ java -version
java version "1.8.0_65"
Java(TM) SE Runtime Environment (build 1.8.0_65-b17)
Java HotSpot(TM) 64-Bit Server VM (build 25.65-b01, mixed mode)
```

---

[返回目录](#目录)

#### 安装Scala2.11+

下载scala2.11，下载链接位于：
```sh
http://scala-lang.org/download/all.html
```

选择相应的版本，二进制文件地址例如：

```sh
$ wget http://downloads.lightbend.com/scala/2.11.8/scala-2.11.8.tgz
```

把二进制文件放到对应的目录，并解压二进制文件：
```sh
$ tar zxvf scala-2.11.8.tgz
```

设置全局环境变量，在文件末尾增加以下配置：
```sh
$ sudo vi /etc/profile
export SCALA_HOME=<Your scala binary directory>
export PATH=$PATH:$SCALA_HOME/bin
```

重新打开一个终端，让环境变量生效，检查scala安装是否正确：
```sh
$ scala
  Welcome to Scala 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_65).
  Type in expressions for evaluation. Or try :help.
  
  scala> 
```

---

[返回目录](#目录)

#### 安装SBT0.13+

下载sbt0.13.13，下载链接位于：
```sh
http://www.scala-sbt.org/download.html
```

选择相应的版本，二进制文件地址例如：

```sh
$ wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz
```

把二进制文件放到对应的目录，并解压二进制文件：
```sh
$ tar zxvf sbt-0.13.13.tgz
```

设置全局环境变量，在文件末尾增加以下配置：
```sh
$ sudo vi /etc/profile
export SBT_HOME=<Your sbt binary directory>
export PATH=$PATH:SBT_HOME/bin
```

重新打开一个终端，让环境变量生效，检查sbt安装是否正确：
```sh
$ sbt
[info] Set current project to cookeem (in build file:/Users/cookeem/)

```
---

[返回目录](#目录)

#### 安装Node5+

下载NodeJS，下载链接位于：
```sh
https://nodejs.org/en/download/
```

选择相应的版本，二进制文件地址例如：

```sh
$ wget https://nodejs.org/dist/v6.9.1/node-v6.9.1-linux-x64.tar.xz
```

把二进制文件放到对应的目录，并解压二进制文件：
```sh
$ tar zxvf node-v6.9.1-linux-x64.tar.xz
```

设置全局环境变量，在文件末尾增加以下配置：
```sh
$ sudo vi /etc/profile
export NODE_HOME=<Your node binary directory>
export PATH=$PATH:NODE_HOME/bin
```

重新打开一个终端，让环境变量生效，检查node和npm安装是否正确：
```sh
$ npm -v
3.8.3

$ node -v
v5.10.1
```
---

[返回目录](#目录)

#### 安装MongoDB3+

下载mongoDB，下载链接位于：
```sh
https://www.mongodb.com/download-center?jmp=nav#community
```

选择相应的版本，二进制文件地址例如（3.4.X版本测试有问题，请选择低版本）：

```sh
$ wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-amazon-3.2.9.tgz
```

把二进制文件放到对应的目录，并解压二进制文件：
```sh
$ tar zxvf mongodb-linux-x86_64-amazon-3.2.9.tgz
```

设置全局环境变量，在文件末尾增加以下配置：
```sh
$ sudo vi /etc/profile
export MONGODB_HOME=<Your mongoDB binary directory>
export PATH=$PATH:MONGODB_HOME/bin
```

创建新的目录，mongodb默认数据文件位于/data/db
```sh
$ sudo mkdir -p /data/db
```

重新打开一个终端，让环境变量生效，启动mongodb，默认端口为27017：
```sh
$ mongod
2016-12-06T17:24:06.268+0800 I CONTROL  [initandlisten] MongoDB starting : pid=2854 port=27017 dbpath=/data/db 64-bit host=cookeemMac.local
2016-12-06T17:24:06.268+0800 I CONTROL  [initandlisten] db version v3.2.9
2016-12-06T17:24:06.268+0800 I CONTROL  [initandlisten] git version: 22ec9e93b40c85fc7cae7d56e7d6a02fd811088c
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten] OpenSSL version: OpenSSL 0.9.8zh 14 Jan 2016
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten] allocator: system
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten] modules: none
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten] build environment:
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten]     distarch: x86_64
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten]     target_arch: x86_64
2016-12-06T17:24:06.269+0800 I CONTROL  [initandlisten] options: {}
2016-12-06T17:24:06.270+0800 I -        [initandlisten] Detected data files in /data/db created by the 'wiredTiger' storage engine, so setting the active storage engine to 'wiredTiger'.
2016-12-06T17:24:06.270+0800 I STORAGE  [initandlisten] wiredtiger_open config: create,cache_size=9G,session_max=20000,eviction=(threads_max=4),config_base=false,statistics=(fast),log=(enabled=true,archive=true,path=journal,compressor=snappy),file_manager=(close_idle_time=100000),checkpoint=(wait=60,log_size=2GB),statistics_log=(wait=0),
2016-12-06T17:24:07.639+0800 I CONTROL  [initandlisten] 
2016-12-06T17:24:07.639+0800 I CONTROL  [initandlisten] ** WARNING: soft rlimits too low. Number of files is 256, should be at least 1000
2016-12-06T17:24:07.701+0800 I NETWORK  [HostnameCanonicalizationWorker] Starting hostname canonicalization worker
2016-12-06T17:24:07.701+0800 I FTDC     [initandlisten] Initializing full-time diagnostic data capture with directory '/data/db/diagnostic.data'
2016-12-06T17:24:07.737+0800 I NETWORK  [initandlisten] waiting for connections on port 27017
```
---

[返回目录](#目录)


### 运行
#### 获取源代码
```sh
git clone https://github.com/cookeem/CookIM.git

cd CookIM
```
---

[返回目录](#目录)

#### 下载node依赖包

进入www目录，安装node的依赖包（npm有国内淘宝镜像，详情请百度）
```sh
$ cd www
$ npm install
```
---

[返回目录](#目录)

#### 开启mongoDB服务

```sh
$ mongod &
```
---

[返回目录](#目录)

#### 下载sbt的jar依赖包

返回CookIM目录，打开一个终端，运行如下命令，下载依赖包，该过程请耐心等待，原因你懂的（sbt有国内OSChina镜像，详情请百度）

```sh
$ cd ..
$ sbt console
```
---

[返回目录](#目录)

#### 使用预打包的libs运行程序

如果嫌sbt下载jar依赖包非常慢，我们已经预先准备好相关的jar依赖包，位于```libs```目录

---

[返回目录](#目录)

#### 启动CookIM服务

启动服务有两种方式，sbt方式以及java方式

a. 进入CookIM所在目录，使用sbt方式启动服务（如果你使用sbt下载了依赖）：
```sh
$ cd #CookIM directory#

$ sbt "run-main com.cookeem.chat.CookIM -w 8080 -a 2551"
```
b. 进入CookIM所在目录，也可以使用java方式启动服务（如果你没有使用sbt下载依赖，而是直接用```libs```目录的依赖包启动服务）：
```sh
$ cd #CookIM directory#

$ java -classpath "libs/*" com.cookeem.chat.CookIM -w 8080 -a 2551 
```

以上命令启动了一个监听8080端口的WEB服务，akka system的监听端口为2551

参数说明：

-h 8080 表示HTTP服务监听8080端口

-a 2551 表示akka集群的seed node监听2551端口，默认seed node为localhost:2551

---

[返回目录](#目录)

#### 打开浏览器，访问以下网址8080
> http://localhost:8080

---

[返回目录](#目录)

#### 启动另一个CookIM服务

打开另外一个终端，启动另一个CookIM服务，测试服务间的消息通讯功能。

a. 进入CookIM所在目录，使用sbt方式启动服务（如果你使用sbt下载了依赖）：
```sh
$ cd #CookIM directory#

$ sbt "run-main com.cookeem.chat.CookIM -w 8081 -a 2552"
```
b. 进入CookIM所在目录，也可以使用java方式启动服务（如果你没有使用sbt下载依赖，而是直接用```libs```目录的依赖包启动服务）：
```sh
$ cd #CookIM directory#

$ java -classpath "libs/*" com.cookeem.chat.CookIM -w 8081 -a 2552 
```

以上命令启动了一个监听8081端口的WEB服务，akka system的监听端口为2552

---

[返回目录](#目录)

#### 打开浏览器，访问以下网址8081
> http://localhost:8081

该演示启动了两个CookIM服务，访问地址分别为8080端口以及8081端口，用户通过两个浏览器分别访问不同的的CookIM服务，用户在浏览器中通过websocket发送消息到akka集群，akka集群通过分布式的消息订阅与发布，把消息推送到集群中相应的节点，实现消息在不同服务间的分布式通讯。

> 你也可以把服务部署在不同的服务器上，请修改```conf/application.conf```配置文件中seed-nodes的配置，把localhost改为主机名

---

[返回目录](#目录)

### 架构

#### 整体服务架构

![CookIM architecture](docs/CookIM-Flow.png)

**CookIM服务由三部分组成，基础原理如下：**

> 1. akka http：用于提供web服务，浏览器通过websocket连接akka http来访问分布式聊天应用；

> 2. akka stream：akka http在接收websocket发送的消息之后（消息包括文本消息：TextMessage以及二进制文件消息：BinaryMessage），把消息放到chatService流中进行流式处理。websocket消息中包含JWT（Javascript web token），如果JWT校验不通过，chatService流会直接返回reject消息；如果JWT校验通过，chatService流会把消息发送到ChatSessionActor中；

> 3. akka cluster：akka stream把用户消息发送到akka cluster，CookIM使用到akka cluster的DistributedPubSub，当用户进入会话的时候，订阅（Subscribe）对应的会话；当用户向会话发送消息的时候，会把消息发布（Publish）到订阅的actor中，此时，群聊中的用户就可以收到消息。

---

[返回目录](#目录)

#### akka stream websocket graph

![CookIM stream](docs/CookIM-ChatStream.png)

akka http在接收到websocket发送的消息之后，会把消息发送到chatService流里边进行处理，这里使用到akka stream graph：

> 1. websocket发送的消息体包含JWT，flowFromWS用户接收websocket消息，并把消息里边的JWT进行解码，验证有效性；

> 2. 对于JWT校验失败的消息，会经过filterFailure进行过滤；对于JWT校验成功的消息，会经过filterSuccess进行过滤；

> 3. builder.materializedValue为akka stream的物化值，在akka stream创建的时候，会自动向connectedWs发送消息，connectedWs把消息转换成UserOnline消息，通过chatSinkActor发送给ChatSessionActor；

> 4. chatActorSink在akka stream结束的时候，向down stream发送UserOffline消息；

> 5. chatSource用于接收从ChatSessionActor中回送的消息，并且把消息发送给flowAcceptBack；

> 6. flowAcceptBack提供keepAlive，保证连接不中断；

> 7. flowReject和flowAcceptBack的消息最后统一通过flowBackWs处理成websocket形式的Message通过websocket回送给用户；

---

[返回目录](#目录)

#### MongoDB数据库说明

 - users： 用户表
```
*login（登录邮箱）
nickname（昵称）
password（密码SHA1）
gender（性别：未知：0，男生：1，女生：2）
avatar（头像，绝对路径，/upload/avatar/201610/26/xxxx.JPG）
lastLogin（最后登录时间，timstamp）
loginCount(登录次数)
sessionsStatus（用户相关的会话状态列表）
    [{sessionid: 会话id, newCount: 未读的新消息数量}]
friends（用户的好友列表：[{uuid: 好友uuid}]）
dateline（注册时间，timstamp）
```

 - sessions： 会话表（记录所有群聊私聊的会话信息）
```
*createuid（创建者的uid）
*ouid（接收者的uid，只有当私聊的时候才有效）
sessionIcon（会话的icon，对于群聊有效）
sessionType（会话类型：0：私聊，1：群聊）
publicType（可见类型：0：不公开邀请才能加入，1：公开）
sessionName（群描述）
dateline（创建日期，timestamp）
usersStatus（会话对应的用户uuid数组）
    [{uid: 用户uuid, online: 是否在线（true：在线，false：离线}]
lastMsgid（最新发送的消息id）
lastUpdate（最后更新时间，timstamp）
dateline（创建时间，timstamp）
```
 - messages： 消息表（记录会话中的消息记录）
```
*uid（消息发送者的uid）
*sessionid（所在的会话id）
msgType（消息类型：）
content（消息内容）
fileInfo（文件内容）
    {
        filePath（文件路径）
        fileName（文件名）
        fileType（文件mimetype）
        fileSize（文件大小）
        fileThumb（缩略图）
    }
*dateline（创建日期，timestamp）
```

 - onlines：（在线用户表）
```
*id（唯一标识）
*uid（在线用户uid）
dateline（更新时间戳）
```

 - notifications：（接收通知表）
```
noticeType：通知类型（"joinFriend", "removeFriend", "inviteSession"）
senduid：操作方uid
*recvuid：接收方uid
sessionid：对应的sessionid
isRead：是否已读（0：未读，1：已读）
dateline（更新时间戳）
```

---

[返回目录](#目录)

#### 消息类型

有两个websocket信道：ws-push和ws-chat

> ws-push向用户下发消息提醒，当用户不在会话中，可以提醒用户有哪些会话有新消息

/ws-push channel
```
上行消息，用于订阅推送消息：
{ userToken: "xxx" }

下行消息：
acceptMsg:     { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "accept", content: "xxx", dateline: "xxx" }
rejectMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "", sessionIcon: "", msgType: "reject", content: "xxx", dateline: "xxx" }
keepAlive:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "", sessionIcon: "", msgType: "keepalive", content: "", dateline: "xxx" }
textMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "text", content: "xxx", dateline: "xxx" }
fileMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "file", filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx", dateline: "xxx" }
onlineMsg:     { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "online", content: "xxx", dateline: "xxx" }
offlineMsg:    { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "offline", content: "xxx", dateline: "xxx" }
joinSessionMsg: { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "join", content: "xxx", dateline: "xxx" }
leaveSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "leave", content: "xxx", dateline: "xxx" }
noticeMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "xxx", sessionIcon: "xxx", msgType: "system", content: "xxx", dateline: "xxx" }

下行到浏览器消息格式：
pushMsg:       { 
                    uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "xxx", 
                    content: "xxx", 
                    fileInfo: { filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx" },
                    dateline: "xxx" 
               }
```

---

[返回目录](#目录)

> ws-chat为用户在会话中的聊天信道，用户在会话中发送消息以及接收消息用

```
/ws-chat channel
上行消息：
onlineMsg:     { userToken: "xxx", sessionToken: "xxx", msgType:"online", content:"" }
textMsg:       { userToken: "xxx", sessionToken: "xxx", msgType:"text", content:"xxx" }
fileMsg:       { userToken: "xxx", sessionToken: "xxx", msgType:"file", fileName:"xxx", fileSize: 999, fileType: "xxx" }<#BinaryInfo#>binary_file_array_buffer

下行消息：    
rejectMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "", sessionIcon: "", msgType: "reject", content: "xxx", dateline: "xxx" }
keepAlive:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "", sessionIcon: "", msgType: "keepalive", content: "", dateline: "xxx" }
textMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "text", content: "xxx", dateline: "xxx" }
fileMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "file", filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx", dateline: "xxx" }
onlineMsg:     { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "online", content: "xxx", dateline: "xxx" }
offlineMsg:    { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "offline", content: "xxx", dateline: "xxx" }
joinSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "join", content: "xxx", dateline: "xxx" }
leaveSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "leave", content: "xxx", dateline: "xxx" }
noticeMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "xxx", sessionIcon: "xxx", msgType: "system", content: "xxx", dateline: "xxx" }

下行到浏览器消息格式：
chatMsg:       { 
                    uid: "xxx", nickname: "xxx", avatar: "xxx", msgType: "xxx", 
                    content: "xxx", 
                    fileInfo: { filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx" },
                    dateline: "xxx" 
               }
```    

---

[返回目录](#目录)

