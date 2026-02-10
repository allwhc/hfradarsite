// ===== CONFIG =====
var DEFAULT_SERVER = "https://hfradarsite.pythonanywhere.com";
var DEFAULT_SITES = ["Cuda","Kalp","Mach","Yanm","Wasi","Jgri","Gopa","Puri","Ptbl","Htby"];
var ADMIN_PWD = "niot@321";
var MONTH_NAMES = ["January","February","March","April","May","June","July","August","September","October","November","December"];
var REASONS_LIST = [
    "Radar System Problem",
    "Macmini Not Working",
    "Generator Not Working",
    "UPS Not Working",
    "Stabilizer Not Working",
    "No Diesel",
    "No Electricity For Long",
    "Antenna Fall Down",
    "AC Failed",
    "System Service/Repair/APM",
    "Other"
];

// ===== STATE =====
var radialValues = [];
var reasons = [];
var currentReasonDay = -1;
var dataSaved = false;
var dataSent = false;
var qrSourceType = "Manual";
var passwordModalCallback = null;

// QR camera state
var qrVideoStream = null;
var qrScanTimer = null;

// ===== INIT =====
document.addEventListener("DOMContentLoaded", function() {
    initNavigation();
    initDropdowns();
    loadSettings();
    loadSavedData();
    loadAttendanceSites();
    loadHistory();
    updateSitesStatus();
});

// ===== NAVIGATION =====
function initNavigation() {
    var menuBtn = document.getElementById("menuToggle");
    var sideMenu = document.getElementById("sideMenu");
    var overlay = document.getElementById("overlay");

    menuBtn.addEventListener("click", function(e) {
        e.stopPropagation();
        sideMenu.classList.add("open");
        overlay.classList.add("show");
    });

    overlay.addEventListener("click", function() {
        sideMenu.classList.remove("open");
        overlay.classList.remove("show");
    });

    var menuItems = document.querySelectorAll(".side-menu-list li");
    for (var i = 0; i < menuItems.length; i++) {
        menuItems[i].addEventListener("click", function() {
            var pageName = this.getAttribute("data-page");
            showPage(pageName);
            for (var j = 0; j < menuItems.length; j++) menuItems[j].classList.remove("active");
            this.classList.add("active");
            sideMenu.classList.remove("open");
            overlay.classList.remove("show");
        });
    }
}

function showPage(pageName) {
    var pages = document.querySelectorAll(".page");
    for (var i = 0; i < pages.length; i++) pages[i].classList.remove("active");
    var el = document.getElementById("page-" + pageName);
    if (el) el.classList.add("active");
}

// ===== TOAST =====
function showToast(msg, duration) {
    var t = document.getElementById("toast");
    t.textContent = msg;
    t.classList.add("show");
    setTimeout(function() { t.classList.remove("show"); }, duration || 2500);
}

// ===== PASSWORD MODAL =====
function showPasswordModal(title, msg, callback) {
    document.getElementById("pwdModalTitle").textContent = title || "Authorization Required";
    document.getElementById("pwdModalMsg").textContent = msg || "";
    document.getElementById("pwdModalInput").value = "";
    document.getElementById("passwordModal").style.display = "flex";
    passwordModalCallback = callback;
    setTimeout(function() { document.getElementById("pwdModalInput").focus(); }, 100);
}

function closePasswordModal(confirmed) {
    document.getElementById("passwordModal").style.display = "none";
    if (passwordModalCallback) {
        var val = document.getElementById("pwdModalInput").value;
        passwordModalCallback(confirmed ? val : null);
        passwordModalCallback = null;
    }
}

// ===== SETTINGS / STORAGE =====
function getServerUrl() {
    return localStorage.getItem("hfr_server_url") || DEFAULT_SERVER;
}

function getSitesList() {
    var stored = localStorage.getItem("hfr_sites_list");
    if (stored) {
        try { return JSON.parse(stored); } catch(e) {}
    }
    return DEFAULT_SITES;
}

function getAppPassword() {
    return localStorage.getItem("hfr_app_pwd") || "";
}

