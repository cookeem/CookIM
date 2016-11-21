/**
 * Created by cookeem on 16/6/3.
 */
app.controller('chatSessionAppCtl', function($rootScope, $scope, $cookies, $timeout, $routeParams, $http, $interval) {
    //Hide sidebar when init
    $rootScope.showNavbar = true;
    //Hide footer when init
    $rootScope.showMessageArea = true;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
        $('.dropdown-button').dropdown({
                inDuration: 300,
                outDuration: 225,
                constrain_width: false, // Does not change width of dropdown to that of the activator
                hover: true, // Activate on hover
                gutter: 0, // Spacing from edge
                belowOrigin: false, // Displays dropdown below the button
                alignment: 'left' // Displays dropdown with edge aligned to the left of button
            }
        );
    }, 0);

    $rootScope.verifyUserToken();

    //refresh userToken on websocket
    if (!$rootScope.wsUserToken || $rootScope.userToken == "") {
        $rootScope.listenUserToken();
    }

    $scope.sessionid = $routeParams.querystring;

    $scope.listMessagesData = {
        "sessionid": $scope.sessionid,
        "page": 1,
        "count": 10,
        "userToken": $rootScope.userToken
    };
    $scope.listMessagesResults = [];
    $scope.sessionToken = "";
    $scope.listMessagesSubmit = function() {
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
                $scope.listMessagesResults = response.data.messages;
                $scope.sessionToken = response.data.sessionToken;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $scope.listMessagesSubmit();

    //websocket listen sessionToken
    $scope.listenSessionToken = function() {
        $rootScope.uid = $cookies.get('uid');
        $rootScope.userToken = $cookies.get('userToken');
        var host = window.location.host;
        var wsUri = "ws://" + host + "/ws-session";
        $rootScope.wsSessionToken = new WebSocket(wsUri);
        $rootScope.wsSessionToken.binaryType = 'arraybuffer';
        $rootScope.listenWs(
            $rootScope.wsSessionToken,
            function(evt) {
                var json = JSON.parse(evt.data);
                $scope.errmsg = json.errmsg;
                $scope.sessionToken = json.sessionToken;
                $rootScope.showWsMessage(evt.data);
            },
            function() {
                var postData = {
                    "userToken": $rootScope.userToken,
                    "sessionid": $scope.sessionid
                };
                $rootScope.sendWsMessage($rootScope.wsSessionToken, JSON.stringify(postData));
            }
        );

        $interval(function () {
            var postData = {
                "userToken": $rootScope.userToken,
                "sessionid": $scope.sessionid
            };
            $rootScope.sendWsMessage($rootScope.wsSessionToken, JSON.stringify(postData));
        }, 15000);
    };
    $scope.listenSessionToken();

    //websocket listen chat session
    $scope.output = [];
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
                var output = $('#output')[0];
                var pre = document.createElement("p");
                pre.style.wordBreak = "break-all";
                pre.innerHTML = evt.data;
                output.appendChild(pre);
                //$scope.output.push(JSON.parse(evt.data));
            },
            function() {
                var onlineData = {
                    "userToken": $rootScope.userToken,
                    "sessionToken": $scope.sessionToken,
                    "msgType": "online",
                    "content": ""
                };
                $rootScope.sendWsMessage($rootScope.wsChatSession, JSON.stringify(onlineData));
            }
        );
    };
    $scope.listenChat();

    $rootScope.sendChatMessage = function() {
        if ($rootScope.wsChatSession) {
            var message = $('#messageContent')[0].value;
            var postData = {
                "userToken": $rootScope.userToken,
                "sessionToken": $scope.sessionToken,
                "msgType": "text",
                "content": message
            };
            $rootScope.sendWsMessage($rootScope.wsChatSession, JSON.stringify(postData));
        }
    };

});