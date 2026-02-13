var myApp = angular.module('tabApp', []);


myApp.controller('TabController', function ($scope, $http, $timeout, $window) {

    $scope.hfdat = [];
    $scope.hfids = [];
    $scope.iv = 0;
    $scope.iv1 = 0;
    $scope.iv2 = 2;
    $scope.rcarr = [];
    $scope.icarr = [];
    $scope.rcdata = [];
    $scope.rcperc = [];
    $scope.lstids = [];
    $scope.hfyr = ""
    $scope.hfmon = ""
    $scope.hfrows = "";
    $scope.hfrowflg = "1";
    $scope.entries1 = [];
    $scope.totent = [];
    $scope.crmon = "";
    $scope.cryr = "";
    $scope.pryr = "";
    $scope.rng = "";
    $scope.titl = [];
    $scope.svstr="";
    $scope.sdict = {};
    $scope.tab1 = 3;
    $scope.isLoading = false;

    // Data Source & Comments variables
    $scope.infoToggle = false;
    $scope.dataComments = {};
    $scope.dataSources = [];
    $scope.showCommentModal = false;
    $scope.commentModalData = {
        site: '',
        day: 0,
        value: '',
        text: '',
        date: ''
    };
    $scope.selectedMon = '';
    $scope.selectedYr = '';

    $scope.avyr = [
        { yr : "--Select--", yrval : "Select"},
        { yr: "2030",yrval: "2030"},
        { yr: "2029",yrval: "2029"},
        { yr: "2028",yrval: "2028"},
        { yr: "2027",yrval: "2027"},
        { yr: "2026",yrval: "2026"},
        { yr: "2025",yrval: "2025"},
        { yr: "2024",yrval: "2024"},
        { yr: "2023",yrval: "2023"},
        { yr: "2022",yrval: "2022"},
        { yr: "2021",yrval: "2021"},
        { yr: "2020",yrval: "2020"},
        { yr: "2019",yrval: "2019"},
        { yr: "2018",yrval: "2018"},
        { yr: "2017",yrval: "2017"},
        { yr: "2016",yrval: "2016"},
        { yr: "2015",yrval: "2015"}
        ]
    $scope.avmon = [
      {nm : "--Select--", smon : "Select"},
      {nm : "January", smon : "01"},
      {nm : "February", smon : "02"},
      {nm : "March", smon : "03"},
      {nm : "April", smon : "04"},
      {nm : "May", smon : "05"},
      {nm : "June", smon : "06"},
      {nm : "July", smon : "07"},
      {nm : "August", smon : "08"},
      {nm : "September", smon : "09"},
      {nm : "October", smon : "10"},
      {nm : "November", smon : "11"},
      {nm : "December", smon : "12"},
    ];

    // Calculate previous month and year for default selection
    var today = new Date();
    var prevMonth = today.getMonth(); // 0-indexed, so current month - 1 = previous month index
    var prevYear = today.getFullYear();
    if (prevMonth === 0) {
        prevMonth = 12; // December
        prevYear = prevYear - 1;
    }
    var prevMonthStr = prevMonth < 10 ? "0" + prevMonth : "" + prevMonth;
    var prevYearStr = "" + prevYear;

    // Find the index in avmon array (index 1 = January = "01", index 12 = December = "12")
    $scope.form = {type : prevMonthStr};
    $scope.form1 = {type : prevYearStr};

    $scope.avyr1 = [
        { yr : "--Select--", yrval : "Select"},
        { yr: "2030",yrval: "2030"},
        { yr: "2029",yrval: "2029"},
        { yr: "2028",yrval: "2028"},
        { yr: "2027",yrval: "2027"},
        { yr: "2026",yrval: "2026"},
        { yr: "2025",yrval: "2025"},
        { yr: "2024",yrval: "2024"},
        { yr: "2023",yrval: "2023"},
        { yr: "2022",yrval: "2022"},
        { yr: "2021",yrval: "2021"},
        { yr: "2020",yrval: "2020"},
        { yr: "2019",yrval: "2019"},
        { yr: "2018",yrval: "2018"},
        { yr: "2017",yrval: "2017"},
        { yr: "2016",yrval: "2016"},
        { yr: "2015",yrval: "2015"}
        ]
    $scope.avmon1 = [
      {nm : "--Select--", smon : "Select"},
      {nm : "January", smon : "01"},
      {nm : "February", smon : "02"},
      {nm : "March", smon : "03"},
      {nm : "April", smon : "04"},
      {nm : "May", smon : "05"},
      {nm : "June", smon : "06"},
      {nm : "July", smon : "07"},
      {nm : "August", smon : "08"},
      {nm : "September", smon : "09"},
      {nm : "October", smon : "10"},
      {nm : "November", smon : "11"},
      {nm : "December", smon : "12"},
    ];
    $scope.fform2 = {type : $scope.avmon1[0].smon};
    $scope.fform21 = {type : $scope.avyr1[0].yrval};

     $scope.avyr2 = [
        { yr : "--Select--", yrval : "Select"},
        { yr: "2030",yrval: "2030"},
        { yr: "2029",yrval: "2029"},
        { yr: "2028",yrval: "2028"},
        { yr: "2027",yrval: "2027"},
        { yr: "2026",yrval: "2026"},
        { yr: "2025",yrval: "2025"},
        { yr: "2024",yrval: "2024"},
        { yr: "2023",yrval: "2023"},
        { yr: "2022",yrval: "2022"},
        { yr: "2021",yrval: "2021"},
        { yr: "2020",yrval: "2020"},
        { yr: "2019",yrval: "2019"},
        { yr: "2018",yrval: "2018"},
        { yr: "2017",yrval: "2017"},
        { yr: "2016",yrval: "2016"},
        { yr: "2015",yrval: "2015"}
        ]
    $scope.avmon2 = [
      {nm : "--Select--", smon : "Select"},
      {nm : "January", smon : "01"},
      {nm : "February", smon : "02"},
      {nm : "March", smon : "03"},
      {nm : "April", smon : "04"},
      {nm : "May", smon : "05"},
      {nm : "June", smon : "06"},
      {nm : "July", smon : "07"},
      {nm : "August", smon : "08"},
      {nm : "September", smon : "09"},
      {nm : "October", smon : "10"},
      {nm : "November", smon : "11"},
      {nm : "December", smon : "12"},
    ];
    $scope.form2 = {type : $scope.avmon2[0].smon};
    $scope.form21 = {type : $scope.avyr2[0].yrval};


    $scope.setTab = function (newTab) {
	    $scope.tab1 = newTab;
	    document.getElementById("gdat").className = "list-group-item menutab";
	    document.getElementById("fdat").className = "list-group-item menutab";
	    document.getElementById("ydat").className = "list-group-item menutab";
	    document.getElementById("adat").className = "list-group-item menutab";
	    document.getElementById("attdat").className = "list-group-item menutab";
	    if(newTab==3) document.getElementById("fdat").className = "list-group-item active";
	    if(newTab==4) document.getElementById("gdat").className = "list-group-item active";
	    if(newTab==5) {
	        document.getElementById("ydat").className = "list-group-item active";
	        $scope.dbjsonpost("getallyearlydata",JSON.stringify({}));
	    }
	    if(newTab==6) document.getElementById("adat").className = "list-group-item active";
	    if(newTab==7) document.getElementById("attdat").className = "list-group-item active";
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
      $scope.isLoading = true;
      var responsePromise = $http.post(starget,data, cfig);
		responsePromise.success(function(response, status, headers, config){
		    $scope.isLoading = false;
			console.log("success post");
			var resp = response;
            var typ=resp.sel;

            if(typ == "getTsuData")
            {
                document.getElementById("msg1").innerHTML = "HF Radar report has been sent successfully";
            }

            if(typ == "getHFData")
            {
                $scope.hfdat = resp.dat;
            }

            if(typ == "getUnqIds")
            {
                $scope.hfids = resp.dat;
            }

            if(typ == "getalldatabymon")
            {
                $scope.rcdata = resp.rcnt;
                $scope.rcperc = resp.perc;
                $scope.lstids = resp.idlist;
                $scope.hfyr = resp.hyr;
                $scope.hfmon = resp.hmon;
                $scope.hfrows = resp.hrows;
                // Store comments and data sources
                $scope.dataComments = resp.comments || {};
                $scope.dataSources = resp.sources || [];
                // Build full source list for all active sites (show "None" if no data)
                var sourceMap = {};
                for(var si = 0; si < $scope.dataSources.length; si++) {
                    sourceMap[$scope.dataSources[si].site.toUpperCase()] = $scope.dataSources[si];
                }
                $scope.dataSourcesFull = [];
                for(var si2 = 0; si2 < resp.idlist.length; si2++) {
                    var siteCode = resp.idlist[si2];
                    var existing = sourceMap[siteCode.toUpperCase()];
                    if(existing) {
                        $scope.dataSourcesFull.push(existing);
                    } else {
                        $scope.dataSourcesFull.push({site: siteCode, date: 'None', source: 'None', timestamp: 'None'});
                    }
                }
                if($scope.hfrows=='28')
                {
                    $scope.hfrowflg="1";
                }
                if($scope.hfrows=='29')
                {
                    $scope.hfrowflg="2";
                }
                if($scope.hfrows=='30')
                {
                    $scope.hfrowflg="3";
                }
                if($scope.hfrows=='31')
                {
                    $scope.hfrowflg="4";
                }
                document.getElementById('rhdr2').innerHTML="(This is Report1)";
            }

            if(typ == "getalldatabyyr" || typ == "getalldatabyyr1")
            {
                $scope.entries1 = resp.percdata;
                $scope.totent = resp.totpercdata;
                $scope.crmon = resp.cmon;
                $scope.cryr = resp.cyr;
                $scope.pryr = resp.pyr;
                $scope.titl = resp.titl;
                var sstr = "<table width='1250' align='center'  border='0' bordercolor='#000000'><thead>";
				sstr +=  "<tr valign='center'><th align='center' colspan='"+($scope.titl.length + 2)+"' bgcolor='#FFFFFF'>HF Radar Data Reception "+$scope.crmon+ " ";
				sstr += $scope.pryr+" - "+ $scope.crmon+" " +  $scope.cryr+"</th></tr>";
				sstr +=  "<tr valign='center'><th align='center' width=130' bgcolor='#FFFFFF'>Site Name</th>";
				for(var i=0;i<$scope.titl.length;i++)
                {
				    sstr +="<th align='center' width='80' bgcolor='#FFFFFF' >"+$scope.titl[i]+"</th>";
                }
				sstr +="</tr></thead><tbody>";
                var ival=0;
                for(var i=0;i<$scope.entries1.length;i++)
                {
                    sstr += "<tr valign='center'>";
    				sstr += "<th align='center' width='130' bgcolor='#FFFFFF'>"+$scope.entries1[i].cid1+"</th>";

    				for(var j=0;j<$scope.entries1[i].pdata.perc1.length;j++)
                    {
        			    sstr += "<th align='center' width='80' bgcolor='"+$scope.entries1[i].pdata.ccode1[j]+"'>"+$scope.entries1[i].pdata.perc1[j]+"</th>";
                    }
                    sstr += "<th align='center' width='80' bgcolor='"+$scope.entries1[i].cod1+"'>"+$scope.entries1[i].perc1+"</th></tr>";

                    ival=ival+1;
                    if(ival>2)
                    {
                        sstr += "<tr valign='center'><th align='center' width='130' bgcolor='#FFFFFF'></th>"
                        for(var j=0;j<$scope.entries1[i].pdata.perc1.length;j++)
                        {
                        sstr += "<th align='center' width='80'bgcolor='#FFFFFF'>&nbsp;</th>";
                        }
                        sstr += "<th align='center' width='80' bgcolor='#FFFFFF'>&nbsp;</th></tr>";
                        ival=0;
                    }
                }
                sstr+="</tbody></table>";
                document.getElementById('rep2').innerHTML=sstr;
                if(typ == "getalldatabyyr") {
                    document.getElementById('rhdr2').innerHTML="(This is Report2)";
                } else {
                    document.getElementById('rhdr2').innerHTML="(This is Report3, which has true totals)";
                }
            }

            if(typ == "getalldatabyyr2")
            {
                $scope.entries1 = resp.percdata;
                $scope.totent = resp.totpercdata;
                $scope.crmon = resp.cmon;
                $scope.cryr = resp.cyr;
                $scope.crmon1 = resp.cmon1;
                $scope.cryr1 = resp.cyr1;
                $scope.pryr = resp.pyr;
                $scope.titl = resp.titl;
                $scope.rng = resp.rng;
                var sstr = "<table width='1250' align='center'  border='0' bordercolor='#000000'><thead>";
				sstr +=  "<tr valign='center'><th align='center' colspan='"+$scope.rng+"' bgcolor='#FFFFFF'>HF Radar Data Reception "+$scope.crmon+ " ";
				sstr += $scope.cryr+" - "+ $scope.crmon1+" " +  $scope.cryr1+"</th></tr>";
				sstr +=  "<tr valign='center'><th align='center' width=130' bgcolor='#FFFFFF'>Site Name</th>";
				for(var i=0;i<$scope.titl.length;i++)
                {
				    sstr +="<th align='center' width='80' bgcolor='#FFFFFF' >"+$scope.titl[i]+"</th>";
                }
				sstr +="</tr></thead><tbody>";
                var ival=0;
                for(var i=0;i<$scope.entries1.length;i++)
                {
                    sstr += "<tr valign='center'>";
    				sstr += "<th align='center' width='130' bgcolor='#FFFFFF'>"+$scope.entries1[i].cid1+"</th>";

    				for(var j=0;j<$scope.entries1[i].pdata.perc1.length;j++)
                    {
        			    sstr += "<th align='center' width='80' bgcolor='"+$scope.entries1[i].pdata.ccode1[j]+"'>"+$scope.entries1[i].pdata.perc1[j]+"</th>";
                    }
                    sstr += "<th align='center' width='80' bgcolor='"+$scope.entries1[i].cod1+"'>"+$scope.entries1[i].perc1+"</th></tr>";

                    ival=ival+1;
                    if(ival>2)
                    {
                        sstr += "<tr valign='center'><th align='center' width='130' bgcolor='#FFFFFF'></th>"
                        for(var j=0;j<$scope.entries1[i].pdata.perc1.length;j++)
                        {
                        sstr += "<th align='center' width='80'bgcolor='#FFFFFF'>&nbsp;</th>";
                        }
                        sstr += "<th align='center' width='80' bgcolor='#FFFFFF'>&nbsp;</th></tr>";
                        ival=0;
                    }
                }
                sstr+="</tbody></table>";
                document.getElementById('rep3').innerHTML=sstr;
            }

            if(typ == "getallyearlydata") {
                $scope.entries1 = resp.entries;
                $scope.titl = resp.idlist;
                $scope.rng = resp.rng;
                var sstr = "<table width='1250' align='center'  border='0' bordercolor='#000000'><thead>";
				sstr +=  "<tr valign='center'><th align='center' colspan='"+$scope.rng+"' bgcolor='#FFFFFF'>HF Radar Data Reception </th></tr>";
				sstr +=  "<tr valign='center'><th align='center' width=130' bgcolor='#FFFFFF'>SITE (INSTALL DATE)</th>";
				for(var i=0;i<$scope.titl.length;i++) sstr +="<th align='center' width='80' bgcolor='#FFFFFF' >"+$scope.titl[i]+"</th>";
                sstr +="</tr></thead><tbody>";
                var ival=0;
                for(var i=0;i<$scope.entries1.length;i++) {
                    sstr += "<tr valign='center'>";
    				sstr += "<th align='center' width='130' bgcolor='#FFFFFF'>"+$scope.entries1[i].yr+"</th>";
    				for(var j=0;j<$scope.entries1[i].avgrcperc.length;j++) {
    				    $scope.rcarr = $scope.entries1[i].avgrcperc;
    				    $scope.icarr = $scope.entries1[i].icol;
    				    sstr += "<th align='center' width='80' bgcolor='"+$scope.icarr[j]+"'>"+$scope.rcarr[j]+"</th>"; }
                    sstr += "</tr>";
                }
                sstr+="</tbody></table>";
                document.getElementById('rep4').innerHTML=sstr;
            }

            // Handle sendToGsheet response
            if(typ == "sendToGsheet") {
                if(resp.stat == "success") {
                    document.getElementById("sendGsheetMsg").innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                } else {
                    document.getElementById("sendGsheetMsg").innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                }
            }

            // Handle saveDataComment response
            if(typ == "saveDataComment") {
                var msgEl = document.getElementById("commentSaveMsg");
                if(msgEl) {
                    if(resp.stat == "success") {
                        msgEl.innerHTML = '<span style="color:green;">' + resp.msg + '</span>';
                        // Update local comments cache
                        var key = $scope.commentModalData.site.toUpperCase() + "_" + $scope.commentModalData.day;
                        $scope.dataComments[key] = {
                            text: $scope.commentModalData.text,
                            by: 'web',
                            at: new Date().toISOString()
                        };
                        // Close modal after short delay
                        $timeout(function() {
                            $scope.showCommentModal = false;
                        }, 1000);
                    } else {
                        msgEl.innerHTML = '<span style="color:red;">' + resp.msg + '</span>';
                    }
                }
            }


		});

		responsePromise.error(function(err, status, headers, config){
		    $scope.isLoading = false;
            console.log(err);
        });
	}

    // Toggle Info display (shows comment indicators and enables click-to-add)
    $scope.toggleInfo = function() {
        $scope.infoToggle = !$scope.infoToggle;
    };

    // Check if a cell has a comment
    $scope.hasComment = function(rowIndex, colIndex) {
        var day = rowIndex + 1;
        var site = $scope.lstids[colIndex];
        if(!site) return false;
        var key = site.toUpperCase() + "_" + day;
        return $scope.dataComments[key] && $scope.dataComments[key].text;
    };

    // Get comment for a cell
    $scope.getComment = function(rowIndex, colIndex) {
        var day = rowIndex + 1;
        var site = $scope.lstids[colIndex];
        if(!site) return null;
        var key = site.toUpperCase() + "_" + day;
        return $scope.dataComments[key] || null;
    };

    // Show tooltip on hover (only if info toggle is on and comment exists)
    $scope.showTooltip = function(event, rowIndex, colIndex) {
        if(!$scope.infoToggle) return;

        var comment = $scope.getComment(rowIndex, colIndex);
        if(!comment || !comment.text) return;

        var tooltip = document.getElementById('cellTooltip');
        if(!tooltip) return;

        var site = $scope.lstids[colIndex];
        var day = rowIndex + 1;
        tooltip.innerHTML = '<strong>' + site + ' - Day ' + day + '</strong><br>' + comment.text;
        if(comment.by) {
            tooltip.innerHTML += '<br><em style="font-size:10px;">By: ' + comment.by + '</em>';
        }

        // Position tooltip above the cell
        var rect = event.target.getBoundingClientRect();
        tooltip.style.left = (rect.left + rect.width/2 - 100) + 'px';
        tooltip.style.top = (rect.top - 60) + 'px';
        tooltip.style.display = 'block';
    };

    // Hide tooltip
    $scope.hideTooltip = function() {
        var tooltip = document.getElementById('cellTooltip');
        if(tooltip) {
            tooltip.style.display = 'none';
        }
    };

    // Handle cell click (for adding/editing comments)
    $scope.cellClick = function(rowIndex, colIndex, value) {
        if(!$scope.infoToggle) return;

        // Check if this is a data row (not the percentage row)
        var maxRow = parseInt($scope.hfrows) - 1;
        if(rowIndex > maxRow) return;

        // Only allow click on 0 or <24 values
        var numVal = parseInt(value);
        if(value !== '0' && (isNaN(numVal) || numVal >= 24)) return;

        var day = rowIndex + 1;
        var site = $scope.lstids[colIndex];
        if(!site) return;

        // Get month number from hfmon
        var monthNames = ["January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"];
        var monNum = monthNames.indexOf($scope.hfmon) + 1;
        var monStr = monNum < 10 ? "0" + monNum : "" + monNum;
        var dayStr = day < 10 ? "0" + day : "" + day;
        var dateStr = $scope.hfyr + "-" + monStr + "-" + dayStr;

        // Get existing comment if any
        var existingComment = $scope.getComment(rowIndex, colIndex);

        $scope.commentModalData = {
            site: site,
            day: day,
            value: value,
            text: existingComment ? existingComment.text : '',
            date: dateStr
        };

        $scope.showCommentModal = true;
        // Clear any previous messages
        $timeout(function() {
            var msgEl = document.getElementById("commentSaveMsg");
            if(msgEl) msgEl.innerHTML = '';
        }, 0);
    };

    // Close comment modal
    $scope.closeCommentModal = function() {
        $scope.showCommentModal = false;
    };

    // Save comment
    $scope.saveComment = function() {
        var apiData = {
            site_code: $scope.commentModalData.site,
            comment_date: $scope.commentModalData.date,
            comment_text: $scope.commentModalData.text,
            created_by: 'web'
        };
        $scope.dbjsonpost("saveDataComment", JSON.stringify(apiData));
    };

    // Helper to parse int (for ng-class expression)
    $scope.parseInt = function(val) {
        return parseInt(val);
    };


});

