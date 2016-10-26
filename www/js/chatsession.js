/**
 * Created by cookeem on 16/6/3.
 */
app.controller('chatSessionAppCtl', function($rootScope, $timeout) {
    //关闭左侧导航栏
    $rootScope.showNavbar = true;
    //关闭底部footer
    $rootScope.showMessageArea = true;
    $timeout(function() {
        showHideSideBar($rootScope.showNavbar);
        $('.dropdown-button').dropdown({
                inDuration: 300,
                outDuration: 225,
                constrain_width: false, // Does not change width of dropdown to that of the activator
                hover: true, // Activate on hover
                gutter: 0, // Spacing from edge
                belowOrigin: false, // Displays dropdown below the button
                alignment: 'left' // Displays dropdown with edge aligned to the left of button
            }
        );
    }, 0);
});