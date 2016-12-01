/**
 * Created by cookeem on 16/6/3.
 */
app.controller('chatListAppCtl', function($rootScope, $scope, $cookies, $timeout, $routeParams, $http) {
    $rootScope.showSideNavbar = true;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = true;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM",
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
    }, 1000);

    $rootScope.getUserTokenRepeat();

    $scope.listSessionsData = {
        "isPublic": 0,
        "userToken": $rootScope.userToken
    };
    $scope.querystring = $routeParams.querystring;
    if ($scope.querystring != "public") {
        $scope.listSessionsData.isPublic = 0;
        $rootScope.titleInfo.title = 'CookIM - Chats joined';
    } else {
        $scope.listSessionsData.isPublic = 1;
        $rootScope.titleInfo.title = 'CookIM - Chats public';
    }
    $scope.listSessionsSubmit = function() {
        $rootScope.listSessionsResults = [];
        $http({
            method  : 'POST',
            url     : '/api/listSessions',
            data    : $.param($scope.listSessionsData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast($rootScope.errmsg, 4000);
            } else {
                $rootScope.listSessionsResults = response.data.sessions;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    $scope.listSessionsSubmit();

    if (!$rootScope.wsPushSession) {
        $rootScope.listenPush();
    }

});