myApp.controller('TabsCtrl', function ($scope) {

	$scope.sendrep = function()
	{
		var x1=document.forms["aform"]["optselmon"].value;
		var x2=document.forms["aform"]["optselyr"].value;
        if ((x1 == "Select"))
		{
			document.getElementById("uIdMessage21").innerHTML = "Select month";
			document.getElementById("uIdMessage29").innerHTML = "";
			document.getElementById("msg1").innerHTML = "";
		}
		else if ((x2 == "Select"))
		{
			document.getElementById("uIdMessage29").innerHTML = "Select year";
			document.getElementById("uIdMessage21").innerHTML = "";
			document.getElementById("msg1").innerHTML = "";
		}
		else
		{
			document.getElementById("uIdMessage21").innerHTML = "";
			document.getElementById("uIdMessage29").innerHTML = "";
			if ((x2==null || x2=="") )
			{
				document.getElementById("msg1").innerHTML = "Enter password";
			}
			else
			{
			    var wifistr = '{"selmon":"' + x1 + '","selyr":"' + x2 +'"}';
                var wifijsonObj = JSON.parse(wifistr);
                $scope.$parent.dbjsonpost("getTsuData",JSON.stringify(wifijsonObj));
			}
		}
	}

	$scope.resetfrm = function()
	{
	    document.getElementById('optselmon').selectedIndex = 0;
        document.getElementById('optselyr').selectedIndex = 0;
        document.getElementById("msg1").innerHTML = "";
        document.getElementById("uIdMessage29").innerHTML = "";
        document.getElementById("uIdMessage21").innerHTML = "";

	}
});

