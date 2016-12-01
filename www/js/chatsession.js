/**
 * Created by cookeem on 16/6/3.
 */
app.controller('chatSessionAppCtl', function($rootScope, $scope, $cookies, $timeout, $routeParams, $http, $interval) {
    $rootScope.showSideNavbar = true;
    $rootScope.showMessageArea = true;
    $rootScope.showAccoutMenu = true;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - My chat!",
        //title icon
        icon: "images/favicon.ico",
        //useful when mode == "group_session"
        sessionid: "",
        //useful when mode == "private_session"
        uid: ""
    };

    $timeout(function() {
        showHideSideBar($rootScope.showSideNavbar);
        $(window).resize(function() {
            showHideSideBar($rootScope.showSideNavbar);
        });
    }, 0);

    $rootScope.getUserTokenRepeat();

    $scope.sessionid = $routeParams.querystring;

    $scope.listMessagesData = {
        "sessionid": $scope.sessionid,
        "page": 1,
        "count": 50,
        "userToken": $rootScope.userToken
    };
    $scope.messages = [];
    $scope.sessionToken = "";
    $scope.listMessagesSubmit = function() {
        $rootScope.verifyUserToken();
        $http({
            method  : 'POST',
            url     : '/api/listMessages',
            data    : $.param($scope.listMessagesData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast($rootScope.errmsg, 4000);
            } else {
                $scope.messages = response.data.messages;
                $scope.sessionToken = response.data.sessionToken;
                $scope.getSessionHeader();
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $scope.getSessionHeader = function() {
        $rootScope.verifyUserToken();
        $http({
            method  : 'POST',
            url     : '/api/getSessionHeader',
            data    : $.param($scope.listMessagesData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast($rootScope.errmsg, 4000);
            } else {
                if (response.data.session.ouid == "") {
                    $rootScope.titleInfo.mode = "group_session";
                } else {
                    $rootScope.titleInfo.mode = "private_session";
                }
                $rootScope.titleInfo.title = response.data.session.sessionName;
                $rootScope.titleInfo.icon = response.data.session.sessionIcon;
                $rootScope.titleInfo.sessionid = response.data.session.sessionid;
                $rootScope.titleInfo.uid = response.data.session.ouid;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $scope.listMessagesSubmit();


    //get sessionToken request
    $scope.getSessionTokenSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "sessionid": $scope.sessionid
        };
        $http({
            method  : 'POST',
            url     : '/api/sessionToken',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $scope.sessionToken = response.data.sessionToken;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $scope.getSessionTokenRepeat = function() {
        // $scope.getSessionTokenSubmit();
        if (!angular.isDefined($scope.getSessionTokenTimer)) {
            $scope.getSessionTokenTimer = $interval(function () {
                $scope.getSessionTokenSubmit();
            }, 300 * 1000);
        }
    };

    $scope.getSessionTokenStop = function() {
        if (angular.isDefined($scope.getSessionTokenTimer)) {
            $interval.cancel($scope.getSessionTokenTimer);
            $scope.getSessionTokenTimer = undefined;
        }
    };

    $scope.getSessionTokenRepeat();

    //websocket listen chat session
    $scope.listenChat = function() {
        $rootScope.uid = $cookies.get('uid');
        $rootScope.userToken = $cookies.get('userToken');
        var host = window.location.host;
        var wsUri = "ws://" + host + "/ws-chat";
        $rootScope.wsChatSession = new WebSocket(wsUri);
        $rootScope.wsChatSession.binaryType = 'arraybuffer';
        $rootScope.listenWs(
            $rootScope.wsChatSession,
            function(evt) {
                $scope.messages.push(JSON.parse(evt.data));
                //when push to array, must tell angular to update the view and update $$hashKey
                $scope.$apply();
                window.scrollTo(0, document.body.scrollHeight);
            },
            function() {
                //it must wait until chatSessionActor created!
                $timeout(function() {
                    var onlineData = {
                        "userToken": $rootScope.userToken,
                        "sessionToken": $scope.sessionToken,
                        "msgType": "online",
                        "content": ""
                    };
                    $rootScope.sendWsMessage($rootScope.wsChatSession, JSON.stringify(onlineData));
                }, 500);
            }
        );
    };
    $scope.listenChat();

    //when route change, close websocket and get session token timer
    $scope.$on('$destroy',function(){
        if ($rootScope.wsChatSession) {
            $rootScope.closeWs($rootScope.wsChatSession);
        }
        $scope.getSessionTokenStop();
    });

    $rootScope.sendChatMessage = function() {
        if ($rootScope.wsChatSession) {
            var messageContent = $('#messageContent')[0];
            var message = messageContent.value.replace(/^\s+|\s+$/g, '');
            var postData = {
                "userToken": $rootScope.userToken,
                "sessionToken": $scope.sessionToken,
                "msgType": "text",
                "content": message
            };
            if (message != '') {
                $rootScope.sendWsMessage($rootScope.wsChatSession, JSON.stringify(postData));
            }
            messageContent.value = "";
        }
    };

    $rootScope.sendChatImage = function() {
        if ($rootScope.wsChatSession) {
            var chatImage = $('#chatImage')[0];
            var file = chatImage.files[0];
            if (file) {
                var isImage = file.type.startsWith("image/");
                var isFitSize = file.size < 4 * 1024 * 1024;
                if (isImage && isFitSize) {
                    $rootScope.sendWsBinary($rootScope.wsChatSession, file, $rootScope.userToken, $scope.sessionToken);
                    $('#sendMessageMenu').trigger('click');
                } else if (! isImage) {
                    Materialize.toast('file type must be image!', 3000);
                } else {
                    Materialize.toast('file size limit 4M!', 3000);
                }
            } else {
                Materialize.toast('please select file first!', 3000);
            }
            chatImage.value = "";
        }
    };

    $rootScope.sendChatFile = function() {
        if ($rootScope.wsChatSession) {
            var chatFile = $('#chatFile')[0];
            var file = chatFile.files[0];
            if (file) {
                var isFitSize = file.size < 4 * 1024 * 1024;
                if (isFitSize) {
                    $rootScope.sendWsBinary($rootScope.wsChatSession, file, $rootScope.userToken, $scope.sessionToken);
                    $('#sendMessageMenu').trigger('click');
                } else {
                    Materialize.toast('file size limit 4M!', 3000);
                }
            } else {
                Materialize.toast('please select file first!', 3000);
            }
            chatFile.value = "";
        }
    };


});

