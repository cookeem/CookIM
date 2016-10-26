/**
 * Created by cookeem on 16/6/2.
 */
app.controller('errorAppCtl', function($rootScope, $timeout) {
    //关闭左侧导航栏
    $rootScope.showNavbar = false;
    //关闭底部footer
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
    }, 0);
});