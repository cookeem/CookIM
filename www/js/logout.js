/**
 * Created by cookeem on 16/6/2.
 */
app.controller('logoutAppCtl', function($rootScope, $scope, $cookies, $http, $timeout) {
    //Hide sidebar when init
    $rootScope.showSideNavbar = false;
    $rootScope.showAccoutMenu = false;
    //Hide footer when init
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showSideNavbar);
        $(window).resize(function() {
            showHideSideBar($rootScope.showSideNavbar);
        });
        $('label').addClass('active');
    }, 0);

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

                $rootScope.getUserTokenStop();

                if ($rootScope.wsPushSession) {
                    $rootScope.closeWs($rootScope.wsPushSession);
                }

                window.location.href = '#!/login';
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };
    $scope.logoutSubmit();
});