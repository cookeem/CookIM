/**
 * Created by cookeem on 16/6/2.
 */
app.controller('notificationsAppCtl', function($rootScope, $timeout, $scope, $http) {
    $rootScope.showSideNavbar = true;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = true;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - Notifications",
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

    $rootScope.getUserTokenRepeat();

    $scope.notifications = [];
    $scope.getNotificationsSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/getNotifications',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                window.location.href = '#!/error';
            } else {
                $scope.notifications = response.data.notifications;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };
    $scope.getNotificationsSubmit();

});