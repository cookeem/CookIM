/**
 * Created by cookeem on 16/9/27.
 */

var app = angular.module('app', ['ngRoute', 'ngAnimate']);

app.config(function($routeProvider, $locationProvider) {
    $routeProvider
        .when('/error', {
            templateUrl: 'error.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/login', {
            templateUrl: 'login.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/register', {
            templateUrl: 'register.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/chatlist', {
            templateUrl: 'chatlist.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/chatsession', {
            templateUrl: 'chatsession.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/changepwd', {
            templateUrl: 'changepwd.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/changeinfo', {
            templateUrl: 'changeinfo.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/logout', {
            templateUrl: 'logout.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .when('/mgmuser-view/:querystring', {
            templateUrl: 'mgmuser-view.html',
            controller: 'contentCtl',
            animation: 'animation-slideleft'
        })
        .otherwise({redirectTo: '/chatlist'});
    //使用#!作为路由前缀
    $locationProvider.html5Mode(false).hashPrefix('!');
});

app.controller('headerCtl', function($rootScope) {
    //sideNav菜单项初始化
    $rootScope.showSideBar = false;
    //关闭底部footer
    $rootScope.showMessageArea = false;

    $rootScope.menuItems = [
        {
            "menuName": "Error",
            "url": "/#!/error"
        },
        {
            "menuName": "Login",
            "url": "/#!/login"
        },
        {
            "menuName": "Register",
            "url": "/#!/register"
        },
        {
            "menuName": "ChatList",
            "url": "/#!/chatlist"
        },
        {
            "menuName": "ChatSession",
            "url": "/#!/chatsession"
        },
    ];

    $rootScope.accountMenuItems = [
        {
            "menuName": "ChangePwd",
            "url": "/#!/changepwd"
        },
        {
            "menuName": "ChangeInfo",
            "url": "/#!/changeinfo"
        },
        {
            "menuName": "Logout",
            "url": "/#!/logout"
        },
    ];

    //sideNav初始化
    var sidebarWidth = 240;
    if ($(window).width() > 992) {
        $('.button-collapse').sideNav({
                'menuWidth': sidebarWidth, // Default is 240
                'edge': 'left', // Choose the horizontal origin
                'closeOnClick': false // Closes side-nav on <a> clicks, useful for Angular/Meteor
            }
        );
    } else {
        $('.button-collapse').sideNav({
                'menuWidth': sidebarWidth, // Default is 240
                'edge': 'left', // Choose the horizontal origin
                'closeOnClick': true // Closes side-nav on <a> clicks, useful for Angular/Meteor
            }
        );
    }
});

app.controller('contentCtl', function($rootScope, $scope, $route, $routeParams, $http) {
    $rootScope.params = $routeParams;

    $rootScope.$on('$routeChangeStart', function(event, currRoute, prevRoute){
        $rootScope.animation = currRoute.animation;
        $('html, body').animate({scrollTop:0}, 0);
        $rootScope.isLoading = true;
    });
    $rootScope.$on('$routeChangeSuccess', function() {
        $rootScope.isLoading = false;
    });
});

app.filter('trustHtml', function ($sce) {
    return function (input) {
        return $sce.trustAsHtml(input);
    }
});

function showHideSideBar(isShow) {
    if (isShow) {
        if ($(window).width() > 992) {
            $('header, main, footer').css('padding-left', '240px');
        } else {
            $('header, main, footer').css('padding-left', '0');
        }
    } else {
        $('header, main, footer').css('padding-left', '0');
    }
}
