/**
 * Created by cookeem on 16/6/3.
 */
app.controller('chatListAppCtl', function($rootScope, $scope, $cookies, $timeout, $routeParams, $http) {
    //Hide sidebar when init
    $rootScope.showNavbar = true;
    //Hide footer when init
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
    }, 0);

    $rootScope.verifyUserToken();

    //refresh userToken on websocket
    if (!$rootScope.websocketUserToken) {
        $rootScope.listenWebsocketUserToken();
    }

    $scope.listSessionsData = {
        "isPublic": 0,
        "showType": 0,
        "page": 1,
        "count": 10,
        "userToken": $rootScope.userToken
    };
    $scope.querystring = $routeParams.querystring;
    if ($scope.querystring != "public") {
        $scope.listSessionsData.isPublic = 0;
    } else {
        $scope.listSessionsData.isPublic = 1;
    }
    $scope.listSessionsResults = [];

    $scope.listSessionsSubmit = function() {
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
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 4000);
                $scope.listSessionsResults = response.data.sessions;
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    };

    $scope.listSessionsSubmit();

});