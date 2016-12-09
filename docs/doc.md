分别打开不同终端，运行以下命令：

```
sbt "run-main com.cookeem.chat.CookIM -w 8080 -a 2551"

sbt "run-main com.cookeem.chat.CookIM -w 8081 -a 2552"
```

浏览器访问：

```
http://localhost:8080/

http://localhost:8081/
```
---

users： 用户表
===
```
*login（登录邮箱）
nickname（昵称）
password（密码SHA1）
gender（性别：未知：0，男生：1，女生：2）
avatar（头像，绝对路径，/upload/avatar/201610/26/xxxx.JPG）
lastLogin（最后登录时间，timstamp）
loginCount(登录次数)
sessionsStatus（用户相关的会话状态列表：[{sessionid: 会话id, newCount: 未读的新消息数量}]）
friends（用户的好友列表：[{uuid: 好友uuid}]）
dateline（注册时间，timstamp）
```
sessions： 会话表（记录所有群聊私聊的会话信息）
===
```
*createuid（创建者的uid）
*ouid（接收者的uid，只有当私聊的时候才有效）
sessionIcon（会话的icon，对于群聊有效）
sessionType（会话类型：0：私聊，1：群聊）
publicType（可见类型：0：不公开邀请才能加入，1：公开）
sessionName（群描述）
dateline（创建日期，timestamp）
usersStatus（会话对应的用户uuid数组：[{uid: 用户uuid, online: 是否在线（true：在线，false：离线）}]）
lastMsgid（最新发送的消息id）
lastUpdate（最后更新时间，timstamp）
dateline（创建时间，timstamp）
```
messages： 消息表（记录会话中的消息记录）
===
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

onlines：（在线用户表）
===
```
*id（唯一标识）
*uid（在线用户uid）
dateline（更新时间戳）
```

notifications：（接收通知表）
===
```
noticeType：通知类型（"joinFriend", "removeFriend", "inviteSession"）
senduid：操作方uid
*recvuid：接收方uid
sessionid：对应的sessionid
isRead：是否已读（0：未读，1：已读）
dateline（更新时间戳）
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
[OK] 用户注册    registerUser
[OK] 用户登录    loginAction
[OK] 用户注销    logoutAction
[OK] 用户修改密码  changePwd
显示个人资料  getUserInfo
[OK] 修改个人资料  updateUserInfo
[OK] 查看会话列表    listSessions
加入群聊会话  joinSession
[OK] 创建群聊会话  createGroupSession
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
```
/ws-push channel
上行：
{ userToken: "xxx" }
下行：
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
下行用户端：
pushMsg:       { 
                    uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "xxx", 
                    content: "xxx", 
                    fileInfo: { filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx" },
                    dateline: "xxx" 
               }
```
---
```
/ws-chat channel
上行：
onlineMsg:     { userToken: "xxx", sessionToken: "xxx", msgType:"online", content:"" }
textMsg:       { userToken: "xxx", sessionToken: "xxx", msgType:"text", content:"xxx" }
fileMsg:       { userToken: "xxx", sessionToken: "xxx", msgType:"file", fileName:"xxx", fileSize: 999, fileType: "xxx" }<#BinaryInfo#>binary_file_array_buffer
下行：    
rejectMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "", sessionIcon: "", msgType: "reject", content: "xxx", dateline: "xxx" }
keepAlive:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "", sessionIcon: "", msgType: "keepalive", content: "", dateline: "xxx" }
textMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "text", content: "xxx", dateline: "xxx" }
fileMsg:       { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "file", filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx", dateline: "xxx" }
onlineMsg:     { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "online", content: "xxx", dateline: "xxx" }
offlineMsg:    { uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "offline", content: "xxx", dateline: "xxx" }
joinSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "join", content: "xxx", dateline: "xxx" }
leaveSessionMsg:{ uid: "xxx", nickname: "xxx", avatar: "xxx", sessionid: "xxx", sessionName: "xxx", sessionIcon: "xxx", msgType: "leave", content: "xxx", dateline: "xxx" }
noticeMsg:     { uid: "", nickname: "", avatar: "", sessionid: "", sessionName: "xxx", sessionIcon: "xxx", msgType: "system", content: "xxx", dateline: "xxx" }
下行用户端：
chatMsg:       { 
                    uid: "xxx", nickname: "xxx", avatar: "xxx", msgType: "xxx", 
                    content: "xxx", 
                    fileInfo: { filePath: "xxx", fileName: "xxx", fileSize: 999, fileType: "xxx", fileThumb: "xxx" },
                    dateline: "xxx" 
               }
```    
---


经常改变的字段：
users.sessionsStatus（用户相关的会话状态列表：[{sessionid: 会话id, newCount: 未读的新消息数量}]）
[OK] users.sessionsStatus.sessionid在joinSession的时候增加记录，在leaveSession的时候删除记录
[OK] users.sessionsStatus.newCount，当用户不在线，createMessage的时候+1，listHistoryMessages的时候设置为0

users.lastLogin（最后登录时间，timstamp）
[OK] 在createUserToken的时候更新

users.loginCount(登录次数)
[OK] 在loginAction的时候+1

sessions.usersStatus（会话对应的用户uuid数组：[{uid: 用户uuid, online: 是否在线（true：在线，false：离线）}]）
[OK] sessions.usersStatus.uid在joinSession的时候增加记录，在leaveSession的时候删除记录
[OK] sessions.usersStatus.online在用户进入会话的时候userOnlineOffline设置为true，在用户离开会话的时候设置为false
sessions.lastMsgid（最新发送的消息id）
[OK] 在createMessage的时候更新对应的lastmsgid

onlines.uid（在线用户uid）
onlines.dateline（更新时间戳）
[OK] 在createUserToken的时候更新

messages
[OK] 在用户发送消息的时候更新(createMessage)


# 改进需求
1、会话列表页（公开的）（群聊）可以（joinSession）；
2、会话列表页（加入的）（群聊）可以（leaveSession）；
3、会话列表页（加入的）（私聊）可以（leaveSession），leaveSession会把双方对应的users.sessionsstatus清除；
4、会话列表页（群聊），可以查看会话中的用户，哪些在线，哪些不在线；
5、查看会话中的用户列表界面，可以向某个用户发起会话（不能向自己发起会话），自动创建会话；
6、会话列表页，接口没有显示会话中最后发送的消息 ———— 对于文件、图片消息需要进行翻译；
7、顶部标题栏根据所在页面显示不同标题以及菜单

---

1、消息查看页（群聊），可以查看会话中的用户，哪些在线，哪些不在线；
2、消息查看页（群聊），可以修改群聊资料；
3、消息查看页（群聊），可以（leaveSession）、可以（inviteSession）邀请好友加入会话；
4、消息查看页（私聊），可以加好友或者删除好友；
5、消息查看页（私聊），可以邀请好友加入会话，自动创建新的会话；
6、消息查看页，对于图片消息可以查看图片大图；对于文件消息可以下载文件；
7、消息查看页，可以向某个用户发起会话（不能向自己发起会话），自动创建会话；
8、消息查看页，可以申请加某个用户为好友（不能加自己为好友）；

---

1、左侧菜单显示已加入的群聊名称以及新消息数量；
2、主界面可以显示pushMessage的toast通知；
3、主界面右上角菜单可以关闭或者开启pushMessage的toast通知；
4、新建通知页面，以及通知表。通知表用户显示加好友通知，邀请加入会话通知。
——通知类型两类：加好友通知、邀请加入会话通知
5、新建好友列表页面，列表上可以删除好友；