myApp.controller('RTabsCtrl', function ($scope) {

    $scope.getalldatabyyr2 = function()
	{
	    var x2=document.forms["rfform2"]["optlstmon21"].value;
	    var x3=document.forms["rfform2"]["optlstyr22"].value;
	    var x4=document.forms["rfform2"]["optlstmon23"].value;
	    var x5=document.forms["rfform2"]["optlstyr24"].value;

	    if(x2 == "Select")
	    {
	        document.getElementById("crmsg3").innerHTML = "Please select from month";
	    }
	    else if(x3 == "Select")
	    {
	        document.getElementById("crmsg3").innerHTML = "Please select from year";
	    }
	    else if(x4 == "Select")
	    {
	        document.getElementById("crmsg3").innerHTML = "Please select to month";
	    }
	    else if(x5 == "Select")
	    {
	        document.getElementById("crmsg3").innerHTML = "Please select to year";
	    }
	    else if(parseInt(x5) < parseInt(x3))
	    {
	        document.getElementById("crmsg3").innerHTML = "To date must be greater than from date";
	    }
	    else if(parseInt(x2) >= parseInt(x4) && parseInt(x5) == parseInt(x3))
	    {
	        document.getElementById("crmsg3").innerHTML = "To date must be greater than from date";
	    }
	    else
	    {
	        document.getElementById("crmsg3").innerHTML = "";
    	    $scope.$parent.iv1=2;
            var wifistr = '{"mon":"' + x2 + '","yr":"' + x3 + '","mon1":"' + x4 + '","yr1":"' + x5 + '"}';
            var wifijsonObj = JSON.parse(wifistr);
            $scope.$parent.dbjsonpost("getalldatabyyr2",JSON.stringify(wifijsonObj));
		}

	}

	$scope.resetfrm2 = function()
	{
	    document.getElementById('optlstmon21').selectedIndex = 0;
	    document.getElementById('optlstyr22').selectedIndex = 0;
	    document.getElementById('optlstmon23').selectedIndex = 0;
	    document.getElementById('optlstyr24').selectedIndex = 0;
        document.getElementById("crmsg3").innerHTML = "";
        $scope.$parent.iv1=0;
	}

});