function loadSettings() {
    document.getElementById("settingsUrl").value = getServerUrl();
    document.getElementById("settingsPassword").value = getAppPassword();
}

function saveSettings() {
    var pwd = document.getElementById("settingsPassword").value.trim();
    var url = document.getElementById("settingsUrl").value.trim();
    if (!pwd) { showToast("Password cannot be empty"); return; }
    if (!url) { showToast("Server URL cannot be empty"); return; }
    if (url.charAt(url.length - 1) === "/") url = url.substring(0, url.length - 1);
    localStorage.setItem("hfr_app_pwd", pwd);
    localStorage.setItem("hfr_server_url", url);
    document.getElementById("settingsMsg").innerHTML = '<span style="color:green;">Settings saved!</span>';
    showToast("Settings saved");
}

function toggleUrlLock() {
    var urlInput = document.getElementById("settingsUrl");
    if (urlInput.disabled) {
        showPasswordModal("Unlock URL", "Enter admin password to edit server URL.", function(pwd) {
            if (pwd === ADMIN_PWD) {
                urlInput.disabled = false;
                document.getElementById("urlLockIcon").innerHTML = "&#128275;";
                showToast("URL unlocked");
            } else if (pwd !== null) {
                showToast("Incorrect password");
            }
        });
    } else {
        urlInput.disabled = true;
        document.getElementById("urlLockIcon").innerHTML = "&#128274;";
    }
}

function syncSitesList() {
    var url = getServerUrl();
    apiPost(url + "/getConfig", {}, function(resp) {
        if (resp && resp.sites && resp.sites.length > 0) {
            localStorage.setItem("hfr_sites_list", JSON.stringify(resp.sites));
            updateSitesStatus();
            initDropdowns();
            loadAttendanceSites();
            showToast("Sites list updated: " + resp.sites.length + " sites");
        } else {
            showToast("No sites received from server");
        }
    }, function(err) {
        showToast("Error: " + err);
    });
}

function updateSitesStatus() {
    var sites = getSitesList();
    var stored = localStorage.getItem("hfr_sites_list");
    var el = document.getElementById("sitesStatus");
    if (stored) {
        el.textContent = "Sites: " + sites.length + " sites loaded (" + sites.join(", ") + ")";
    } else {
        el.textContent = "Sites: Using default list";
    }
}

// ===== DROPDOWNS =====
function initDropdowns() {
    var now = new Date();
    // Default to previous month
    var defMonth = now.getMonth(); // 0-indexed: Jan=0
    var defYear = now.getFullYear();
    if (defMonth === 0) {
        defMonth = 12;
        defYear--;
    }
    // defMonth is now 1-indexed previous month

    // Month dropdowns (value 1-12)
    var monthHtml = "";
    for (var i = 1; i <= 12; i++) {
        monthHtml += '<option value="' + i + '"' + (i === defMonth ? ' selected' : '') + '>' + MONTH_NAMES[i - 1] + '</option>';
    }
    document.getElementById("selMonth").innerHTML = monthHtml;
    document.getElementById("verifyMonth").innerHTML = monthHtml;

    // Year dropdowns
    var curYear = now.getFullYear();
    var yearHtml = "";
    for (var y = curYear; y >= curYear - 4; y--) {
        yearHtml += '<option value="' + y + '"' + (y === defYear ? ' selected' : '') + '>' + y + '</option>';
    }
    document.getElementById("selYear").innerHTML = yearHtml;
    document.getElementById("verifyYear").innerHTML = yearHtml;

    // Site dropdown
    var sites = getSitesList();
    var siteHtml = '<option value="">Select Site</option>';
    for (var s = 0; s < sites.length; s++) {
        siteHtml += '<option value="' + sites[s] + '">' + sites[s] + '</option>';
    }
    document.getElementById("selSite").innerHTML = siteHtml;

    // Restore last site
    var lastSite = localStorage.getItem("hfr_last_site");
    if (lastSite) document.getElementById("selSite").value = lastSite;

    buildDayGrid();
}

