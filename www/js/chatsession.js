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

    $scope.sessionid = $routeParams.querystring;

    $rootScope.sendChatMessage = function() {
        if ($rootScope.websocketChatSession) {
            var message = $('#messageContent')[0].value;
            $rootScope.sendWebsocketMessage($rootScope.websocketChatSession, message);
        }
    };

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
                $scope.listenWebsocketChatSession($scope.sessionToken);
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    };

    $scope.listMessagesSubmit();

    //websocket listen sessionToken
    $scope.listenWebsocketSessionToken = function() {
        $rootScope.uid = $cookies.get('uid');
        $rootScope.userToken = $cookies.get('userToken');
        var host = window.location.host;
        var wsUri = "ws://" + host + "/ws-session";
        $rootScope.websocketSessionToken = new WebSocket(wsUri);
        $rootScope.websocketSessionToken.binaryType = 'arraybuffer';
        $rootScope.listenWebsocket(
            $rootScope.websocketSessionToken,
            function(evt) {
                var json = JSON.parse(evt.data);
                if (json.uid != "") {
                    $scope.errmsg = json.errmsg;
                    $scope.sessionToken = json.sessionToken;
                }
                $rootScope.showWebsocketMessage(evt.data);
            }
        );

        $interval(function () {
            var postData = {
                "userToken": $rootScope.userToken,
                "sessionid": $scope.sessionid
            };
            $rootScope.sendWebsocketMessage($rootScope.websocketSessionToken, JSON.stringify(postData));
        }, 15000);
    };

    $scope.listenWebsocketSessionToken();

    //websocket listen chat session
    $scope.listenWebsocketChatSession = function(sessionid) {
        $rootScope.uid = $cookies.get('uid');
        $rootScope.userToken = $cookies.get('userToken');

        var host = window.location.host;
        var wsUri = "ws://" + host + "/ws-chat";
        $rootScope.websocketChatSession = new WebSocket(wsUri);
        $rootScope.websocketChatSession.binaryType = 'arraybuffer';
        $rootScope.listenWebsocket(
            $rootScope.websocketChatSession,
            function(evt) {
                $rootScope.showWebsocketMessage(evt.data);
            }
        );

        $interval(function () {
            var postData = {
                "userToken": $rootScope.userToken,
                "sessionToken": $scope.sessionToken,
                "msgType":"text",
                "content":"xxx"
            };
            $rootScope.sendWebsocketMessage($rootScope.websocketSessionToken, JSON.stringify(postData));
        }, 15000);

    };

});