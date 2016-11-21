/**
 * Created by cookeem on 16/6/3.
 */
app.controller('registerAppCtl', function($rootScope, $scope, $cookies, $timeout, $http) {
    //Hide sidebar when init
    $rootScope.showNavbar = true;
    //Hide footer when init
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
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
                $rootScope.uid = response.data.uid;
                $rootScope.userToken = response.data.token;
                //cookies will expires after 15 minutes
                var expiresDate = new Date();
                expiresDate.setTime(expiresDate.getTime() + 15 * 60 * 1000);
                $cookies.put('uid', $rootScope.uid, {'expires': expiresDate});
                $cookies.put('userToken', $rootScope.userToken, {'expires': expiresDate});

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