myApp.controller('FTabsCtrl', function ($scope) {

    $scope.getalldatabymon = function()
	{
	    var x2=document.forms["fform"]["optlstmon"].value;
	    var x3=document.forms["fform"]["optlstyr"].value;

	    if(x2 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select month";
	    }
	    else if(x3 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select year";
	    }
	    else
	    {
	        document.getElementById("msg3").innerHTML = "";
	        $scope.$parent.iv=1;
	        // Store selected month/year for comment functionality
	        $scope.$parent.selectedMon = x2;
	        $scope.$parent.selectedYr = x3;
	        var wifistr = '{"mon":"' + x2 + '","yr":"' + x3 + '"}';
            var wifijsonObj = JSON.parse(wifistr);
            $scope.$parent.dbjsonpost("getalldatabymon",JSON.stringify(wifijsonObj));
	    }
	}

	$scope.getalldatabyyr = function()
	{
	    var x2=document.forms["fform"]["optlstmon"].value;
	    var x3=document.forms["fform"]["optlstyr"].value;

	    if(x2 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select month";
	    }
	    else if(x3 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select year";
	    }
	    else
	    {
	        document.getElementById("msg3").innerHTML = "";
    	    $scope.$parent.iv=2;
            var wifistr = '{"mon":"' + x2 + '","yr":"' + x3 + '"}';
            var wifijsonObj = JSON.parse(wifistr);
            $scope.$parent.dbjsonpost("getalldatabyyr",JSON.stringify(wifijsonObj));
		}

	}

	$scope.getalldatabyyr1 = function()
	{
	    var x2=document.forms["fform"]["optlstmon"].value;
	    var x3=document.forms["fform"]["optlstyr"].value;

	    if(x2 == "Select")
	    {
	       document.getElementById("msg3").innerHTML = "Please select month";
	    }
	    else if(x3 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select year";
	    }
	    else
	    {
	        document.getElementById("msg3").innerHTML = "";
    	    $scope.$parent.iv=2;
            var wifistr = '{"mon":"' + x2 + '","yr":"' + x3 + '"}';
            var wifijsonObj = JSON.parse(wifistr);
            $scope.$parent.dbjsonpost("getalldatabyyr1",JSON.stringify(wifijsonObj));
		}

	}

	// SEND button - sends Monthly Report data to Google Sheets
	$scope.sendToGsheet = function()
	{
	    var x2=document.forms["fform"]["optlstmon"].value;
	    var x3=document.forms["fform"]["optlstyr"].value;

	    if(x2 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select month";
	        document.getElementById("sendGsheetMsg").innerHTML = "";
	    }
	    else if(x3 == "Select")
	    {
	        document.getElementById("msg3").innerHTML = "Please select year";
	        document.getElementById("sendGsheetMsg").innerHTML = "";
	    }
	    else
	    {
	        document.getElementById("msg3").innerHTML = "";
	        // Get month name for confirmation message
	        var monthNames = ["", "January", "February", "March", "April", "May", "June",
	                         "July", "August", "September", "October", "November", "December"];
	        var monthNum = parseInt(x2);
	        var monthName = monthNames[monthNum];

	        // Show confirmation dialog
	        if(confirm("Send Monthly Report data for " + monthName + " " + x3 + " to Google Sheets?"))
	        {
	            document.getElementById("sendGsheetMsg").innerHTML = '<span style="color:blue;">Sending to Google Sheets...</span>';
	            var wifistr = '{"mon":"' + x2 + '","yr":"' + x3 + '"}';
	            var wifijsonObj = JSON.parse(wifistr);
	            $scope.$parent.dbjsonpost("exportMonthlyData",JSON.stringify(wifijsonObj));
	        }
		}
	}

	$scope.resetfrm2 = function()
	{
	    document.getElementById('optlstmon').selectedIndex = 0;
	    document.getElementById('optlstyr').selectedIndex = 0;
        document.getElementById("msg3").innerHTML = "";
        $scope.$parent.iv=0;
	}



});

