/**
 * Created by cookeem on 16/6/3.
 */
app.controller('registerAppCtl', function($rootScope, $scope, $cookies, $timeout, $http) {
    $rootScope.showSideNavbar = false;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = false;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - Register user",
        //title icon
        icon: "images/cookim.svg",
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

    $scope.registerData = {
        "login": "",
        "nickname": "",
        "password": "",
        "repassword": "",
        "gender": 0
    };

    $scope.registerSubmit = function() {
        $http({
            method  : 'POST',
            url     : '/api/registerUser',
            data    : $.param($scope.registerData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.setCookieUserToken(response.data.uid, response.data.userToken);
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 3000);

                //redirect url
                window.location.href = '#!/chatlist/public';
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    }

});