// ===== DAY GRID =====
function getDaysInMonth(month, year) {
    return new Date(year, month, 0).getDate();
}

function getDayName(year, month, day) {
    var d = new Date(year, month - 1, day);
    return ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"][d.getDay()];
}

function buildDayGrid() {
    var month = parseInt(document.getElementById("selMonth").value);
    var year = parseInt(document.getElementById("selYear").value);
    if (!month || !year) return;

    var days = getDaysInMonth(month, year);

    // Init arrays if size doesn't match
    if (radialValues.length !== days) {
        radialValues = [];
        reasons = [];
        for (var k = 0; k < days; k++) { radialValues.push(0); reasons.push(""); }
    }

    var html = "";
    for (var i = 0; i < days; i++) {
        var dayNum = i + 1;
        var dayName = getDayName(year, month, dayNum);
        var dateStr = dayNum + "/" + month + "/" + year;
        var val = radialValues[i] || 0;
        var hasReason = reasons[i] && reasons[i] !== "";
        var noteClass = val >= 24 ? "btn-note hidden" : (hasReason ? "btn-note has-reason" : "btn-note");
        var altClass = (i % 2 === 0) ? " alt" : "";

        html += '<div class="day-row' + altClass + '">';
        html += '<div class="day-info">';
        html += '<div class="day-date">' + dateStr + '</div>';
        html += '<div class="day-name">Day ' + dayNum + ' (' + dayName + ')</div>';
        html += '</div>';
        html += '<div class="day-controls">';
        html += '<button class="btn-adj" onclick="adjRadial(' + i + ',-1)">−</button>';
        html += '<span class="radial-val" id="rv' + i + '">' + val + '</span>';
        html += '<button class="btn-adj" onclick="adjRadial(' + i + ',1)">+</button>';
        html += '<button class="' + noteClass + '" id="nb' + i + '" onclick="openReasonModal(' + i + ')">&#9998;</button>';
        html += '</div>';
        html += '</div>';
    }
    document.getElementById("dayGrid").innerHTML = html;
    updateTotal();
    updateDataStatus();
}

function adjRadial(idx, delta) {
    var val = (radialValues[idx] || 0) + delta;
    if (val < 0) val = 0;
    if (val > 24) val = 24;
    radialValues[idx] = val;
    document.getElementById("rv" + idx).textContent = val;
    dataSaved = false;
    dataSent = false;
    updateTotal();
    updateDataStatus();

    var nb = document.getElementById("nb" + idx);
    if (val >= 24) {
        nb.className = "btn-note hidden";
        reasons[idx] = "";
    } else {
        var hasReason = reasons[idx] && reasons[idx] !== "";
        nb.className = hasReason ? "btn-note has-reason" : "btn-note";
    }
}

function updateTotal() {
    var total = 0;
    for (var i = 0; i < radialValues.length; i++) total += (radialValues[i] || 0);
    document.getElementById("totalRadial").textContent = total;
}

function updateDataStatus() {
    var badge = document.getElementById("dataStatus");
    var sendBtn = document.getElementById("btnSend");
    if (dataSent) {
        badge.style.display = "inline-block";
        badge.className = "status-badge status-sent";
        badge.textContent = "Sent";
        sendBtn.disabled = true;
    } else if (dataSaved) {
        badge.style.display = "inline-block";
        badge.className = "status-badge status-saved";
        badge.textContent = "Saved";
        sendBtn.disabled = false;
    } else {
        badge.style.display = "none";
        sendBtn.disabled = true;
    }
}

function onMonthYearSiteChange() {
    var site = document.getElementById("selSite").value;
    if (site) localStorage.setItem("hfr_last_site", site);
    dataSaved = false;
    dataSent = false;
    qrSourceType = "Manual";
    // Try loading saved data, else reset
    if (!loadSavedData()) {
        radialValues = [];
        reasons = [];
        buildDayGrid();
    }
}

