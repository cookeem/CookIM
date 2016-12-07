# CookIM - 一个基于akka的分布式websocket聊天程序

![CookIM logo](docs/cookim.png)

- [中文文档](README_CN.md)
- [English document](README.md)

---

### 目录
1. [演示](#演示)
1. [功能](#功能)
1. [安装前准备](#安装前准备)
    1. [安装Java 8+](#安装Java 8+)
    1. [安装Scala 2.11+](#安装Scala 2.11+)
    1. [安装SBT 0.13+](#安装SBT 0.13+)
    1. [安装NodeJS 5+](#安装NodeJS 5+)
    1. [安装MongoDB 3+](#安装MongoDB 3+)
1. [运行](#运行)
    1. [获取源代码](#获取源代码)
    1. [下载node依赖包](#下载node依赖包)
    1. [开启mongoDB服务](#开启mongoDB服务)
    1. [下载sbt的jar依赖包](#下载sbt的jar依赖包)
    1. [使用预打包的libs运行程序](#使用预打包的libs运行程序)
    1. [启动CookIM服务](#启动CookIM服务)
    1. [打开浏览器，访问以下网址8080](#打开浏览器，访问以下网址8080)
    1. [启动另一个CookIM服务](#启动另一个CookIM服务)
    1. [打开浏览器，访问以下网址8081](#打开浏览器，访问以下网址8081)
1. [架构](#架构)
    1. [整体服务架构](#整体服务架构)
    1. [akka stream websocket流计算graph](#akka stream websocket流计算graph)
    1. [分布式订阅与发布时序](#分布式订阅与发布时序)
    1. [MongoDB数据库说明](#MongoDB数据库说明)
    1. [消息类型](#消息类型)

---

###演示

**PC**

![screen snapshot](docs/screen.png) 

**手机**

![screen snapshot](docs/screen2.png)


- **演示地址:** [https://im.cookeem.com](https://im.cookeem.com)


---

### 功能

---


### 安装前准备

---
- #### 安装Java 8+

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

- #### 安装Scala 2.11+

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

- #### 安装SBT 0.13+

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

- #### 安装NodeJS 5+

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

- #### 安装MongoDB 3+

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


### 运行
- #### 获取源代码
```sh
git clone https://github.com/cookeem/CookIM.git

cd CookIM
```
---

- #### 下载node依赖包

进入www目录，安装node的依赖包（npm有国内淘宝镜像，详情请百度）
```sh
$ cd www
$ npm install
```
---

- #### 开启mongoDB服务

```sh
$ mongod &
```
---

- #### 下载sbt的jar依赖包

返回CookIM目录，打开一个终端，运行如下命令，下载依赖包，该过程请耐心等待，原因你懂的（sbt有国内OSChina镜像，详情请百度）

```sh
$ cd ..
$ sbt console
```
---

- #### 使用预打包的libs运行程序

如果嫌sbt下载jar依赖包非常慢，我们已经预先准备好相关的jar依赖包，位于```libs```目录

---

- #### 启动CookIM服务

启动服务有两种方式，sbt方式以及java方式

a. 进入CookIM所在目录，使用sbt方式启动服务（如果你使用sbt下载了依赖）：
```sh
$ cd #CookIM directory#

$ sbt "run-main com.cookeem.chat.CookIM -h 8080 -n 2551"
```
b. 进入CookIM所在目录，也可以使用java方式启动服务（如果你没有使用sbt下载依赖，而是直接用```libs```目录的依赖包启动服务）：
```sh
$ cd #CookIM directory#

$ java -classpath "libs/*" com.cookeem.chat.CookIM -h 8080 -n 2551 
```

以上命令启动了一个监听8080端口的WEB服务，akka system的监听端口为2551

参数说明：

-h 8080 表示HTTP服务监听8080端口

-n 2551 表示akka集群的seed node监听2551端口，默认seed node为localhost:2551

---

- #### 打开浏览器，访问以下网址8080
> http://localhost:8080

---

- #### 启动另一个CookIM服务

打开另外一个终端，启动另一个CookIM服务，测试服务间的消息通讯功能。

a. 进入CookIM所在目录，使用sbt方式启动服务（如果你使用sbt下载了依赖）：
```sh
$ cd #CookIM directory#

$ sbt "run-main com.cookeem.chat.CookIM -h 8081 -n 2552"
```
b. 进入CookIM所在目录，也可以使用java方式启动服务（如果你没有使用sbt下载依赖，而是直接用```libs```目录的依赖包启动服务）：
```sh
$ cd #CookIM directory#

$ java -classpath "libs/*" com.cookeem.chat.CookIM -h 8081 -n 2552 
```

以上命令启动了一个监听8081端口的WEB服务，akka system的监听端口为2552

---

- #### 打开浏览器，访问以下网址8081
> http://localhost:8081

该演示启动了两个CookIM服务，访问地址分别为8080端口以及8081端口，用户通过两个浏览器分别访问不同的的CookIM服务，用户在浏览器中通过websocket发送消息到akka集群，akka集群通过分布式的消息订阅与发布，把消息推送到集群中相应的节点，实现消息在不同服务间的分布式通讯。

> 你也可以把服务部署在不同的服务器上，请修改```conf/application.conf```配置文件中seed-nodes的配置，把localhost改为主机名

---

### 架构

![CookIM architecture](docs/CookIM-Flow.png)

![CookIM stream](docs/CookIM-ChatStream.png)
