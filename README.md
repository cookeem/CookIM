### 1. akka node port支持自动递增

---
### 2. 消息发送接收支持json格式

---
```
sendMessage { 
    nickname: string, 
    uid: int,
    uuid: string, 
    time: long, 
    msg: string
}

recvMessage { 
    nickname: string, 
    uid: int,
    uuid: string, 
    time: long, 
    size: long,
    msgid: string,
    msg: string
}

#发送二进制消息的时候附带的信息
sendBinary {
    nickname: string, 
    uid: int,
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
    uid: int,
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

---
### 8. 使用redis保存会话信息、用户信息

---
### 9. 支持用户注册和登录

---
### 10. 支持把群聊内容保存到mongodb

---
### 11. 文件支持保存到本地目录，并自动命名文件。并且能够根据客户端发送的文件md5信息与服务端文件md5信息进行比较，建立文件与消息id对应关系

---
### 12. 支持用户修改个人资料，包括头像信息

---
### 13. 修改前端界面，支持angularjs、jquery、semantic。

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