// ===== REASON MODAL =====
function openReasonModal(dayIdx) {
    currentReasonDay = dayIdx;
    var month = parseInt(document.getElementById("selMonth").value);
    var year = parseInt(document.getElementById("selYear").value);
    document.getElementById("reasonDayLabel").textContent = (dayIdx + 1) + "/" + month + "/" + year;
    document.getElementById("reasonRadialLabel").textContent = radialValues[dayIdx] || 0;

    var currentReason = reasons[dayIdx] || "";
    var isCustom = currentReason !== "" && REASONS_LIST.indexOf(currentReason) === -1 && currentReason !== "Other";

    var html = "";
    for (var r = 0; r < REASONS_LIST.length; r++) {
        var reason = REASONS_LIST[r];
        var sel = (reason === currentReason || (reason === "Other" && isCustom)) ? " selected" : "";
        html += '<button class="reason-option' + sel + '" data-reason="' + reason + '" onclick="selectReasonOption(this)">' + reason + '</button>';
    }
    document.getElementById("reasonOptions").innerHTML = html;

    if (isCustom) {
        document.getElementById("reasonCustomDiv").style.display = "block";
        document.getElementById("reasonCustomText").value = currentReason;
    } else {
        document.getElementById("reasonCustomDiv").style.display = "none";
        document.getElementById("reasonCustomText").value = "";
    }

    document.getElementById("reasonModal").style.display = "flex";
}

function selectReasonOption(btn) {
    var options = document.querySelectorAll(".reason-option");
    for (var i = 0; i < options.length; i++) options[i].classList.remove("selected");
    btn.classList.add("selected");
    if (btn.getAttribute("data-reason") === "Other") {
        document.getElementById("reasonCustomDiv").style.display = "block";
        setTimeout(function() { document.getElementById("reasonCustomText").focus(); }, 100);
    } else {
        document.getElementById("reasonCustomDiv").style.display = "none";
    }
}

function saveReason() {
    var selected = document.querySelector(".reason-option.selected");
    if (!selected) {
        reasons[currentReasonDay] = "";
    } else {
        var val = selected.getAttribute("data-reason");
        if (val === "Other") {
            var custom = document.getElementById("reasonCustomText").value.trim();
            reasons[currentReasonDay] = custom ? ("Other: " + custom) : "Other";
        } else {
            reasons[currentReasonDay] = val;
        }
    }
    var nb = document.getElementById("nb" + currentReasonDay);
    if (reasons[currentReasonDay]) {
        nb.className = "btn-note has-reason";
    } else {
        nb.className = "btn-note";
    }
    dataSaved = false;
    updateDataStatus();
    closeReasonModal();
}

function closeReasonModal() {
    document.getElementById("reasonModal").style.display = "none";
    currentReasonDay = -1;
}

// ===== SAVE / SEND DATA =====
function getStorageKey() {
    var m = document.getElementById("selMonth").value;
    var y = document.getElementById("selYear").value;
    var s = document.getElementById("selSite").value;
    return "hfr_data_" + s + "_" + m + "_" + y;
}

function saveData() {
    var site = document.getElementById("selSite").value;
    if (!site) { showToast("Please select a site"); return; }

    var total = 0;
    for (var i = 0; i < radialValues.length; i++) total += (radialValues[i] || 0);

    if (!confirm("Total Radial Count: " + total + "\n\nIs this correct?")) return;

    var data = {
        site: site,
        month: document.getElementById("selMonth").value,
        year: document.getElementById("selYear").value,
        radials: radialValues.slice(),
        reasons: reasons.slice(),
        total: total,
        sent: false,
        source: qrSourceType,
        savedAt: new Date().toISOString()
    };
    localStorage.setItem(getStorageKey(), JSON.stringify(data));
    dataSaved = true;
    dataSent = false;
    updateDataStatus();
    showToast("Data saved locally");
}

function loadSavedData() {
    var key = getStorageKey();
    var stored = localStorage.getItem(key);
    if (stored) {
        try {
            var data = JSON.parse(stored);
            radialValues = data.radials || [];
            reasons = data.reasons || [];
            dataSaved = true;
            dataSent = data.sent || false;
            qrSourceType = data.source || "Manual";
            buildDayGrid();
            return true;
        } catch(e) {}
    }
    return false;
}

