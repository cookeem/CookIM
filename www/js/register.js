/**
 * Created by cookeem on 16/6/3.
 */
app.controller('registerAppCtl', function($rootScope, $timeout) {
    //关闭左侧导航栏
    $rootScope.showNavbar = true;
    //关闭底部footer
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
    }, 0);
});