### CookIM 是一个分布式的websocket聊天程序


分别打开不同终端，运行以下命令：

```
sbt "run-main com.cookeem.chat.CookIM -h 8080 -n 2551"

sbt "run-main com.cookeem.chat.CookIM -h 8081 -n 2552"
```

浏览器访问：

```
http://localhost:8080/chat/websocket.html?username=cookeem&chatid=room01

http://localhost:8081/chat/websocket.html?username=faith&chatid=room01
```
---
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
*login（登录邮箱）
nickname（昵称）
password（密码SHA1）
gender（性别：未知：0，男生：1，女生：2）
avatar（头像，绝对路径，/upload/avatar/201610/26/xxxx.JPG）
lastlogin（最后登录时间，timstamp）
logincount(登录次数)
sessionsstatus（用户相关的会话状态列表：[{sessionid: 会话id, newcount: 未读的新消息数量}]）
friends（用户的好友列表：[{uuid: 好友uuid}]）
dateline（注册时间，timstamp）
```
sessions： 会话表（记录所有群聊私聊的会话信息）
===
```
*senduid（创建者的uid）
*recvuid（接收者的uid，只有当私聊的时候才有效）
sessiontype（会话类型：0：私聊，1：群聊）
visabletype（可见类型：0：不可见，1：公开可见）
jointype（加入类型：0：所有人可以加入，1：群里用户邀请才能加入）
name（群描述）
dateline（创建日期，timestamp）
usersstatus（会话对应的用户uuid数组：[{uid: 用户uuid, online: 是否在线（true：在线，false：离线）}]）
lastmsgid（最新发送的消息id）
dateline（新建时间）
```
messages： 消息表（记录会话中的消息记录）
===
```
*senduid（消息发送者的uuid）
*sessionid（所在的会话id）
msgtype（消息类型：0：文字消息，1：图片消息，2：语音消息，3：视频消息，4：文件消息，5：语音聊天，6：视频聊天）
content（消息内容）
fileinfo（文件内容）
{
    filepath（文件路径）
    filename（文件名）
    filetype（文件mimetype）
    filemd5（文件的md5）
    size（文件大小）
    dateline（创建日期，timestamp）
}
dateline（创建日期，timestamp）
```

online（在线用户表）
===
```
*id（唯一标识）
*uid（在线用户uid）
time（更新时间戳）
```
### 11. 文件支持保存到本地目录，并自动命名文件。并且能够根据客户端发送的文件md5信息与服务端文件md5信息进行比较，建立文件与消息id对应关系

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

### 23. 支持用户不在线的时候消息缓存到队列，用户上线的时候，把队列中的消息发送给用户

### 24. 支持查看历史消息


### jwt验证流程

jwt用于保存服务端返回给用户的资源信息，这些资源通过明文传输，但是传输过程不可以篡改。
在jwt里边保存过期日期即可

jwt应该放在request的header中

浏览器（输入login -> 提交username，password）
服务器（验证username，password -> 输出jwt(uid)）
浏览器（获取jwt(uid)并保存到程序中，请求uid对应的会话列表界面 -> 提交jwt(uid)）
服务器（验证jwt(uid)是否有效 -> 输出jwt(uid)以及session列表信息）
浏览器（显示会话列表页面，点击某个会话 -> 提交jwt(uid)以及sessionid信息）
服务器（验证jwt(uid)是否有效 -> 输出jwt(uid, sessionid)以及session中的消息）
浏览器（展示会话消息查看页面，通过websocket通道提交发送消息 -> 提交jwt(uid, sessionid)以及消息内容）
服务器（通过websocket通道，验证jwt(uid, sessionid)是否有效，如果有效，表示uid有在sessionid中发消息的权限 -> 通过websocket通道发送消息）


# mongodb读写操作
用户注册    registerUser
用户登录    loginAction
用户注销    logoutAction
用户修改密码  changePwd
显示个人资料  getUserInfo
修改个人资料  updateUserInfo
查看公开会话列表（群聊）    listPublicSessions
查看加入的会话（查看全部列表、查看私聊列表、查看群聊列表）   listJoinedSessions
加入群聊会话  joinSession
创建群聊会话  createGroupSession
修改群聊信息  updateSessionInfo
离开群聊会话  leaveSession
查看历史消息（分页排序）    listHistoryMessages
创建私聊会话  createPrivateSession
查看群聊私聊资料（显示参与者列表） getSessionInfo


# websocket存在三个channel：
UserTokenChannel：用于从服务端推送UserToken到客户端，UserToken包含如下信息：uid、nickname、avatar，在keepalive中发送UserToken给客户端
SessionTokenChannel：当用户打开某个会话页面的时候，从服务端推送SessionToken到客户端，SessionToken包含如下信息：sessionid，表明用户有权在这个session中发送消息，在keepalive中发送SessionToken
MessageChannel：用于接收用户消息，以及向用户发送消息。当用户向服务端发送消息的时候，必须提供UserToken以及SessionToken，当这两个token验证都通过的情况下，用户可以发送消息，否则拒绝用户发送消息，并回送错误消息给用户。

# MessageChannel消息
## browser -> cluster（上行）
## （消息中必须包含UserToken以及SessionToken，用于验证权限，如果鉴权失败，则下发Notice(reject)）
用户进入会话（UserOnline）
---
先验证userToken以及sessionToken，如果不通过，则让actorRef关闭。
如果通过，则设置对应的sessions.usersstatus以及users.sessionsstatus，并把Notice(online)发送到各个节点,

用户离开会话（UserOffline）
---
设置对应的sessions.usersstatus以及users.sessionsstatus

用户发送文本消息（UserText）
---
用户发送文件消息（UserBinary），把文件信息放到UserBinary中
---
{
    "userToken": "",
    "sessionToken": "",
    "fileInfo": {
        "fileName": "",
        "fileSize": 0,
        "fileType": ""
    }
}<#fileInfo#>Binary
## cluster -> browser（下行）
向用户下发的文字消息（ChatText），保存对应的messages，更新users.sessionsstatus
{
    "uid": "",
    "nickname": "",
    "avatar": "",
    "msgType": "text",
    "time": "",
    "message": ""
}
集群间发布的消息（ClusterText），用于集群间的订阅发布
{
    "uid": "",
    "nickname": "",
    "avatar": "",
    "msgType": "text",
    "time": "",
    "message": ""
}
向用户下发的文件消息（ChatBinaryInfo），保存对应的messages，更新users.sessionsstatus
{
    "uid": "",
    "nickname": "",
    "avatar": "",
    "msgType": "binary",
    "time": "",
    "fileInfo": {
        "filePath": "",
        "fileName": "",
        "fileSize": 0,
        "fileType": ""
    }
}
集群间发布的消息（ClusterBinaryInfo），用于集群间的订阅发布
{
    "uid": "",
    "nickname": "",
    "avatar": "",
    "msgType": "binary",
    "time": "",
    "fileInfo": {
        "filePath": "",
        "fileName": "",
        "fileSize": 0,
        "fileType": ""
    }
}
由系统下发消息（ChatNotice）
{
    "uid": "",
    "nickname": "system",
    "avatar": "",
    "msgType": "notice",
    "noticeType": "reject",  //reject, online, offline, join, leave, keepAlive
    "time": "",
    "message": ""
}
集群间发布的系统消息（ClusterNotice），用于集群间的订阅发布
{
    "uid": "",
    "nickname": "system",
    "avatar": "",
    "msgType": "notice",
    "noticeType": "reject",  //reject, online, offline, join, leave, keepAlive
    "time": "",
    "message": ""
}

## cluster -> browser（下行keepAlive消息）
系统下发的KeepAlive消息（KeepAliveUser），keepAlive包含UserToken（uid，nickname，avatar）
{
    "userToken": ""
}
系统下发的KeepAlive消息（KeepAliveSession），keepAlive包含SessionToken（sessionid）
{
    "sessionToken": ""
}


users表需要记录用户参与的session以及参与的session的最新消息
sessions表需要记录会话的参与者的当前是否在线状态

在会话中发送消息，消息要批量入库到mongodb
会话中发送消息的时候，浏览器通过ws发送jwt以及消息体给服务端，服务端要验证jwt（jwt中包含如下信息：uid,nickname,avatar），如果jwt不正确要回送错误提示而不进行群发，修改ChatSessionActor中的逻辑
在websocket的keepalive上增加更新刷新online动作，更新online并回送jwt
用户上线的时候读取session的历史消息即可
用户发送图片的时候，使用TextMessage下发消息回浏览器


# 用户最多当前在一个session中在线
