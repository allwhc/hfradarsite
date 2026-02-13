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
        document.getElementById("attendance").className = "list-group-item menutab";
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

        if(tabNum == 14)
        {
            document.getElementById("attendance").className = "list-group-item active";
            // Load sites list for filter dropdown and attendance records
            $scope.dbjsonpost("admin/getSites","{}");
            $scope.dbjsonpost("admin/getAttendance", JSON.stringify({site_code: ""}));
            $scope.dbjsonpost("admin/getSitesWithCoords", "{}");
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

            // Attendance responses
            if(typ == "adminGetAttendance")
            {
                $scope.attendanceRecords = resp.records || [];
            }

            if(typ == "adminGetSitesWithCoords")
            {
                $scope.sitesWithCoords = resp.sites || [];
            }

            if(typ == "adminUpdateSiteCoords")
            {
                var coordsMsgEl = document.getElementById("coordsMsg");
                if(coordsMsgEl) {
                    if(resp.stat == "success") {
                        coordsMsgEl.innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                    } else {
                        coordsMsgEl.innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                    }
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

    // Update from GitHub (fully automatic - git pull + auto reload)
    $scope.updateFromGithub = function() {
        if(!confirm("This will update your code from GitHub and reload the web app.\n\nYour database will be automatically preserved.\n\nContinue?")) {
            return;
        }

        document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:blue;">Step 1/2: Pulling latest code from GitHub (preserving database)...</span>';

        var cfig = {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 60000  // 60 second timeout for git pull
        };

        // Step 1: Pull from GitHub (database is automatically preserved via git stash)
        $http.post("admin/pullFromGithub", "{}", cfig).then(
            function(gitResponse) {
                var gitResp = gitResponse.data;
                if(gitResp.stat == "success") {
                    // Step 2: Auto-reload the web app (this can take 15-30 seconds)
                    var msg = '<div style="color:blue; font-weight:bold;">Step 2/2: Reloading web app...</div>';
                    msg += '<div style="margin-top:10px; color:#666;"><span class="loading-dots">Please wait, this may take up to 30 seconds</span></div>';
                    document.getElementById("githubUpdateMsg").innerHTML = msg;

                    // Configure with longer timeout for reload
                    var reloadCfig = {
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        timeout: 45000  // 45 second timeout for reload
                    };

                    $http.post("admin/reloadWebApp", "{}", reloadCfig).then(
                        function(reloadResponse) {
                            var reloadResp = reloadResponse.data;
                            if(reloadResp.stat == "success") {
                                // Success! Reload initiated
                                var msg = '<div style="color:green; font-weight:bold; font-size:18px;">&#9989; Update Complete!</div>';
                                msg += '<div style="margin-top:15px; padding:15px; background-color:#d4edda; border:2px solid #28a745; border-radius:5px;">';
                                msg += '<strong style="color:#155724; font-size:16px;">&#10004; All Done!</strong><br>';
                                msg += '<ul style="color:#155724; margin-top:10px; text-align:left;">';
                                msg += '<li>✅ Code updated from GitHub</li>';
                                msg += '<li>✅ Database preserved (no changes)</li>';
                                msg += '<li>✅ Web app reload initiated</li>';
                                msg += '</ul>';
                                msg += '<p style="color:#155724; margin-top:15px;"><strong>⏳ Please wait 15-30 seconds for reload to complete, then refresh this page.</strong></p>';
                                msg += '</div>';
                                if(gitResp.git_output) {
                                    msg += '<div style="margin-top:10px; font-size:12px; color:#666;">Git output: ' + gitResp.git_output.substring(0, 200) + '</div>';
                                }
                                document.getElementById("githubUpdateMsg").innerHTML = msg;
                            } else {
                                // Reload failed - show manual reload button
                                $scope.showManualReloadOption(reloadResp.msg);
                            }
                        },
                        function(error) {
                            // Reload request failed or timed out - show manual reload button
                            var errorMsg = error.status === -1 ? 'Request timed out after 45 seconds' : 'Connection error';
                            $scope.showManualReloadOption(errorMsg);
                        }
                    );
                } else {
                    document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:red;">Error pulling from GitHub: ' + gitResp.msg + '</span>';
                }
            },
            function(error) {
                document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:red;">Error communicating with server during git pull.</span>';
                console.log(error);
            }
        );
    }

    // Show manual reload option with button
    $scope.showManualReloadOption = function(errorMsg) {
        var msg = '<div style="color:orange; font-weight:bold;">⚠ Code Updated, Manual Reload Available</div>';
        msg += '<div style="margin-top:10px;">✅ Code pulled from GitHub successfully</div>';
        msg += '<div style="margin-top:10px;">✅ Database preserved</div>';
        msg += '<div style="margin-top:15px; padding:15px; background-color:#fff3cd; border:2px solid #ffc107; border-radius:5px;">';
        msg += '<strong style="color:#856404;">⚠ Auto-reload issue:</strong><br>';
        msg += '<span style="color:#856404;">' + errorMsg + '</span><br><br>';
        msg += '<p style="color:#856404; margin-bottom:15px;">Choose an option below:</p>';
        msg += '<button class="btn btn-warning btn-lg" onclick="angular.element(this).scope().manualReloadWebApp()" style="margin-right:10px;"><span style="font-size:16px;">&#128259;</span> Try Reload Again</button>';
        msg += '<p style="color:#666; margin-top:15px; font-size:13px;">Or manually reload from <a href="https://www.pythonanywhere.com/user/hfradarsite/webapps/" target="_blank">PythonAnywhere Web tab</a></p>';
        msg += '</div>';
        document.getElementById("githubUpdateMsg").innerHTML = msg;
    }

    // Manual reload function (called by button)
    $scope.manualReloadWebApp = function() {
        document.getElementById("githubUpdateMsg").innerHTML = '<span style="color:blue;">Attempting to reload web app... Please wait up to 45 seconds...</span>';

        var cfig = {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 45000
        };

        $http.post("admin/reloadWebApp", "{}", cfig).then(
            function(response) {
                var resp = response.data;
                if(resp.stat == "success") {
                    var msg = '<div style="color:green; font-weight:bold; font-size:18px;">&#9989; Reload Successful!</div>';
                    msg += '<div style="margin-top:15px; padding:15px; background-color:#d4edda; border:2px solid #28a745; border-radius:5px;">';
                    msg += '<strong style="color:#155724; font-size:16px;">&#10004; Web app reloaded!</strong><br>';
                    msg += '<p style="color:#155724; margin-top:10px;"><strong>Refresh this page</strong> to see the changes.</p>';
                    msg += '</div>';
                    document.getElementById("githubUpdateMsg").innerHTML = msg;
                } else {
                    $scope.showManualReloadOption(resp.msg);
                }
            },
            function(error) {
                var errorMsg = error.status === -1 ? 'Request timed out. The reload may still be in progress.' : 'Connection error: ' + error.statusText;
                $scope.showManualReloadOption(errorMsg);
            }
        );
    }

})

// Attendance Controller
amyApp.controller('AttendanceCtrl', function ($scope, $http) {
    $scope.selectedSite = "";
    $scope.attendanceRecords = [];
    $scope.sitesWithCoords = [];

    // Load attendance records
    $scope.loadAttendance = function() {
        var cfig = {
            headers: { 'Content-Type': 'application/json' }
        };
        $http.post("admin/getAttendance", JSON.stringify({site_code: $scope.selectedSite}), cfig).then(
            function(response) {
                $scope.attendanceRecords = response.data.records || [];
            },
            function(error) {
                console.log("Error loading attendance:", error);
            }
        );
    }

    // Load sites with coordinates directly
    $scope.loadSitesWithCoords = function() {
        var cfig = {
            headers: { 'Content-Type': 'application/json' }
        };
        $http.post("admin/getSitesWithCoords", "{}", cfig).then(
            function(response) {
                $scope.sitesWithCoords = response.data.sites || [];
            },
            function(error) {
                console.log("Error loading sites with coords:", error);
            }
        );
    }

    // Save site coordinates
    $scope.saveSiteCoords = function(site) {
        var cfig = {
            headers: { 'Content-Type': 'application/json' }
        };
        var data = {
            site_code: site.code,
            site_lat: site.lat,
            site_lng: site.lng,
            technician: site.technician || ''
        };
        $http.post("admin/updateSiteCoords", JSON.stringify(data), cfig).then(
            function(response) {
                var resp = response.data;
                var coordsMsgEl = document.getElementById("coordsMsg");
                if(resp.stat == "success") {
                    coordsMsgEl.innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                } else {
                    coordsMsgEl.innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            },
            function(error) {
                document.getElementById("coordsMsg").innerHTML = '<span style="color:red;">Error saving coordinates.</span>';
            }
        );
    }

    // Auto-load on init
    $scope.loadAttendance();
    $scope.loadSitesWithCoords();
})
