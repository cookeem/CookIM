/**
 * Created by cookeem on 16/6/3.
 */
app.controller('changePwdAppCtl', function($rootScope, $scope, $cookies, $timeout, $http) {
    $rootScope.showSideNavbar = true;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = true;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - Change login password",
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

    $rootScope.verifyUserToken();

    $rootScope.getUserTokenRepeat();

    $scope.changePwdData = {
        "oldPwd": "",
        "newPwd": "",
        "renewPwd": "",
        "userToken": $rootScope.userToken
    };

    $scope.changePwdSubmit = function() {
        $rootScope.verifyUserToken();
        $http({
            method  : 'POST',
            url     : '/api/changePwd',
            data    : $.param($scope.changePwdData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 3000);
                window.location.href = '#!/chatlist/joined';
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

});