myApp.controller('YTabsCtrl', function ($scope) {
$scope.ival2=2;
});

myApp.controller('ATabsCtrl', function ($scope) {
$scope.ival2=2;
});

// Attendance Controller (User View)
myApp.controller('AttTabsCtrl', function ($scope, $http) {
    $scope.attSiteFilter = "";
    $scope.attRecords = [];
    $scope.attSiteList = [];
    $scope.attSummary = [];
    $scope.showAllRecords = false;

    // Load site list on init
    var cfig = { headers: { 'Content-Type': 'application/json' } };
    $http.post("getConfig", "{}", cfig).then(function(response) {
        $scope.attSiteList = response.data.sites || [];
    });

    // Load attendance summary (latest entry per site)
    $scope.loadAttendanceSummary = function() {
        var cfig = { headers: { 'Content-Type': 'application/json' } };
        $http.post("getAttendanceSummary", "{}", cfig).then(
            function(response) {
                $scope.attSummary = response.data.summary || [];
            },
            function(error) {
                console.log("Error loading attendance summary:", error);
            }
        );
    }

    // Load all attendance records
    $scope.loadUserAttendance = function() {
        var cfig = { headers: { 'Content-Type': 'application/json' } };
        $http.post("getAttendance", JSON.stringify({site_code: $scope.attSiteFilter}), cfig).then(
            function(response) {
                $scope.attRecords = response.data.records || [];
            },
            function(error) {
                console.log("Error loading attendance:", error);
            }
        );
    }

    // Toggle between summary and all records
    $scope.toggleView = function() {
        $scope.showAllRecords = !$scope.showAllRecords;
        if ($scope.showAllRecords) {
            $scope.loadUserAttendance();
        }
    }

    // Auto-load summary on init
    $scope.loadAttendanceSummary();
});