function sendData() {
    var site = document.getElementById("selSite").value;
    var month = parseInt(document.getElementById("selMonth").value);
    var year = parseInt(document.getElementById("selYear").value);

    if (!site) { showToast("Please select a site"); return; }
    if (!dataSaved) { showToast("Please save data first"); return; }

    var total = 0;
    for (var i = 0; i < radialValues.length; i++) total += (radialValues[i] || 0);

    // Determine if this is the default (previous) month
    var now = new Date();
    var prevMonth = now.getMonth(); // 0-indexed
    var prevYear = now.getFullYear();
    if (prevMonth === 0) { prevMonth = 12; prevYear--; }

    var isCurrentPeriod = (month === prevMonth && year === prevYear);

    if (!isCurrentPeriod) {
        showPasswordModal("Authorization Required", "Enter admin password to send data for " + MONTH_NAMES[month - 1] + " " + year, function(pwd) {
            if (pwd === ADMIN_PWD) {
                doSend(site, month, year, total);
            } else if (pwd !== null) {
                showToast("Incorrect password");
            }
        });
    } else {
        var confirmMsg = "Site: " + site + "\nMonth: " + MONTH_NAMES[month - 1] + " " + year + "\nTotal: " + total + "\n\nSend this data?";
        if (confirm(confirmMsg)) {
            doSend(site, month, year, total);
        }
    }
}

function doSend(site, month, year, total) {
    var days = getDaysInMonth(month, year);
    var cred1 = [];
    for (var i = 0; i < days; i++) {
        var dayNum = i + 1;
        var mm = month < 10 ? "0" + month : "" + month;
        var dd = dayNum < 10 ? "0" + dayNum : "" + dayNum;
        var dateStr = year + "-" + mm + "-" + dd;
        var entry = {
            id: site,
            dt: dateStr,
            rc: String(radialValues[i] || 0),
            rchex: ""
        };
        if (reasons[i] && reasons[i] !== "") {
            entry.reason = reasons[i];
        }
        cred1.push(entry);
    }

    var payload = {
        source_type: qrSourceType,
        cred1: cred1
    };

    showToast("Sending data...");
    document.getElementById("btnSend").disabled = true;

    apiPost(getServerUrl() + "/upldTsuData", payload, function(resp) {
        dataSent = true;
        var key = getStorageKey();
        var stored = localStorage.getItem(key);
        if (stored) {
            var data = JSON.parse(stored);
            data.sent = true;
            localStorage.setItem(key, JSON.stringify(data));
        }
        updateDataStatus();
        showToast("Data sent successfully!");
        addHistory(site, month, year, total, qrSourceType, true);
    }, function(err) {
        document.getElementById("btnSend").disabled = false;
        showToast("Send failed: " + err);
        addHistory(site, month, year, total, qrSourceType, false);
    });
}

function resetData() {
    if (!confirm("Reset all radial values to 0?")) return;
    var days = radialValues.length;
    radialValues = [];
    reasons = [];
    for (var i = 0; i < days; i++) { radialValues.push(0); reasons.push(""); }
    dataSaved = false;
    dataSent = false;
    qrSourceType = "Manual";
    buildDayGrid();
    showToast("All values reset");
}

// ===== QR CODE (Native getUserMedia + jsQR) =====
function startQRScan() {
    var modal = document.getElementById("qrModal");
    var video = document.getElementById("qrVideo");
    var statusEl = document.getElementById("qrStatus");

    modal.style.display = "flex";
    statusEl.textContent = "Starting camera...";

    // Check for camera support
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        statusEl.textContent = "Camera not supported on this browser.";
        return;
    }

    var constraints = {
        video: {
            facingMode: "environment",
            width: { ideal: 1280 },
            height: { ideal: 720 }
        }
    };

    navigator.mediaDevices.getUserMedia(constraints)
        .then(function(stream) {
            qrVideoStream = stream;
            video.srcObject = stream;
            video.setAttribute("playsinline", "true");
            video.play();
            statusEl.textContent = "Point camera at QR code...";
            // Start scanning after video is playing
            video.addEventListener("loadedmetadata", function() {
                startQRScanning();
            });
        })
        .catch(function(err) {
            statusEl.textContent = "Camera error: " + err.message;
        });
}

