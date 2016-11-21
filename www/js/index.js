/**
 * Created by cookeem on 16/9/27.
 */

var app = angular.module('app', ['ngRoute', 'ngAnimate', 'ngCookies']);

app.config(function($routeProvider, $locationProvider) {
    $routeProvider
        .when('/error', {
            templateUrl: 'error.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/login', {
            templateUrl: 'login.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/register', {
            templateUrl: 'register.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/chatlist/:querystring', {
            templateUrl: 'chatlist.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/chatsession/:querystring', {
            templateUrl: 'chatsession.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/changepwd', {
            templateUrl: 'changepwd.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/changeinfo', {
            templateUrl: 'changeinfo.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/logout', {
            templateUrl: 'logout.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .otherwise({redirectTo: '/chatlist/public'});
    //使用#!作为路由前缀
    $locationProvider.html5Mode(false).hashPrefix('!');
});

app.controller('headerCtl', function($rootScope) {
    //sideNav菜单项初始化
    $rootScope.showSideBar = false;
    //Hide footer when init
    $rootScope.showMessageArea = false;

    $rootScope.menuItems = [
        {
            "menuName": "Chats Public",
            "url": "#!/chatlist/public"
        },
        {
            "menuName": "Chats Joined",
            "url": "#!/chatlist/joined"
        }
    ];

    $rootScope.accountMenuItems = [
    ];

    //sideNav初始化
    var sidebarWidth = 240;
    if ($(window).width() > 992) {
        $('.button-collapse').sideNav({
                'menuWidth': sidebarWidth, // Default is 240
                'edge': 'left', // Choose the horizontal origin
                'closeOnClick': false // Closes side-nav on <a> clicks, useful for Angular/Meteor
            }
        );
    } else {
        $('.button-collapse').sideNav({
                'menuWidth': sidebarWidth, // Default is 240
                'edge': 'left', // Choose the horizontal origin
                'closeOnClick': true // Closes side-nav on <a> clicks, useful for Angular/Meteor
            }
        );
    }

});

app.controller('contentCtl', function($rootScope, $scope, $cookies, $route, $http, $interval) {
    //when loading show the preloader
    $rootScope.$on('$routeChangeStart', function(event, currRoute){
        $rootScope.animation = currRoute.animation;
        $('html, body').animate({scrollTop:0}, 0);
        $rootScope.isLoading = true;
    });
    //when load finished hide the preloader
    $rootScope.$on('$routeChangeSuccess', function() {
        $rootScope.isLoading = false;
    });

    //global variable
    $rootScope.errmsg = '';
    $rootScope.uid = '';
    $rootScope.userToken = '';

    //verify user token, if failure then redirect to error page
    $rootScope.verifyUserToken = function() {
        if ($cookies.get('uid')) {
            $rootScope.uid = $cookies.get('uid');
        }
        if ($cookies.get('userToken')) {
            $rootScope.userToken = $cookies.get('userToken');
        }
        if ($rootScope.userToken == "") {
            $rootScope.errmsg = "no privilege or not login";
            window.location.href = '#!/error';
        }
        var userTokenData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/verifyUserToken',
            data    : $.param(userTokenData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            if (response.data.uid == "") {
                $rootScope.errmsg = "no privilege or not login";
                window.location.href = '#!/error';
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    // create session interface
    $scope.sessionData = {
        "name" : "",
        "publictype": true
    };
    $scope.createSessionSubmit = function() {
        $rootScope.verifyUserToken();
        var publictype = 0;
        if ($scope.sessionData.publictype) {
            publictype = 1;
        }
        var formData = new FormData();
        formData.append("publictype", publictype);
        formData.append("name", $scope.sessionData.name);
        formData.append("userToken", $rootScope.userToken);
        var chatIconInput = $('#chatIconInput')[0];
        if (chatIconInput.files && chatIconInput.files[0]) {
            formData.append("chaticon", chatIconInput.files[0]);
        }

        $http({
            method  : 'POST',
            url     : '/api/createGroupSession',
            // mix file upload and form multipart
            data    : formData,
            transformRequest: angular.identity,
            headers : { 'Content-Type': undefined }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 3000);
                $('#modalCreateSession').modal('close');
                $('#chatIcon')[0].src = "images/avatar/unknown.jpg";
                $scope.sessionData = {
                    "name" : "",
                    "publictype": true
                };
                var elmChatIconInput = $('#chatIconInput')[0];
                elmChatIconInput.value = "";
                window.location.href = '#!/chatlist/joined';
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //websocket listen userToken
    $rootScope.listenUserToken = function() {
        $rootScope.uid = $cookies.get('uid');
        $rootScope.userToken = $cookies.get('userToken');
        var host = window.location.host;
        var wsUri = "ws://" + host + "/ws-user";
        $rootScope.wsUserToken = new WebSocket(wsUri);
        $rootScope.wsUserToken.binaryType = 'arraybuffer';
        $rootScope.listenWs(
            $rootScope.wsUserToken,
            function(evt) {
                var json = JSON.parse(evt.data);
                if (json.uid != "") {
                    $rootScope.uid = json.uid;
                    $rootScope.userToken = json.userToken;
                    //cookies will expires after 15 minutes
                    var expiresDate = new Date();
                    expiresDate.setTime(expiresDate.getTime() + 15 * 60 * 1000);
                    $cookies.put('uid', $rootScope.uid, {'expires': expiresDate});
                    $cookies.put('userToken', $rootScope.userToken, {'expires': expiresDate});
                }
                $rootScope.showWsMessage(evt.data);
            },
            function() {
                var postData = {
                    "userToken": $rootScope.userToken
                };
                $rootScope.sendWsMessage($rootScope.wsUserToken, JSON.stringify(postData));
            }
        );

        $interval(function () {
            var postData = {
                "userToken": $rootScope.userToken
            };
            $rootScope.sendWsMessage($rootScope.wsUserToken, JSON.stringify(postData));
        }, 15000);
    };



    $rootScope.listenWs = function(ws, onWsMessage, onWsOpen, onWsError, onWsClose) {
        var i = 0;
        if (typeof(onWsMessage)==='undefined') onWsMessage = function(evt) {
            $rootScope.showWsMessage("RECEIVE: " + evt);
        };

        if (typeof(onWsOpen)==='undefined') onWsOpen = function() {
            $rootScope.showWsMessage("CONNECTED");
        };

        if (typeof(onWsError)==='undefined') onWsError = function(evt) {
            $rootScope.showWsMessage('ERROR:\n' + evt.data);
        };

        if (typeof(onWsClose)==='undefined') onWsClose = function(ws) {
            i = i + 1;
            $rootScope.showWsMessage("DISCONNECTED");
            if (i < 3) {
                $rootScope.listenWs(ws, onWsMessage, onWsOpen, onWsClose, onWsError);
            }
        };

        ws.onmessage = onWsMessage;
        ws.onopen = onWsOpen;
        ws.onclose = onWsClose;
        ws.onerror = onWsError;
    };

    $rootScope.onWsMessage = function(evt) {
        $rootScope.showWsMessage(evt.data);
    };

    //show text websocket message
    $rootScope.showWsMessage = function(message) {
        console.log(message);
    };

    //send message through websocket
    $rootScope.sendWsMessage = function(ws, message) {
        ws.send(message);
    };

    //send file through websocket
    $rootScope.sendWsFile = function(ws) {
        var file = document.getElementById('filename').files[0];
        var isImage = file.type.startsWith("image/");
        var isFitSize = file.size < 2048 * 1024;
        if (isImage && isFitSize) {
            var reader = new FileReader();
            reader.readAsArrayBuffer(file);
            reader.onloadend = function() {
                var fileInfo = {
                    filename: file.name,
                    filesize: file.size,
                    filetype: file.type
                };
                var fileInfoStr = JSON.stringify(fileInfo) + "<#BinaryInfo#>";
                var fileInfoBuf = utf8StringToArrayBuffer(fileInfoStr);
                var fileBuf = reader.result;
                var mixBuf = concatenateBuffers(fileInfoBuf, fileBuf);
                ws.send(mixBuf);
            };
        } else if (! isImage) {
            alert('file type must be image!');
        } else {
            alert('file size limit 2048 * 1024!');
        }
    };

    $rootScope.closeWs = function(ws) {
        ws.close();
    };

    //materializecss init
    $('.modal').modal();
});

app.filter('trustHtml', function ($sce) {
    return function (input) {
        return $sce.trustAsHtml(input);
    }
});

//show or hide materializecss sidebar
function showHideSideBar(isShow) {
    if (isShow) {
        if ($(window).width() > 992) {
            $('header, main, footer').css('padding-left', '240px');
        } else {
            $('header, main, footer').css('padding-left', '0');
        }
    } else {
        $('header, main, footer').css('padding-left', '0');
    }
}

//utf8 string to array buffer
function utf8StringToArrayBuffer(s) {
    var escstr = encodeURIComponent(s);
    var binstr = escstr.replace(/%([0-9A-F]{2})/g, function(match, p1) {
        return String.fromCharCode('0x' + p1);
    });
    var ua = new Uint8Array(binstr.length);
    Array.prototype.forEach.call(binstr, function (ch, i) {
        ua[i] = ch.charCodeAt(0);
    });
    return ua;
}

//array buffer to utf8 string
function arrayBufferToUtf8String(ua) {
    var binstr = Array.prototype.map.call(ua, function (ch) {
        return String.fromCharCode(ch);
    }).join('');
    var escstr = binstr.replace(/(.)/g, function (m, p) {
        var code = p.charCodeAt(p).toString(16).toUpperCase();
        if (code.length < 2) {
            code = '0' + code;
        }
        return '%' + code;
    });
    return decodeURIComponent(escstr);
}

//concat to array buffer, use for websocket concat binary info into binary array buffer
function concatenateBuffers(buffA, buffB) {
    var byteLength = buffA.byteLength + buffB.byteLength;
    var resultBuffer = new ArrayBuffer(byteLength);
    var resultView = new Uint8Array(resultBuffer);
    var viewA = new Uint8Array(buffA);
    var viewB = new Uint8Array(buffB);
    resultView.set(viewA);
    resultView.set(viewB, viewA.byteLength);
    return resultView.buffer
}

var showChatIcon = function(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function (e) {
            $('#chatIcon').attr('src', e.target.result);
        };
        reader.readAsDataURL(input.files[0]);
    }
};
