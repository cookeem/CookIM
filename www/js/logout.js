/**
 * Created by cookeem on 16/6/2.
 */
app.controller('logoutAppCtl', function($rootScope, $scope, $cookies, $http, $timeout) {

    $rootScope.verifyUserToken();

    $scope.logoutSubmit = function() {
        var submitData = {
            "userToken": $rootScope.userToken
        };

        $http({
            method  : 'POST',
            url     : '/api/logoutUser',
            data    : $.param(submitData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
                window.history.back();
            } else {
                $cookies.remove('uid');
                $cookies.remove('userToken');
                $rootScope.uid = "";
                $rootScope.userToken = "";

                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 3000);
                $timeout(function() {
                    if ($rootScope.websocketUserToken) {
                        //close websocket when logout
                        $rootScope.closeWebsocket($rootScope.websocketUserToken);
                    }
                }, 1000);
                window.location.href = '#!/login';
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    };
    $scope.logoutSubmit();
});