function startQRScanning() {
    var video = document.getElementById("qrVideo");
    var canvas = document.getElementById("qrCanvas");
    var ctx = canvas.getContext("2d");
    var statusEl = document.getElementById("qrStatus");

    function scanFrame() {
        if (!qrVideoStream) return;

        if (video.readyState === video.HAVE_ENOUGH_DATA) {
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

            var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            var code = jsQR(imageData.data, imageData.width, imageData.height, {
                inversionAttempts: "dontInvert"
            });

            if (code && code.data) {
                statusEl.textContent = "QR Code found!";
                stopQRScan();
                processQRData(code.data);
                return;
            }
        }
        qrScanTimer = requestAnimationFrame(scanFrame);
    }

    qrScanTimer = requestAnimationFrame(scanFrame);
}

function stopQRScan() {
    if (qrScanTimer) {
        cancelAnimationFrame(qrScanTimer);
        qrScanTimer = null;
    }
    if (qrVideoStream) {
        var tracks = qrVideoStream.getTracks();
        for (var i = 0; i < tracks.length; i++) tracks[i].stop();
        qrVideoStream = null;
    }
    var video = document.getElementById("qrVideo");
    video.srcObject = null;
    document.getElementById("qrModal").style.display = "none";
}

function processQRData(qrString) {
    // Format: Site:Month:Year:Day1:Day2:...DayN:Total[:Hex1:Hex2:...]
    var parts = qrString.split(":");
    if (parts.length < 4) {
        showToast("Invalid QR format");
        return;
    }

    var qrSite = parts[0];
    var qrMonth = parseInt(parts[1]);
    var qrYear = parseInt(parts[2]);

    if (isNaN(qrMonth) || isNaN(qrYear) || qrMonth < 1 || qrMonth > 12) {
        showToast("Invalid QR data");
        return;
    }

    var days = getDaysInMonth(qrMonth, qrYear);
    var qrRadials = [];
    var qrTotal = 0;

    for (var i = 0; i < days; i++) {
        var partIdx = 3 + i;
        var val = 0;
        if (partIdx < parts.length) {
            var p = parts[partIdx];
            // Skip hex values
            if (p.indexOf("0x") === 0 || p.indexOf("0X") === 0) {
                val = 0;
            } else {
                val = parseInt(p) || 0;
            }
        }
        if (val < 0) val = 0;
        if (val > 24) val = 24;
        qrRadials.push(val);
        qrTotal += val;
    }

    // Validate total if present
    var totalIdx = 3 + days;
    if (totalIdx < parts.length) {
        var expectedTotal = parseInt(parts[totalIdx]);
        if (!isNaN(expectedTotal) && expectedTotal !== qrTotal) {
            showToast("QR validation failed! Expected total: " + expectedTotal + ", got: " + qrTotal);
            return;
        }
    }

    // Match site name (case-insensitive)
    var sites = getSitesList();
    var matchedSite = qrSite;
    for (var s = 0; s < sites.length; s++) {
        if (sites[s].toLowerCase() === qrSite.toLowerCase()) {
            matchedSite = sites[s];
            break;
        }
    }

    // Set dropdowns
    document.getElementById("selMonth").value = qrMonth;
    document.getElementById("selYear").value = qrYear;
    document.getElementById("selSite").value = matchedSite;

    radialValues = qrRadials;
    reasons = [];
    for (var j = 0; j < days; j++) reasons.push("");

    dataSaved = false;
    dataSent = false;
    qrSourceType = "QR";

    buildDayGrid();
    showToast("QR data loaded (Total: " + qrTotal + "). Edit before saving.");
}

