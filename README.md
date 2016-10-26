### CookIM 是一个分布式的websocket聊天程序


分别打开不同终端，运行以下命令：

```
sbt "run-main com.cookeem.chat.CookIM -h 8080 -n 2551"

sbt "run-main com.cookeem.chat.CookIM -h 8081 -n 2552"
```

浏览器访问：

```
http://localhost:8080/websocket.html?username=cookeem&chatid=room01

http://localhost:8081/websocket.html?username=faith&chatid=room01
```
### 1. akka node port支持自动递增

---
### 2. 消息发送接收支持json格式

---
```
sendMessage { 
    nickname: string, 
    uuid: string, 
    time: long, 
    msg: string
}

recvMessage { 
    nickname: string, 
    uuid: string, 
    time: long, 
    size: long,
    msgid: string,
    msg: string
}

#发送二进制消息的时候附带的信息
sendBinary {
    nickname: string, 
    uuid: string, 
    filename: string,
    filetype: string,
    filemd5: string,
    size: long,
    time: long, 
    msg: string
}

recvBinary { 
    nickname: string, 
    uuid: string, 
    filename: string,
    filetype: string,
    filemd5: string,
    size: long,
    time: long, 
    msgid: string,
    msg: string
}

```
### 3. 客户端支持显示用户名，发送时间

---
### 4. 支持消息保存到mongodb

---
### 5. 支持用户进入聊天室的时候显示之前的消息

---
### 6. 显示二进制内容机制修改，改为推送json信息

---
### 7. 用户信息保存在mongodb

users： 用户表
===
```
*uuid（用户的唯一标识uuid）
*login（登录邮箱）
nickname（昵称）
password（密码SHA1）
dateline（注册时间，timstamp）
lastlogin（最后登录时间，timstamp）
logincount(登录次数)
gender（性别：未知：0，男生：1，女生：2）
avatar（头像，绝对路径，/upload/avatar/201610/26/xxxx.JPG）
sessions（用户相关的会话列表：[{sessionid: 会话id}]）
friends（用户的好友列表：[{uuid: 好友uuid}]）
```
sessions： 会话表（记录所有群聊私聊的会话信息）
===
```
*sessionid（会话的唯一标识uuid）
*creatoruuid（创建者的uuid）
sessiontype（会话类型：0：私聊，1：群聊）
visabletype（可见类型：0：不可见，1：公开可见）
jointype（加入类型：0：所有人可以加入，1：群里用户邀请才能加入）
desc（群描述）
dateline（创建日期，timestamp）
users（会话对应的用户uuid数组：[{uuid: 用户uuid}]）
\# messages（消息的id数组：[{msgid: 消息的id}]）
```
messages： 消息表（记录会话中的消息记录）
===
```
*msgid（消息的id）
*uuid（消息发送者的uuid）
*sessionid（所在的会话id）
msgtype（消息类型：0：文字消息，1：图片消息，2：语音消息，3：视频消息，4：文件消息，5：语音聊天，6：视频聊天）
dateline（创建日期，timestamp）
content（消息内容）
fileinfo（文件内容）
{
    *fileid（文件id）
    filepath（文件路径）
    filename（文件名）
    filetype（文件mimetype）
    filemd5（文件的md5）
    size（文件大小）
    dateline（创建日期，timestamp）
}
```
### 8. 使用redis保存会话信息、用户信息

cookim:onlineusers（在线用户表，set类型）
===
```
*uuid（用户的唯一标识uuid）
```
cookim:users:uuid:xxxxx:info（用户表，hash类型）
===
```
nickname（昵称）
gender（性别：未知：0，男生：1，女生：2）
avatar（头像，绝对路径，/upload/avatar/201610/26/xxxx.JPG）
```
cookim:sessions:sessionid:xxxxx:msgid（会话消息表，list类型）
===
```
msgid（消息的id）
```
---
### 9. 支持用户注册和登录

---
### 10. 支持把群聊内容保存到mongodb

---
### 11. 文件支持保存到本地目录，并自动命名文件。并且能够根据客户端发送的文件md5信息与服务端文件md5信息进行比较，建立文件与消息id对应关系

---
### 12. 支持用户修改个人资料，包括头像信息

---
### 13. 修改前端界面，支持angularjs、jquery、materializeCss。

### 14. 用户登录界面

### 15. 用户注册界面

---
昵称
登录邮箱
密码
性别
头像（支持选择和上传）

### 16. 用户修改资料界面

---
昵称
性别
头像（支持选择和上传）

### 17. 用户修改密码界面

---
修改密码，需要输入原密码

### 18. 聊天列表界面

---
支持私聊以及群聊

### 19. 聊天界面、支持瀑布式以及私聊左右气泡式

---
群聊为瀑布式、私聊为左右气泡式

### 20. 支持显示状态： 在线、隐身、离开、忙碌

---
在线、离开、忙碌表示用户的头像状态，可以针对不同的聊天会话设置显示状态
隐身状态为不显示在群聊列表中

### 21. 支持表情

---
支持表情emoji（参见twitter的emoji库）
支持已经梳理好的表情
服务端支持直接解释表情文本为表情

### 22. 支持视频直播
