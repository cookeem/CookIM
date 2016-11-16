/**
 * Created by cookeem on 16/6/3.
 */
app.controller('changeInfoAppCtl', function($rootScope, $scope, $cookies, $timeout, $http) {
    //Hide sidebar when init
    $rootScope.showNavbar = true;
    //Hide footer when init
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
    }, 0);

    var cookie_userToken = "";
    var cookie_uid = "";
    if ($cookies.get('userToken')) {
        cookie_userToken = $cookies.get('userToken');
    } else {
        Materialize.toast('please login first', 4000);
        window.location.href = '#!/login';
    }
    if ($cookies.get('uid')) {
        cookie_uid = $cookies.get('uid');
    }

    $scope.getUserInfoData = {
        "uid": cookie_uid,
        "userToken": cookie_userToken
    };

    $scope.changeUserInfoData = {
        "nickname": "",
        "gender": 0,
        "avatar": "",
        "userToken": cookie_userToken
    };
    var avatarInput = $('#avatarInput');

    $scope.getUserInfoSubmit = function() {
        $http({
            method  : 'POST',
            url     : '/api/getUserInfo',
            data    : $.param($scope.getUserInfoData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 4000);
            } else {
                $rootScope.successmsg = response.data.successmsg;
                $scope.changeUserInfoData.nickname = response.data.userInfo.nickname;
                $scope.changeUserInfoData.avatar = response.data.userInfo.avatar;
                $scope.changeUserInfoData.gender = response.data.userInfo.gender;
                $('label').addClass('active');
                console.log($scope.changeUserInfoData);
                Materialize.toast($rootScope.successmsg, 4000);
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    };
    $scope.getUserInfoSubmit();

    $scope.changeUserInfoSubmit = function() {
        if (avatarInput.files) {
            $scope.changeUserInfoData.avatar = avatarInput.files[0];
        }
        $http({
            method  : 'POST',
            url     : '/api/updateUser',
            data    : $.param($scope.changeUserInfoData),
            headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 4000);
            } else {
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 4000);
            }
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    };

});

var showImage = function(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function (e) {
            $('#avatarImage').attr('src', e.target.result);
        };
        reader.readAsDataURL(input.files[0]);
    }
};

