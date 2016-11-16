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
                Materialize.toast("error: " + $rootScope.errmsg, 4000);
            } else {
                var expiresDate = new Date();
                //cookies will expires after 15 minutes
                expiresDate.setTime(expiresDate.getTime() + 15 * 60 * 1000);
                $cookies.put('uid', response.data.uid, {'expires': expiresDate});
                $cookies.put('userToken', response.data.token, {'expires': expiresDate});

                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 4000);
                window.location.href = '#!/chatlist';
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    }

});

