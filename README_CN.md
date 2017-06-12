# CookIM - 一个基于akka的分布式websocket聊天程序

![CookIM logo](docs/cookim.png)

- [中文文档](README_CN.md)
- [English document](README.md)

---

- [GitHub项目地址](https://github.com/cookeem/CookIM/)
- [OSChina项目地址](https://git.oschina.net/cookeem/CookIM/)

---

### 目录

1. [演示](#演示)
    1. [PC演示](#PC演示)
    1. [手机演示](#手机演示)
    1. [演示地址](#演示地址)
1. [以Docker-Compose方式启动CookIM集群](#以docker-compose方式启动cookim集群)
    1. [启动集群](#启动集群)
    1. [增加节点](#增加节点)
    1. [调试容器](#调试容器)
    1. [停止集群](#停止集群)
1. [运行](#运行)
    1. [本地运行需求](#本地运行需求)
    1. [获取源代码](#获取源代码)
    1. [配置与打包](#配置与打包)
    1. [启动CookIM服务](#启动cookim服务)
    1. [打开浏览器，访问以下网址8080](#打开浏览器访问以下网址8080)
    1. [启动另一个CookIM服务](#启动另一个cookim服务)
    1. [打开浏览器，访问以下网址8081](#打开浏览器访问以下网址8081)
1. [架构](#架构)
    1. [整体服务架构](#整体服务架构)
    1. [akka stream websocket graph](#akka-stream-websocket-graph)
    1. [MongoDB数据库说明](#mongodb数据库说明)
    1. [消息类型](#消息类型)
1. [ChangeLog](#ChangeLog)
    1. [0.1.0-SNAPSHOT](#010-snapshot)
    1. [0.2.0-SNAPSHOT](#020-snapshot)
    

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

### 以Docker-Compose方式启动CookIM集群

#### 启动集群

进入CookIM所在目录，运行以下命令，以docker-compose方式启动CookIM集群，该集群启动了三个容器：mongo、cookim1、cookim2
```sh
$ git clone https://github.com/cookeem/CookIM.git

$ cd CookIM

$ sudo docker-compose up -d
Creating mongo
Creating cookim1
Creating cookim2
```

成功启动集群后，浏览器分别访问以下网址，对应不同的CookIM服务
> http://localhost:8080
> http://localhost:8081

---

[返回目录](#目录)

#### 增加节点

可以通过修改docker-compose.yml文件增加CookIM服务节点，例如增加第三个节点cookim3：

```yaml
      cookim3:
        image: cookeem/cookim
        container_name: cookim3
        hostname: cookim3
        environment:
          HOST_NAME: cookim3
          WEB_PORT: 8080
          AKKA_PORT: 2551
          SEED_NODES: cookim1:2551
        ports:
        - "8082:8080"
        depends_on:
        - mongo
        - cookim1
```
---

[返回目录](#目录)

#### 调试容器

查看cookim1容器日志输出
```sh
$ sudo docker logs -f cookim1
```

进入cookim1容器进行调试
```sh
$ sudo docker exec -ti cookim1 bash
```
---

[返回目录](#目录)

#### 停止集群
```sh
$ sudo docker-compose stop
$ sudo docker-compose rm -f
```
---

[返回目录](#目录)

### 运行

#### 本地运行需求

* JDK 8+
* Scala 2.11+
* SBT 0.13.15
* MongoDB 2.6 - 3.4

---

[返回目录](#目录)

#### 获取源代码
```sh
git clone https://github.com/cookeem/CookIM.git

cd CookIM
```
---

[返回目录](#目录)

#### 配置与打包

配置文件位于```conf/application.conf```，请务必配置mongodb的uri配置
```sh
mongodb {
  dbname = "cookim"
  uri = "mongodb://mongo:27017/local"
}
```

对CookIM进行打包fatjar，打包后文件位于```target/scala-2.11/CookIM-assembly-0.2.0-SNAPSHOT.jar```
```sh
sbt clean assembly
```

---

[返回目录](#目录)

#### 启动CookIM服务

CookIM的数据保存在MongoDB中，启动CookIM前务必先启动MongoDB

a. 调试方式启动服务：
```sh
$ sbt "run-main com.cookeem.chat.CookIM -h localhost -w 8080 -a 2551 -s localhost:2551"
```

b. 产品方式启动服务：
```sh
$ java -classpath "target/scala-2.11/CookIM-assembly-0.2.0-SNAPSHOT.jar" com.cookeem.chat.CookIM -h localhost -w 8080 -a 2551 -s localhost:2551
```

以上命令启动了一个监听8080端口的WEB服务，akka system的监听端口为2551

参数说明：

-a,--akka-port <AKKA-PORT>： akka system 监听端口2551

-h,--host-name <HOST-NAME>： 外部访问本机的主机名

-n,--nat： 是否使用NAT转换，docker模式下必须设置（可选）

-s,--seed-nodes <SEED-NODES>：表示akka集群的seed node监听2551端口，默认seed node为localhost:2551

---

[返回目录](#目录)

#### 打开浏览器，访问以下网址8080
> http://localhost:8080

---

[返回目录](#目录)

#### 启动另一个CookIM服务

打开另外一个终端，启动另一个CookIM服务，测试服务间的消息通讯功能。

a. 调试方式启动服务：
```sh
$ sbt "run-main com.cookeem.chat.CookIM -h localhost -w 8081 -a 2552 -s localhost:2551"
```

b. 产品方式启动服务：
```sh
$ java -classpath "target/scala-2.11/CookIM-assembly-0.2.0-SNAPSHOT.jar" com.cookeem.chat.CookIM -h localhost -w 8081 -a 2552 -s localhost:2551
```

以上命令启动了一个监听8081端口的WEB服务，akka system的监听端口为2552

---

[返回目录](#目录)

#### 打开浏览器，访问以下网址8081
> http://localhost:8081

该演示启动了两个CookIM服务，访问地址分别为8080端口以及8081端口，用户通过两个浏览器分别访问不同的的CookIM服务，用户在浏览器中通过websocket发送消息到akka集群，akka集群通过分布式的消息订阅与发布，把消息推送到集群中相应的节点，实现消息在不同服务间的分布式通讯。

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

 - akka http在接收到websocket发送的消息之后，会把消息发送到chatService流里边进行处理，这里使用到akka stream graph：

> 1. websocket发送的消息体包含JWT，flowFromWS用于接收websocket消息，并把消息里边的JWT进行解码，验证有效性；

> 2. 对于JWT校验失败的消息，会经过filterFailure进行过滤；对于JWT校验成功的消息，会经过filterSuccess进行过滤；

> 3. builder.materializedValue为akka stream的物化值，在akka stream创建的时候，会自动向connectedWs发送消息，connectedWs把消息转换成UserOnline消息，通过chatSinkActor发送给ChatSessionActor；

> 4. chatActorSink向chatSessionActor发送消息，在akka stream结束的时候，向down stream发送UserOffline消息；

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
friends（用户的好友列表：[好友uuid]）
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
fileMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "file", fileName: "xxx", fileType: "xxx", fileid: "xxx", thumbid: "xxx", dateline: "xxx" }
onlineMsg:     { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "online", content: "xxx", dateline: "xxx" }
offlineMsg:    { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "offline", content: "xxx", dateline: "xxx" }
joinSessionMsg: { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "join", content: "xxx", dateline: "xxx" }
leaveSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "leave", content: "xxx", dateline: "xxx" }
noticeMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "xxx", sessionIcon: "xxx", msgType: "system", content: "xxx", dateline: "xxx" }

下行到浏览器消息格式：
pushMsg:       { 
                    uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "xxx", 
                    content: "xxx", fileName: "xxx", fileType: "xxx", fileid: "xxx", thumbid: "xxx",
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
fileMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "file", fileName: "xxx", fileType: "xxx", fileid: "xxx", thumbid: "xxx", dateline: "xxx" }
onlineMsg:     { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "online", content: "xxx", dateline: "xxx" }
offlineMsg:    { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "offline", content: "xxx", dateline: "xxx" }
joinSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "join", content: "xxx", dateline: "xxx" }
leaveSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "leave", content: "xxx", dateline: "xxx" }
noticeMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "xxx", sessionIcon: "xxx", msgType: "system", content: "xxx", dateline: "xxx" }

下行到浏览器消息格式：
chatMsg:       { 
                    uid: "xxx", nickname: "xxx", avatar: "xxx", msgType: "xxx", 
                    content: "xxx", fileName: "xxx", fileType: "xxx", fileid: "xxx", thumbid: "xxx",
                    dateline: "xxx" 
               }
```    

---

[返回目录](#目录)

### ChangeLog
#### 0.1.0-SNAPSHOT

---

[返回目录](#目录)

#### 0.2.0-SNAPSHOT

* CookIM支持MongoDB 3.4.4
* 更新akka版本为2.5.2
* 更新容器启动方式，只保留docker-compose方式启动集群

---

[返回目录](#目录)
