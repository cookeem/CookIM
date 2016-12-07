/**
 * Created by cookeem on 16/9/27.
 */

var app = angular.module('app', ['ngRoute', 'ngAnimate', 'ngCookies', 'ui.materialize']).run(function($rootScope, $timeout) {
    //init global variable

    //check page is loading
    $rootScope.isLoading = true;
    //show error.html page error message
    $rootScope.errmsg = '';
    //login user uid
    $rootScope.uid = '';
    //login user token
    $rootScope.userToken = '';

    //show left side nav bar
    $rootScope.showSideNavbar = false;
    //show buttom message area
    $rootScope.showMessageArea = false;
    //show top rigth account menu
    $rootScope.showAccoutMenu = false;
    //top bar title info
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM",
        //title icon
        icon: "images/cookim.svg",
        //useful when mode == "group_session"
        sessionid: "",
        //useful when mode == "private_session"
        uid: ""
    };

    //side nav bar joined chat menu items
    $rootScope.joinedSessions = [];
    //show in chatlist page
    $rootScope.listSessionsResults = [];

    $timeout(function() {
        //materializecss init
        //sideNavbar init
        // if ($(window).width() > 992) {
        //     $('.button-collapse').sideNav({
        //             'menuWidth': 240, // Default is 240
        //             'edge': 'left', // Choose the horizontal origin
        //             'closeOnClick': false // Closes side-nav on <a> clicks, useful for Angular/Meteor
        //         }
        //     );
        // } else {
        //     $('.button-collapse').sideNav({
        //             'menuWidth': 240, // Default is 240
        //             'edge': 'left', // Choose the horizontal origin
        //             'closeOnClick': true // Closes side-nav on <a> clicks, useful for Angular/Meteor
        //         }
        //     );
        // }

        $('.modal').modal();
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
        $('select').material_select();
    }, 0);

});

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
        .when('/friends', {
            templateUrl: 'friends.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/notifications', {
            templateUrl: 'notifications.html',
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
        .otherwise({redirectTo: '/login'});
    //use /#! as route path
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
            "url": "#!/chatlist/public",
            "rsCount": 0
        },
        {
            "menuName": "Chats Joined",
            "url": "#!/chatlist/joined",
            "rsCount": 0
        },
        {
            "menuName": "Friends",
            "url": "#!/friends",
            "rsCount": 0
        },
        {
            "menuName": "Notifications",
            "url": "#!/notifications",
            "rsCount": 0
        }
    ];

});

