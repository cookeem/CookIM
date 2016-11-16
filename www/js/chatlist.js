/**
 * Created by cookeem on 16/6/3.
 */
app.controller('chatListAppCtl', function($rootScope, $timeout) {
    //Hide sidebar when init
    $rootScope.showNavbar = true;
    //Hide footer when init
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
    }, 0);
});