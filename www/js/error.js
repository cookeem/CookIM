/**
 * Created by cookeem on 16/6/2.
 */
app.controller('errorAppCtl', function($rootScope, $timeout) {
    $rootScope.showSideNavbar = false;
    $rootScope.showMessageArea = false;
    $rootScope.showAccoutMenu = false;
    $rootScope.titleInfo = {
        //private_session, group_session, other
        mode: "other",
        //title text
        title: "CookIM - Error encounter",
        //title icon
        icon: "images/favicon.ico",
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
});