app.controller('contentCtl', function($rootScope, $scope, $cookies, $route, $http, $interval, $timeout) {

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

    //get rootScope.userToken and rootScope.uid from cookies
    $rootScope.getCookieUserToken = function() {
        if ($cookies.get('uid')) {
            $rootScope.uid = $cookies.get('uid');
        } else {
            $rootScope.uid = "";
        }
        if ($cookies.get('userToken')) {
            $rootScope.userToken = $cookies.get('userToken');
        } else {
            $rootScope.userToken = "";
        }
    };

    //set rootScope.userToken and rootScope.uid and cookies
    $rootScope.setCookieUserToken = function(uid, userToken) {
        $rootScope.uid = uid;
        $rootScope.userToken = userToken;
        //cookies will expires after 15 minutes
        var expiresDate = new Date();
        expiresDate.setTime(expiresDate.getTime() + 15 * 60 * 1000);
        $cookies.put('uid', $rootScope.uid, {'expires': expiresDate});
        $cookies.put('userToken', $rootScope.userToken, {'expires': expiresDate});
    };

    //verify user token, if failure then redirect to error page
    $rootScope.verifyUserToken = function() {
        $rootScope.getCookieUserToken();
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

    //get userToken request
    $rootScope.getUserTokenSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/userToken',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.setCookieUserToken(response.data.uid, response.data.userToken);
                $rootScope.listJoinedSessionsSubmit();
                $rootScope.getNewNotificationCountSubmit();
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $rootScope.getUserTokenRepeat = function() {
        $rootScope.getUserTokenSubmit();
        if (!angular.isDefined($rootScope.getUserTokenTimer)) {
            $rootScope.getUserTokenTimer = $interval(function () {
                $rootScope.getUserTokenSubmit();
            }, 300 * 1000);
        }
    };

    $rootScope.getUserTokenStop = function() {
        if (angular.isDefined($rootScope.getUserTokenTimer)) {
            $interval.cancel($rootScope.getUserTokenTimer);
            $rootScope.getUserTokenTimer = undefined;
        }
    };

    //list joined sessions list
    $rootScope.listJoinedSessionsSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/listJoinedSessions',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.joinedSessions = response.data.sessions;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //get new notification count
    $rootScope.getNewNotificationCountSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/getNewNotificationCount',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.menuItems.forEach(function(menuItem) {
                    if (menuItem.url == "#!/notifications") {
                        menuItem.rsCount = response.data.rsCount;
                    }
                });
                $timeout(function() {
                    $rootScope.$apply();
                }, 0);
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    /*************************************************************************/
    //session create and edit
    $scope.sessionData = {
        "sessionName" : "",
        "publicType": true
    };

    //create session
    $scope.createSessionSubmit = function() {
        $rootScope.verifyUserToken();
        var publicType = 0;
        if ($scope.sessionData.publicType) {
            publicType = 1;
        }
        var formData = new FormData();
        formData.append("publicType", publicType);
        formData.append("sessionName", $scope.sessionData.sessionName);
        formData.append("userToken", $rootScope.userToken);
        var chatIconInput = $('#chatIconInput')[0];
        if (chatIconInput.files && chatIconInput.files[0]) {
            formData.append("sessionIcon", chatIconInput.files[0]);
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
                    "sessionName" : "",
                    "publicType": true
                };
                chatIconInput.value = "";
                $rootScope.redirectOrReload('/chat/#!/chatlist/joined');
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $rootScope.sessionDataEdit = {
        "sessionName" : "",
        "sessionIcon" : "",
        "publicType": true
    };

    //get edit group session info
    $rootScope.sessionidEdit = "";
    $rootScope.getEditSessionSubmit = function(sessionid) {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "sessionid": sessionid
        };
        $http({
            method  : 'POST',
            url     : '/api/getGroupSessionInfo',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.sessionDataEdit.sessionName = response.data.session.sessionName;
                $rootScope.sessionDataEdit.sessionIcon = response.data.session.sessionIcon;
                $rootScope.sessionDataEdit.publicType = response.data.session.publicType == 1;
                $('#modalEditSession').modal('open');
                $('#modalEditSession label').addClass('active');
                $('#modalEditSession i').addClass('active');
                $rootScope.sessionidEdit = sessionid;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //edit session
    $rootScope.editSessionSubmit = function() {
        $rootScope.verifyUserToken();
        var publicType = 0;
        if ($rootScope.sessionDataEdit.publicType) {
            publicType = 1;
        }
        var formData = new FormData();
        formData.append("publicType", publicType);
        formData.append("sessionName", $rootScope.sessionDataEdit.sessionName);
        formData.append("userToken", $rootScope.userToken);
        formData.append("sessionid", $rootScope.sessionidEdit);
        var chatIconInputEdit = $('#chatIconInputEdit')[0];
        if (chatIconInputEdit.files && chatIconInputEdit.files[0]) {
            formData.append("sessionIcon", chatIconInputEdit.files[0]);
        }
        $http({
            method  : 'POST',
            url     : '/api/editGroupSession',
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
                $('#modalEditSession').modal('close');
                $rootScope.hideSessionMenu();
                $('#chatIconEdit')[0].src = "images/avatar/unknown.jpg";
                $rootScope.sessionDataEdit = {
                    "sessionName" : "",
                    "publicType": true
                };
                chatIconInputEdit.value = "";
                $rootScope.sessionidEdit = "";
                $rootScope.redirectOrReload('/chat/#!/chatlist/joined');
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //get joined users
    $rootScope.joinedUsers = {
      "onlineUsers": [],
      "offlineUsers": []
    };
    $rootScope.getJoinedUsersSubmit = function(sessionid) {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "sessionid": sessionid
        };
        $http({
            method  : 'POST',
            url     : '/api/getJoinedUsers',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.joinedUsers.onlineUsers = response.data.onlineUsers;
                $rootScope.joinedUsers.offlineUsers = response.data.offlineUsers;
                $('#modalJoinedUsers').modal('open');
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };
    $rootScope.hideJoinedUsersModal = function() {
        $('#modalJoinedUsers').modal('close');
    };

    //join session
    $rootScope.sessionidToJoin = "";
    $rootScope.showJoinSessionModal = function(sessionid) {
        $rootScope.sessionidToJoin = sessionid;
        $('#modalJoinSession').modal('open');
    };

    $rootScope.hideJoinSessionModal = function() {
        $rootScope.sessionidToJoin = "";
        $('#modalSessionMenu').modal('close');
        $('#modalJoinSession').modal('close');
    };

    $rootScope.joinGroupSessionSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "sessionid": $rootScope.sessionidToJoin
        };
        $http({
            method  : 'POST',
            url     : '/api/joinGroupSession',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.hideJoinSessionModal();
                window.location.href = "/chat/#!/chatsession/" + postData.sessionid;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //leave session
    $rootScope.sessionidToLeave = "";
    $rootScope.showLeaveSessionModal = function(sessionid) {
        $rootScope.sessionidToLeave = sessionid;
        $('#modalLeaveSession').modal('open');
    };

    $rootScope.hideLeaveSessionModal = function() {
        $rootScope.sessionidToLeave = "";
        $('#modalLeaveSession').modal('close');
        $rootScope.hideSessionMenu();
    };

    $rootScope.leaveGroupSessionSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "sessionid": $rootScope.sessionidToLeave
        };
        $http({
            method  : 'POST',
            url     : '/api/leaveGroupSession',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.hideLeaveSessionModal();
                $rootScope.redirectOrReload("/chat/#!/chatlist/joined");
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //invite friends
    $rootScope.sessionidToInvite = "";
    $rootScope.ouidToInvite = "";
    $rootScope.friendsFormData = {
        friendsToInvite: []
    };
    $rootScope.friendsList = [];

    $rootScope.showInviteFriendsModal = function(sessionid, ouid) {
        $rootScope.sessionidToInvite = sessionid;
        $rootScope.ouidToInvite = ouid;
        $rootScope.getInviteFriendsSubmit();
    };

    $rootScope.hideInviteFriendsModal = function() {
        $rootScope.sessionidToInvite = "";
        $rootScope.ouidToInvite = "";
        $rootScope.friendsFormData.friendsToInvite = [];
        $rootScope.friendsList = [];
        $('#modalInviteFriends').modal('close');
        $rootScope.hideSessionMenu();
        $rootScope.hideUserMenu();
    };

    $rootScope.getInviteFriendsSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/getFriends',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.friendsList = response.data.friends;
                $('#modalInviteFriends').modal('open');
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $rootScope.inviteFriendsSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "ouid": $rootScope.ouidToInvite,
            "sessionid": $rootScope.sessionidToInvite,
            "friends": JSON.stringify($rootScope.friendsFormData.friendsToInvite)
        };
        $http({
            method  : 'POST',
            url     : '/api/inviteFriends',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.hideInviteFriendsModal();
                Materialize.toast($rootScope.successmsg, 3000);
                $rootScope.redirectOrReload("/chat/#!/chatsession/"+response.data.sessionid);
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //join friend
    $rootScope.friendidToJoin = "";
    $rootScope.friendnicknameToJoin = "";
    $rootScope.showJoinFriendModal = function(friendid, friendnickname) {
        $rootScope.friendidToJoin = friendid;
        $rootScope.friendnicknameToJoin = friendnickname;
        $('#modalJoinFriend').modal('open');
    };

    $rootScope.hideJoinFriendModal = function() {
        $rootScope.friendidToJoin = "";
        $rootScope.friendnicknameToJoin = "";
        $('#modalJoinFriend').modal('close');
        $rootScope.hideUserMenu();
        $rootScope.hideSessionMenu();
        $rootScope.hideJoinedUsersModal();
    };

    $rootScope.joinFriendSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "fuid": $rootScope.friendidToJoin
        };
        $http({
            method  : 'POST',
            url     : '/api/joinFriend',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                Materialize.toast("success: " + response.data.successmsg, 3000);
                $rootScope.hideJoinFriendModal();
                $rootScope.redirectOrReload("/chat/#!/friends");
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //remove friend
    $rootScope.friendidToRemove = "";
    $rootScope.friendnicknameToRemove = "";
    $rootScope.showRemoveFriendModal = function(friendid, friendnickname) {
        $rootScope.friendidToRemove = friendid;
        $rootScope.friendnicknameToRemove = friendnickname;
        $('#modalRemoveFriend').modal('open');
    };

    $rootScope.hideRemoveFriendModal = function() {
        $rootScope.friendidToRemove = "";
        $rootScope.friendnicknameToRemove = "";
        $('#modalRemoveFriend').modal('close');
        $rootScope.hideUserMenu();
        $rootScope.hideSessionMenu();
        $rootScope.hideJoinedUsersModal();
    };

    $rootScope.removeFriendSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "fuid": $rootScope.friendidToRemove
        };
        $http({
            method  : 'POST',
            url     : '/api/removeFriend',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                Materialize.toast("success: " + response.data.successmsg, 3000);
                $rootScope.hideRemoveFriendModal();
                $rootScope.redirectOrReload("/chat/#!/friends");
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    //user menu
    $rootScope.userMenuData = {
        uid: "",
        nickname: "",
        isFriend: false
    };

    $rootScope.showUserMenu = function(ouid) {
        if ($rootScope.uid != ouid) {
            $rootScope.verifyUserToken();
            var postData = {
                "userToken": $rootScope.userToken,
                "ouid": ouid
            };
            $http({
                method  : 'POST',
                url     : '/api/getUserMenu',
                data    : $.param(postData),
                headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
            }).then(function successCallback(response) {
                console.log(response.data);
                if (response.data.errmsg) {
                    $rootScope.errmsg = response.data.errmsg;
                    Materialize.toast("error: " + $rootScope.errmsg, 3000);
                } else {
                    $rootScope.userMenuData.uid = response.data.user.uid;
                    $rootScope.userMenuData.nickname = response.data.user.nickname;
                    $rootScope.userMenuData.isFriend = response.data.user.isFriend;
                    $('#modalUserMenu').modal('open');
                }
            }, function errorCallback(response) {
                console.error("http request error:" + response.data);
            });
        } else {
            Materialize.toast("no operations for yourself", 3000)
        }
    };

    $rootScope.hideUserMenu = function() {
        $rootScope.userMenuData = {
            uid: "",
            nickname: "",
            isFriend: false
        };
        $('#modalUserMenu').modal('close');
    };

    //session menu
    $rootScope.sessionMenuData = {
        sessionid: "",
        sessionName: "",
        sessionIcon: "",
        ouid: "",
        joined: false,
        editable: false
    };
    $rootScope.showSessionMenu = function(sessionid) {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "sessionid": sessionid
        };
        $http({
            method  : 'POST',
            url     : '/api/getSessionMenu',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.sessionMenuData.sessionid = response.data.session.sessionid;
                $rootScope.sessionMenuData.sessionName = response.data.session.sessionName;
                $rootScope.sessionMenuData.sessionIcon = response.data.session.sessionIcon;
                $rootScope.sessionMenuData.ouid = response.data.session.ouid;
                $rootScope.sessionMenuData.joined = response.data.session.joined;
                $rootScope.sessionMenuData.editable = response.data.session.editable;
                $('#modalSessionMenu').modal('open');
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $rootScope.hideSessionMenu = function() {
        $rootScope.ouidToInvite = "";
        $rootScope.sessionMenuData = {
            sessionid: "",
            sessionName: "",
            sessionIcon: "",
            ouid: "",
            joined: false,
            editable: false
        };
        $('#modalSessionMenu').modal('close');
    };

    //chat with user
    $rootScope.getPrivateSessionSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "ouid": $rootScope.userMenuData.uid
        };
        $http({
            method  : 'POST',
            url     : '/api/getPrivateSession',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                var sessionid = response.data.sessionid;
                $rootScope.hideUserMenu();
                $rootScope.hideSessionMenu();
                $rootScope.hideJoinedUsersModal();
                $('#modalJoinedUsers').modal('close');
                window.location.href = "/chat/#!/chatsession/" + sessionid;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
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

    $rootScope.sendWsBinary = function(ws, file, userToken, sessionToken) {
        var reader = new FileReader();
        reader.readAsArrayBuffer(file);
        reader.onloadend = function() {
            var postData = {
                userToken: userToken,
                sessionToken: sessionToken,
                msgType: "file",
                fileName: file.name,
                fileSize: file.size,
                fileType: file.type
            };
            var headerStr = JSON.stringify(postData) + "<#BinaryInfo#>";
            var headerBuf = utf8StringToArrayBuffer(headerStr);
            var fileBuf = reader.result;
            var mixBuf = concatenateBuffers(headerBuf, fileBuf);
            ws.send(mixBuf);
        };
    };

    $rootScope.closeWs = function(ws) {
        ws.close();
    };

    //websocket listen push session
    $rootScope.listenPush = function() {
        $rootScope.uid = $cookies.get('uid');
        $rootScope.userToken = $cookies.get('userToken');
        var host = window.location.host;
        var wsProtocal = "ws:";
        if (window.location.protocol == "https:") {
            wsProtocal = "wss:"
        } else if (window.location.protocol == "http:") {
            wsProtocal = "ws:"
        }
        var wsUri = wsProtocal + "//" + host + "/ws-push";
        $rootScope.wsPushSession = new WebSocket(wsUri);
        $rootScope.wsPushSession.binaryType = 'arraybuffer';
        $rootScope.listenWs(
            $rootScope.wsPushSession,
            function(evt) {
                var json = JSON.parse(evt.data);
                $rootScope.updateJoinedSessions(json);
                // console.log(evt.data);
            },
            function() {
                //it must wait until pushSessionActor created!
                $timeout(function() {
                    var onlineData = {
                        "userToken": $rootScope.userToken
                    };
                    $rootScope.sendWsMessage($rootScope.wsPushSession, JSON.stringify(onlineData));
                }, 500);
            }
        );
    };

    $rootScope.updateJoinedSessions = function(json) {
        if (json.msgType != 'accept' && json.msgType != 'reject' && json.msgType != 'keepalive') {
            var sessionid = json.sessionid;
            if (sessionid != "" && $rootScope.titleInfo.sessionid != sessionid) {
                $rootScope.joinedSessions.forEach(function(session) {
                    if (session.sessionid == sessionid) {
                        session.newCount = session.newCount + 1;
                        session.lastUpdate = json.dateline;
                    }
                });
                $rootScope.joinedSessions = $rootScope.joinedSessions.sort(function(session1, session2) {
                    if (session1.lastUpdate > session2.lastUpdate) return -1;
                    if (session1.lastUpdate < session2.lastUpdate) return 1;
                    return 0;
                });
                $rootScope.listSessionsResults.forEach(function(session) {
                    if (session.sessionid == sessionid) {
                        console.log(session.sessionid + ':' + sessionid);
                        var content = json.content;
                        if (json.msgType == 'file' && json.fileInfo.fileThumb == "/") {
                            content = "send a [FILE]";
                        } else if (json.msgType == 'file' && json.fileInfo.fileThumb.length > 1) {
                            content = "send a [PHOTO]";
                        }
                        session.message = {
                            uid: json.uid,
                            nickname: json.nickname,
                            avatar: json.avatar,
                            msgType: json.msgType,
                            content: content,
                            dateline: json.dateline
                        };
                        session.lastUpdate = json.dateline;
                        session.newCount = session.newCount + 1;
                    }
                });
                $rootScope.listSessionsResults = $rootScope.listSessionsResults.sort(function(session1, session2) {
                    if (session1.lastUpdate > session2.lastUpdate) return -1;
                    if (session1.lastUpdate < session2.lastUpdate) return 1;
                    return 0;
                });

                $rootScope.$apply();
            }

        }
    };

    $rootScope.redirectOrReload = function(distPath) {
        var href = window.location.href;
        var origin = window.location.origin;
        var path = href.replace(origin, "");
        if (path == distPath) {
            $route.reload();
        } else {
            window.location.href = distPath;
        }

    }

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
            $('.top-right-menu').css('padding-right', '240px');
            $('.button-collapse').sideNav('destroy');
            $('.button-collapse').sideNav({
                    'menuWidth': 240, // Default is 240
                    'edge': 'left', // Choose the horizontal origin
                    'closeOnClick': false // Closes side-nav on <a> clicks, useful for Angular/Meteor
                }
            );
        } else {
            $('header, main, footer').css('padding-left', '0');
            $('.top-right-menu').css('padding-right', '0');
            $('.button-collapse').sideNav('destroy');
            $('.button-collapse').sideNav({
                    'menuWidth': 240, // Default is 240
                    'edge': 'left', // Choose the horizontal origin
                    'closeOnClick': true // Closes side-nav on <a> clicks, useful for Angular/Meteor
                }
            );
        }
    } else {
        $('header, main, footer').css('padding-left', '0');
        $('.top-right-menu').css('padding-right', '0');
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

var showChatIconEdit = function(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function (e) {
            $('#chatIconEdit').attr('src', e.target.result);
        };
        reader.readAsDataURL(input.files[0]);
    }
};

