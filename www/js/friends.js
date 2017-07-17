/**
 * Created by cookeem on 16/6/2.
 */
app.controller('friendsAppCtl', function($rootScope, $scope, $http, $timeout) {
    $rootScope.showSideNavbar = true;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = true;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - Friends",
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
        $('.tooltipped').tooltip({delay: 50});
    }, 0);

    $rootScope.getUserTokenRepeat();
    $scope.searchResult = [];
    $rootScope.getFriendsByNickname = function(nickName) {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken,
            "nickName" : nickName
        };
        $http({
            method  : 'POST',
            url     : '/api/getUserInfoByName',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                window.location.href = '#!/error';
            } else {
                $scope.searchResult = response.data.userInfo;
                console.log($scope.searchResult);
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };
    $scope.friends = [];
    $scope.getFriendsSubmit = function() {
        $rootScope.verifyUserToken();
        var postData = {
            "userToken": $rootScope.userToken
        };
        $http({
            method  : 'POST',
            url     : '/api/getFriends',
            data    : $.param(postData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                window.location.href = '#!/error';
            } else {
                $scope.friends = response.data.friends;
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };
    $scope.getFriendsSubmit();
});