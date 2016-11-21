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

    $rootScope.verifyUserToken();

    $scope.getUserInfoData = {
        "uid": $rootScope.uid,
        "userToken": $rootScope.userToken
    };

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
                window.location.href = '#!/error';
            } else {
                $rootScope.successmsg = response.data.successmsg;
                $scope.changeUserInfoData.nickname = response.data.userInfo.nickname;
                $scope.changeUserInfoData.avatar = response.data.userInfo.avatar;
                $scope.changeUserInfoData.gender = response.data.userInfo.gender;
                $('label').addClass('active');
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
        });
    };

    if ($rootScope.errmsg == '') {
        $scope.getUserInfoSubmit();
    }

    $scope.changeUserInfoData = {
        "nickname": "",
        "gender": 0,
        "avatar": ""
    };

    $scope.changeUserInfoSubmit = function() {
        var formData = new FormData();
        formData.append("nickname", $scope.changeUserInfoData.nickname);
        formData.append("gender", $scope.changeUserInfoData.gender);
        formData.append("userToken", $rootScope.userToken);
        var avatarInput = $('#avatarInput')[0];
        if (avatarInput.files && avatarInput.files[0]) {
            formData.append("avatar", avatarInput.files[0]);
        }
        $http({
            method  : 'POST',
            url     : '/api/updateUser',
            // mix file upload and form multipart
            data    : formData,
            transformRequest: angular.identity,
            headers : { 'Content-Type': undefined }
        }).then(function successCallback(response) {
            console.log(response.data);
            if (response.data.errmsg) {
                $rootScope.errmsg = response.data.errmsg;
                Materialize.toast("error: " + $rootScope.errmsg, 3000);
            } else {
                $rootScope.successmsg = response.data.successmsg;
                Materialize.toast($rootScope.successmsg, 3000);
                $scope.getUserInfoSubmit();
            }
        }, function errorCallback(response) {
            console.error("http request error:" + response.data);
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

