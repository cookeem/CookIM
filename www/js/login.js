/**
 * Created by cookeem on 16/6/3.
 */
app.controller('loginAppCtl', function($rootScope, $scope, $cookies, $timeout, $http) {
    $rootScope.showSideNavbar = false;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = false;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - User login",
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
        $('label').addClass('active');
    }, 0);

    var cookie_login = "";
    var cookie_password = "";
    var cookie_remember = false;
    if ($cookies.get('login')) {
        cookie_login = $cookies.get('login');
    }
    if ($cookies.get('password')) {
        cookie_password = $cookies.get('password');
    }
    if ($cookies.get('remember')) {
        cookie_remember = true;
    }
    $scope.loginData = {
        "login": cookie_login,
        "password": cookie_password,
        "remember": cookie_remember
    };

    $scope.loginSubmit = function() {
        $scope.submitData = {
            "login": $scope.loginData.login,
            "password": $scope.loginData.password
        };

        $http({
            method  : 'POST',
            url     : '/api/loginUser',
            data    : $.param($scope.submitData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if ($scope.loginData.remember) {
                $cookies.put('login', $scope.loginData.login);
                $cookies.put('password', $scope.loginData.password);
                $cookies.put('remember', $scope.loginData.remember);
            } else {
                $cookies.remove('login');
                $cookies.remove('password');
                $cookies.remove('remember');
            }
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.setCookieUserToken(response.data.uid, response.data.userToken);
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 3000);

                //redirect url
                window.location.href = '#!/chatlist/joined';
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    }
});


