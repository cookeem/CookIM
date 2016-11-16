/**
 * Created by cookeem on 16/6/2.
 */
app.controller('logoutAppCtl', function($rootScope, $scope, $cookies, $http) {
    var cookie_userToken = "";
    if ($cookies.get('userToken')) {
        cookie_userToken = $cookies.get('userToken');
    }

    $scope.logoutSubmit = function() {
        $scope.submitData = {
            "userToken": cookie_userToken
        };

        $http({
            method  : 'POST',
            url     : '/api/logoutUser',
            data    : $.param($scope.submitData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 4000);
                window.history.back();
            } else {
                $cookies.remove('uid');
                $cookies.remove('userToken');

                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 4000);
                window.location.href = '#!/login';
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    };
    $scope.logoutSubmit();
});