// ===== ATTENDANCE =====
function loadAttendanceSites() {
    var sites = getSitesList();
    var html = '<option value="">-- Select Site --</option>';
    for (var i = 0; i < sites.length; i++) {
        html += '<option value="' + sites[i] + '">' + sites[i] + '</option>';
    }
    document.getElementById("attSite").innerHTML = html;

    var savedName = localStorage.getItem("hfr_att_name");
    var savedSite = localStorage.getItem("hfr_att_site");
    if (savedName) document.getElementById("attName").value = savedName;
    if (savedSite) document.getElementById("attSite").value = savedSite;
}

function markAttendance() {
    var name = document.getElementById("attName").value.trim();
    var site = document.getElementById("attSite").value;

    if (!name) { showToast("Please enter your name"); return; }
    if (!site) { showToast("Please select a site"); return; }

    localStorage.setItem("hfr_att_name", name);
    localStorage.setItem("hfr_att_site", site);

    document.getElementById("btnMarkAttendance").disabled = true;
    document.getElementById("attLoading").style.display = "block";
    document.getElementById("attResult").style.display = "none";

    if (!navigator.geolocation) {
        submitAttendance(name, site, 0, 0);
        return;
    }

    navigator.geolocation.getCurrentPosition(
        function(pos) {
            submitAttendance(name, site, pos.coords.latitude, pos.coords.longitude);
        },
        function(err) {
            submitAttendance(name, site, 0, 0);
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 120000 }
    );
}

function submitAttendance(name, site, lat, lng) {
    var payload = {
        staff_name: name,
        site_code: site,
        latitude: lat,
        longitude: lng
    };

    apiPost(getServerUrl() + "/markAttendance", payload, function(resp) {
        document.getElementById("btnMarkAttendance").disabled = false;
        document.getElementById("attLoading").style.display = "none";
        var resultEl = document.getElementById("attResult");
        resultEl.style.display = "block";

        if (resp.stat === "success") {
            resultEl.className = "result-box result-success";
            resultEl.innerHTML = '<h5>Attendance Marked!</h5><p>Time: ' + (resp.timestamp || "") + '</p>';
        } else {
            resultEl.className = "result-box result-error";
            resultEl.innerHTML = '<h5>Could Not Mark Attendance</h5><p>' + (resp.msg || "Unknown error") + '</p>';
        }
    }, function(err) {
        document.getElementById("btnMarkAttendance").disabled = false;
        document.getElementById("attLoading").style.display = "none";
        var resultEl = document.getElementById("attResult");
        resultEl.style.display = "block";
        resultEl.className = "result-box result-error";
        resultEl.innerHTML = '<h5>Error</h5><p>' + err + '</p>';
    });
}

// ===== VERIFY DATA =====
function fetchVerifyData() {
    var month = document.getElementById("verifyMonth").value;
    var year = document.getElementById("verifyYear").value;

    var now = new Date();
    var prevMonth = now.getMonth();
    var prevYear = now.getFullYear();
    if (prevMonth === 0) { prevMonth = 12; prevYear--; }

    var isDefault = (parseInt(month) === prevMonth && parseInt(year) === prevYear);

    if (!isDefault) {
        showPasswordModal("Authorization Required", "Enter admin password to view data for " + MONTH_NAMES[parseInt(month) - 1] + " " + year, function(pwd) {
            if (pwd === ADMIN_PWD) {
                doFetchVerify(month, year);
            } else if (pwd !== null) {
                showToast("Incorrect password");
            }
        });
    } else {
        doFetchVerify(month, year);
    }
}

function doFetchVerify(month, year) {
    document.getElementById("verifyLoading").style.display = "block";
    document.getElementById("verifyResult").style.display = "none";
    document.getElementById("verifyError").style.display = "none";

    apiPost(getServerUrl() + "/getalldatabymon", { mon: month, yr: year }, function(resp) {
        document.getElementById("verifyLoading").style.display = "none";

        if (!resp || !resp.rcnt || !resp.idlist) {
            document.getElementById("verifyError").style.display = "block";
            document.getElementById("verifyError").innerHTML = '<h5>No Data</h5><p>No data found for this period.</p>';
            return;
        }

        buildVerifyTable(resp);
        document.getElementById("verifyResult").style.display = "block";
    }, function(err) {
        document.getElementById("verifyLoading").style.display = "none";
        document.getElementById("verifyError").style.display = "block";
        document.getElementById("verifyError").innerHTML = '<h5>Error</h5><p>' + err + '</p>';
    });
}

