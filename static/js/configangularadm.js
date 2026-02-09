var amyApp = angular.module('admtabApp', []);

amyApp.controller('aTabController', function ($scope, $http) {
    $scope.credentries = [];
    $scope.sitesList = [];
    $scope.sitePairs = [];
    $scope.ungroupedSites = [];
    $scope.tab1 = 8;

    $scope.setTab = function (newTab) {
	    $scope.tab1 = newTab;

	    tabNum = newTab;
	    document.getElementById("ucred").className = "list-group-item menutab";
        document.getElementById("acred").className = "list-group-item menutab";
        document.getElementById("lcred").className = "list-group-item menutab";
        document.getElementById("mcred").className = "list-group-item menutab";
        document.getElementById("sitesmgmt").className = "list-group-item menutab";
        document.getElementById("dbbackup").className = "list-group-item menutab";
        document.getElementById("lgout").className = "list-group-item menutab";

        if(tabNum == 8)
        {
            document.getElementById("ucred").className = "list-group-item active";
            document.getElementById("msgcredentials").innerHTML = "";
            document.forms["Credentialsform"]["username"].value = "";
            document.forms["Credentialsform"]["password"].value = "";
        }

        if(tabNum == 9)
        {
            document.getElementById("acred").className = "list-group-item active";
            document.forms["AdCredentialsform"]["username"].value = "";
            document.forms["AdCredentialsform"]["password"].value = "";
            document.getElementById("amsgcredentials").innerHTML = "";
        }

        if(tabNum == 10)
        {
            document.getElementById("lcred").className = "list-group-item active";
            $scope.dbjsonpost("lstcredentials","{}");
        }

        if(tabNum == 11)
        {
            document.getElementById("mcred").className = "list-group-item active";
            $scope.dbjsonpost("getactivationstat","{}");
        }

        if(tabNum == 12)
        {
            document.getElementById("sitesmgmt").className = "list-group-item active";
            $scope.dbjsonpost("admin/getSites","{}");
            $scope.dbjsonpost("admin/getSitePairs","{}");
        }

        if(tabNum == 13)
        {
            document.getElementById("dbbackup").className = "list-group-item active";
            document.getElementById("downloadMsg").innerHTML = "";
            document.getElementById("uploadMsg").innerHTML = "";
            document.getElementById("githubUpdateMsg").innerHTML = "";
        }
    }

	$scope.isSet = function (tabNum) {
		return $scope.tab1 == tabNum;
	}

    $scope.dbjsonpost = function(starget,data) {
		var cfig = {
                headers : {
                    'Content-Type': 'application/json'
                }
            }
        var responsePromise = $http.post(starget,data, cfig);
		responsePromise.success(function(response, status, headers, config){
			console.log("success post");
			var resp = response;
            var typ=resp.sel;
            if(typ == "cred")
            {
                var utype=resp.utype;
                if(utype == "user")
                    document.getElementById("msgcredentials").innerHTML = "User credentials are set successfully";
                else if(utype == "admin")
                    document.getElementById("amsgcredentials").innerHTML = "Admin credentials are set successfully";
            }

            if(typ == "lstcred")
            {
                $scope.credentries = resp.credentries;
            }

            if(typ == "siteactivate")
            {
                document.getElementById("mmsg").innerHTML = "Hf Radar website is activated!";
                document.getElementById("mmsg1").innerHTML = "Current Activation Status: Active";
            }

            if(typ == "sitedeactivate")
            {
                document.getElementById("mmsg").innerHTML = "Hf Radar website is deactivated!";
                document.getElementById("mmsg1").innerHTML = "Current Activation Status: Non-Active";
            }

            if(typ == "getactivationstat")
            {
                document.getElementById("mmsg").innerHTML = "&nbsp;";
                document.getElementById("mmsg1").innerHTML = resp.stat;
            }

            // Sites List Management responses
            if(typ == "adminGetSites")
            {
                $scope.sitesList = resp.sites;
            }

            if(typ == "adminAddSite")
            {
                if(resp.stat == "success") {
                    document.getElementById("addSiteMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    document.getElementById("newSiteCode").value = "";
                    // Refresh sites list
                    $scope.dbjsonpost("admin/getSites","{}");
                } else {
                    document.getElementById("addSiteMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            if(typ == "adminToggleSite")
            {
                if(resp.stat == "success") {
                    document.getElementById("sitesListMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    // Refresh sites list
                    $scope.dbjsonpost("admin/getSites","{}");
                } else {
                    document.getElementById("sitesListMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            if(typ == "adminEditSite")
            {
                if(resp.stat == "success") {
                    document.getElementById("sitesListMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    // Refresh sites list
                    $scope.dbjsonpost("admin/getSites","{}");
                } else {
                    document.getElementById("sitesListMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            // Site Pairs responses
            if(typ == "adminGetSitePairs")
            {
                $scope.sitePairs = resp.pairs;
                $scope.ungroupedSites = resp.ungrouped || [];
            }

            if(typ == "adminAddSitePair")
            {
                if(resp.stat == "success") {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    // Refresh pairs list
                    $scope.dbjsonpost("admin/getSitePairs","{}");
                } else {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            if(typ == "adminAddNewGroup")
            {
                if(resp.stat == "success") {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    // Refresh pairs list
                    $scope.dbjsonpost("admin/getSitePairs","{}");
                } else {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            if(typ == "adminRemoveSitePair")
            {
                if(resp.stat == "success") {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    // Refresh pairs list
                    $scope.dbjsonpost("admin/getSitePairs","{}");
                } else {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            if(typ == "adminDeleteGroup")
            {
                if(resp.stat == "success") {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    // Refresh pairs list
                    $scope.dbjsonpost("admin/getSitePairs","{}");
                } else {
                    document.getElementById("pairsMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

		});
		responsePromise.error(function(err, status, headers, config){
            console.log(err);
        });
	}
})


amyApp.controller('CredTabsCtrl',function ($scope, $http, $compile) {

    $scope.clrusr = function() {
        document.getElementById("msgcredentials").innerHTML = "";
        document.forms["Credentialsform"]["username"].value = "";
        document.forms["Credentialsform"]["password"].value = "";
    }

    $scope.changecred2 = function() {

        var xa=document.forms["Credentialsform"]["username"].value;
        var x0=document.forms["Credentialsform"]["password"].value;
        var utype=document.forms["Credentialsform"]["utype"].value;
        if(xa == "")
        {
            document.getElementById("cuIdMessage1").innerHTML = "Please enter username";
        }
        else if(x0 == "")
        {
            document.getElementById("cuIdMessage1").innerHTML = "";
            document.getElementById("cuIdMessage2").innerHTML = "Please enter password";
        }
        else
        {
            document.getElementById("cuIdMessage1").innerHTML = "";
            document.getElementById("cuIdMessage2").innerHTML = "";
            document.getElementById("msgcredentials").innerHTML = "";
            var apistr = '{"username":"'+xa+'","password":"'+x0+'","utype":"'+utype+'"}';
            var apijsonObj = JSON.parse(apistr);
            $scope.$parent.dbjsonpost("credentials",JSON.stringify(apijsonObj));
        }
    }

})

amyApp.controller('AdCredTabsCtrl',function ($scope, $http, $compile) {

    $scope.clradm = function() {
        document.forms["AdCredentialsform"]["username"].value = "";
        document.forms["AdCredentialsform"]["password"].value = "";
        document.getElementById("amsgcredentials").innerHTML = "";
    }

    $scope.achangecred2 = function() {

        var xa=document.forms["AdCredentialsform"]["username"].value;
        var x0=document.forms["AdCredentialsform"]["password"].value;
        var utype=document.forms["AdCredentialsform"]["utype"].value;
        if(xa == "")
        {
            document.getElementById("auIdMessage1").innerHTML = "Please enter username";
        }
        else if(x0 == "")
        {
            document.getElementById("auIdMessage1").innerHTML = "";
            document.getElementById("auIdMessage2").innerHTML = "Please enter password";
        }
        else
        {
            document.getElementById("auIdMessage1").innerHTML = "";
            document.getElementById("auIdMessage2").innerHTML = "";
            document.getElementById("amsgcredentials").innerHTML = "";
            var apistr = '{"username":"'+xa+'","password":"'+x0+'","utype":"'+utype+'"}';
            var apijsonObj = JSON.parse(apistr);
            $scope.$parent.dbjsonpost("credentials",JSON.stringify(apijsonObj));
        }
    }

})

amyApp.controller('LstCredTabsCtrl',function ($scope, $http, $compile) {
    $scope.cred = "";

})

amyApp.controller('LstMngTabsCtrl',function ($scope, $http, $compile) {
    $scope.credm = "";

    $scope.siteactivate = function() {
        $scope.$parent.dbjsonpost("siteactivate","{}");
    }

    $scope.sitedeactivate = function() {
        $scope.$parent.dbjsonpost("sitedeactivate","{}");
    }

})

// Sites List Management Controller
amyApp.controller('SitesListCtrl',function ($scope, $http, $compile) {
    // sitesList is defined in parent controller (aTabController)

    // Add new site
    $scope.addSite = function() {
        var siteCode = document.getElementById("newSiteCode").value.trim();
        if(siteCode == "") {
            document.getElementById("addSiteMsg").innerHTML = '<span style="color:red;">Please enter a site code</span>';
            return;
        }

        var apistr = '{"site_code":"' + siteCode + '"}';
        var apijsonObj = JSON.parse(apistr);
        $scope.$parent.dbjsonpost("admin/addSite", JSON.stringify(apijsonObj));
    }

    // Toggle site active/inactive
    $scope.toggleSite = function(siteCode, isActive) {
        var apistr = '{"site_code":"' + siteCode + '", "is_active":' + isActive + '}';
        var apijsonObj = JSON.parse(apistr);
        $scope.$parent.dbjsonpost("admin/toggleSite", JSON.stringify(apijsonObj));
    }

    // Start editing a site
    $scope.startEditSite = function(site) {
        site.editing = true;
        site.newCode = site.code;
    }

    // Cancel editing
    $scope.cancelEditSite = function(site) {
        site.editing = false;
        site.newCode = "";
    }

    // Save site edit (update site code)
    $scope.saveSiteEdit = function(site) {
        var newCode = site.newCode.trim();
        if(newCode == "") {
            document.getElementById("sitesListMsg").innerHTML = '<span style="color:red;">Site code cannot be empty</span>';
            return;
        }
        if(newCode == site.code) {
            site.editing = false;
            return;
        }

        var apistr = '{"old_code":"' + site.code + '", "new_code":"' + newCode + '"}';
        var apijsonObj = JSON.parse(apistr);
        $scope.$parent.dbjsonpost("admin/editSite", JSON.stringify(apijsonObj));
        site.editing = false;
    }

    // Site Pairs/Groups functions

    // Add a new auto-named group
    $scope.addNewGroup = function() {
        $scope.$parent.dbjsonpost("admin/addNewGroup", "{}");
    }

    // Add a site to a specific group (from per-group dropdown)
    $scope.addSiteToGroup = function(groupName) {
        var selectEl = document.getElementById("addSiteTo_" + groupName);
        if(!selectEl) return;
        var siteCode = selectEl.value;
        if(siteCode == "") {
            document.getElementById("pairsMsg").innerHTML = '<span style="color:red;">Please select a site to add</span>';
            return;
        }
        var apistr = '{"group_name":"' + groupName + '", "site_code":"' + siteCode + '"}';
        var apijsonObj = JSON.parse(apistr);
        $scope.$parent.dbjsonpost("admin/addSitePair", JSON.stringify(apijsonObj));
    }

    $scope.removeSitePair = function(groupName, siteCode) {
        if(confirm("Remove site '" + siteCode + "' from group '" + groupName + "'?")) {
            var apistr = '{"group_name":"' + groupName + '", "site_code":"' + siteCode + '"}';
            var apijsonObj = JSON.parse(apistr);
            $scope.$parent.dbjsonpost("admin/removeSitePair", JSON.stringify(apijsonObj));
        }
    }

    $scope.deleteGroup = function(groupName) {
        if(confirm("Delete entire group '" + groupName + "' and all its site associations?")) {
            var apistr = '{"group_name":"' + groupName + '"}';
            var apijsonObj = JSON.parse(apistr);
            $scope.$parent.dbjsonpost("admin/deleteGroup", JSON.stringify(apijsonObj));
        }
    }

})

// Database Backup Controller
amyApp.controller('DbBackupCtrl', function ($scope, $http) {

    // Download backup
    $scope.downloadBackup = function() {
        document.getElementById("downloadMsg").innerHTML = '<span style="color:blue;">Preparing backup... Please wait.</span>';

        var cfig = {
            headers: {
                'Content-Type': 'application/json'
            }
        };

        $http.post("admin/downloadBackup", "{}", cfig).then(
            function(response) {
                var resp = response.data;
                if(resp.stat == "success") {
                    // Create downloadable file
                    var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(resp.backup, null, 2));
                    var downloadAnchorNode = document.createElement('a');
                    downloadAnchorNode.setAttribute("href", dataStr);
                    downloadAnchorNode.setAttribute("download", "hfradar_backup_" + resp.timestamp + ".json");
                    document.body.appendChild(downloadAnchorNode);
                    downloadAnchorNode.click();
                    downloadAnchorNode.remove();
                    document.getElementById("downloadMsg").innerHTML = '<span style="color:green;">Backup downloaded successfully! File: hfradar_backup_' + resp.timestamp + '.json</span>';
                } else {
                    document.getElementById("downloadMsg").innerHTML = '<span style="color:red;">Error: ' + resp.msg + '</span>';
                }
            },
            function(error) {
                document.getElementById("downloadMsg").innerHTML = '<span style="color:red;">Error downloading backup. Please try again.</span>';
                console.log(error);
            }
        );
    }

    // Upload/Restore backup
    $scope.uploadBackup = function() {
        var fileInput = document.getElementById("backupFile");
        if(!fileInput.files || fileInput.files.length == 0) {
            document.getElementById("uploadMsg").innerHTML = '<span style="color:red;">Please select a backup file first.</span>';
            return;
        }

        if(!confirm("Are you sure you want to restore from this backup? This will overwrite existing data.")) {
            return;
        }

        var file = fileInput.files[0];
        var reader = new FileReader();

        reader.onload = function(e) {
            try {
                var backupData = JSON.parse(e.target.result);

                document.getElementById("uploadMsg").innerHTML = '<span style="color:blue;">Restoring backup... Please wait.</span>';

                var cfig = {
                    headers: {
                        'Content-Type': 'application/json'
                    }
                };

                $http.post("admin/restoreBackup", JSON.stringify({backup: backupData}), cfig).then(
                    function(response) {
                        var resp = response.data;
                        if(resp.stat == "success") {
                            document.getElementById("uploadMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                            fileInput.value = "";
                        } else {
                            document.getElementById("uploadMsg").innerHTML = '<span style="color:red;">Error: ' + resp.msg + '</span>';
                        }
                    },
                    function(error) {
                        document.getElementById("uploadMsg").innerHTML = '<span style="color:red;">Error restoring backup. Please try again.</span>';
                        console.log(error);
                    }
                );
            } catch(err) {
                document.getElementById("uploadMsg").innerHTML = '<span style="color:red;">Invalid backup file format. Please select a valid JSON backup file.</span>';
            }
        };

        reader.readAsText(file);
    }

    // Update from GitHub (auto-download backup first, then git pull)
    $scope.updateFromGithub = function() {
        if(!confirm("This will update your code from GitHub.\n\n1. Database backup will be auto-downloaded\n2. Latest code will be pulled from GitHub\n3. You must reload the web app from PythonAnywhere\n\nContinue?")) {
            return;
        }

        document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:blue;">Step 1/2: Downloading database backup...</span>';

        var cfig = {
            headers: {
                'Content-Type': 'application/json'
            }
        };

        // Step 1: Download backup first
        $http.post("admin/downloadBackup", "{}", cfig).then(
            function(response) {
                var resp = response.data;
                if(resp.stat == "success") {
                    // Auto-download the backup file
                    var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(resp.backup, null, 2));
                    var downloadAnchorNode = document.createElement('a');
                    downloadAnchorNode.setAttribute("href", dataStr);
                    downloadAnchorNode.setAttribute("download", "hfradar_backup_" + resp.timestamp + ".json");
                    document.body.appendChild(downloadAnchorNode);
                    downloadAnchorNode.click();
                    downloadAnchorNode.remove();

                    // Step 2: Now pull from GitHub
                    document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:blue;">Step 2/2: Pulling latest code from GitHub...</span>';

                    $http.post("admin/pullFromGithub", "{}", cfig).then(
                        function(gitResponse) {
                            var gitResp = gitResponse.data;
                            if(gitResp.stat == "success") {
                                var msg = '<div style="color:green; font-weight:bold;">&#10004; Update Successful!</div>';
                                msg += '<div style="margin-top:10px;">&#128190; Database backup saved: hfradar_backup_' + resp.timestamp + '.json</div>';
                                msg += '<div style="margin-top:5px;">&#128640; Code updated from GitHub</div>';
                                msg += '<div style="margin-top:15px; padding:10px; background-color:#fff3cd; border:1px solid #ffc107; border-radius:5px;">';
                                msg += '<strong style="color:#856404;">&#9888; IMPORTANT - Next Step:</strong><br>';
                                msg += '<span style="color:#856404;">Go to PythonAnywhere Web tab and click the <strong>"Reload"</strong> button to apply changes!</span>';
                                msg += '</div>';
                                if(gitResp.git_output) {
                                    msg += '<div style="margin-top:10px; font-size:12px; color:#666;">Git output: ' + gitResp.git_output.substring(0, 200) + '</div>';
                                }
                                document.getElementById("githubUpdateMsg").innerHTML = msg;
                            } else {
                                document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:red;">Error pulling from GitHub: ' + gitResp.msg + '</span>';
                            }
                        },
                        function(error) {
                            document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:red;">Error communicating with server during git pull.</span>';
                            console.log(error);
                        }
                    );
                } else {
                    document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:red;">Error creating backup before update: ' + resp.msg + '</span>';
                }
            },
            function(error) {
                document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:red;">Error downloading backup. Update cancelled.</span>';
                console.log(error);
            }
        );
    }

})
