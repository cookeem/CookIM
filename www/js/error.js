/**
 * Created by cookeem on 16/6/2.
 */
app.controller('errorAppCtl', function($rootScope, $timeout) {
    //Hide sidebar when init
    $rootScope.showNavbar = false;
    //Hide footer when init
    $rootScope.showMessageArea = false;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
    }, 0);
});