function buildVerifyTable(resp) {
    var sites = resp.idlist || [];
    var rows = resp.rcnt || [];

    var html = '<thead><tr><th>Day</th>';
    for (var s = 0; s < sites.length; s++) html += '<th>' + sites[s] + '</th>';
    html += '</tr></thead><tbody>';

    for (var r = 0; r < rows.length; r++) {
        var isLastRow = (r === rows.length - 1);
        var rowStyle = "";
        if (isLastRow) {
            rowStyle = ' style="background:#4CAF50; color:#fff; font-weight:bold;"';
        } else if (r % 2 === 0) {
            rowStyle = ' style="background:#E3F2FD;"';
        }

        html += '<tr' + rowStyle + '>';
        html += '<td style="font-weight:bold;">' + (isLastRow ? '%' : (r + 1)) + '</td>';

        var rowData = rows[r];
        if (typeof rowData === "string") {
            try { rowData = JSON.parse(rowData); } catch(e) { rowData = [rowData]; }
        }
        if (!Array.isArray(rowData)) rowData = [rowData];

        for (var c = 0; c < sites.length; c++) {
            var val = (c < rowData.length) ? rowData[c] : "";
            var cellStyle = "";
            if (isLastRow && val !== "") {
                var pct = parseFloat(val);
                if (pct >= 70) cellStyle = ' style="background:#80FF00; color:#000;"';
                else if (pct >= 50) cellStyle = ' style="background:#FFFF66; color:#000;"';
                else cellStyle = ' style="background:#FF9933; color:#000;"';
            }
            html += '<td' + cellStyle + '>' + val + '</td>';
        }
        html += '</tr>';
    }

    html += '</tbody>';
    document.getElementById("verifyTable").innerHTML = html;
}

// ===== HISTORY =====
function addHistory(site, month, year, total, method, success) {
    var history = JSON.parse(localStorage.getItem("hfr_history") || "[]");
    history.unshift({
        site: site,
        month: month,
        year: year,
        total: total,
        method: method === "QR" ? "QR Code Scan" : "Manual Entry",
        success: success,
        timestamp: new Date().toLocaleString()
    });
    if (history.length > 50) history = history.slice(0, 50);
    localStorage.setItem("hfr_history", JSON.stringify(history));
    loadHistory();
}

function loadHistory() {
    var history = JSON.parse(localStorage.getItem("hfr_history") || "[]");
    var el = document.getElementById("historyList");

    if (history.length === 0) {
        el.innerHTML = '<p style="text-align:center; color:#999; padding:40px 20px;">No history yet. Send data to see transmission history here.</p>';
        return;
    }

    var html = "";
    for (var i = 0; i < history.length; i++) {
        var h = history[i];
        var iconClass = h.success ? "success" : "failed";
        var iconSymbol = h.success ? "&#10003;" : "&#10007;";
        var monthName = MONTH_NAMES[(h.month || 1) - 1] || "";

        html += '<div class="history-card">';
        html += '<div class="history-icon ' + iconClass + '">' + iconSymbol + '</div>';
        html += '<div class="history-info">';
        html += '<div class="hi-title">' + h.site + ' - ' + monthName + ' ' + h.year + '</div>';
        html += '<div class="hi-sub">' + h.timestamp + ' | ' + h.method + '</div>';
        html += '</div>';
        html += '<div class="history-total">' + h.total + '</div>';
        html += '</div>';
    }

    el.innerHTML = html;
}

// ===== API HELPER =====
function apiPost(url, data, onSuccess, onError) {
    fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(function(response) {
        if (!response.ok) throw new Error("HTTP " + response.status);
        return response.json();
    })
    .then(function(json) {
        onSuccess(json);
    })
    .catch(function(err) {
        onError(err.message || "Network error");
    });
}
