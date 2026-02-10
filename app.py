from flask import Flask,request,json,send_file,render_template
from flask_cors import CORS, cross_origin
from xlwt import Workbook
import os,math,smtplib,os.path,sqlite3,subprocess,requests
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from datetime import datetime, date
from email import encoders
from gsheetRAD import insert_data_to_gsheet, get_previous_month_and_year

app = Flask(__name__)
cors = CORS(app, resources={r"/api/*": {"origins": "*"}})
app.secret_key = 'A0Zr98j/3yX R~XHH!jmN]LWX/,?RT'

# Database path
DB_PATH = '/home/hfradarsite/deploy/users.db'

# PythonAnywhere API Configuration (for auto-reload)
# Set these environment variables in PythonAnywhere bash or in a .env file
PYTHONANYWHERE_USERNAME = os.environ.get('PA_USERNAME', 'hfradarsite')  # Your PythonAnywhere username
PYTHONANYWHERE_DOMAIN = os.environ.get('PA_DOMAIN', 'hfradarsite.pythonanywhere.com')  # Your domain
PYTHONANYWHERE_API_TOKEN = os.environ.get('PA_API_TOKEN', '')  # Your API token from PythonAnywhere Account page

# Database helper functions
def upd_qry(sql):
    dbcon = None
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute(sql)
        dbcon.commit()
        cursor.close()
    except Exception as e:
        print(e)
    finally:
        if dbcon:
            dbcon.close()
    return

def dboper(qry,bval):
    stat = False
    row = []
    dbcon = None
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute(qry)
        if bval:
            row = cursor.fetchall()
            if row is None:
                row = []
        else:
            row = cursor.fetchone()
        stat = True
        cursor.close()
    except Exception as e:
        print ("error",e)
        if bval:
            row = []
    finally:
        if dbcon:
            dbcon.close()
    return row, stat

# Initialize sites table if not exists
def init_sites_table():
    dbcon = None
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        # Create sites table
        cursor.execute('''CREATE TABLE IF NOT EXISTS sites (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            site_code VARCHAR(10) UNIQUE NOT NULL,
            display_order INTEGER,
            is_active INTEGER DEFAULT 1
        )''')
        dbcon.commit()

        # Check if sites table is empty
        cursor.execute("SELECT COUNT(*) FROM sites")
        count = cursor.fetchone()[0]

        if count == 0:
            # Populate with existing sites
            default_sites = ["Cuda","Kalp","Mach","Yanm","Wasi","Jgri","Gopa","Puri","Ptbl","Htby"]
            for idx, site in enumerate(default_sites):
                cursor.execute("INSERT INTO sites (site_code, display_order, is_active) VALUES (?, ?, 1)", (site, idx + 1))
            dbcon.commit()
            print("Sites table initialized with default sites")
        cursor.close()
    except Exception as e:
        print("Error initializing sites table:", e)
    finally:
        if dbcon:
            dbcon.close()

# Initialize on startup
init_sites_table()

# Add site_lat and site_lng columns to sites table (migration)
def migrate_sites_add_coords():
    dbcon = None
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        # Check if columns exist
        cursor.execute("PRAGMA table_info(sites)")
        cols = [row[1] for row in cursor.fetchall()]
        if 'site_lat' not in cols:
            cursor.execute("ALTER TABLE sites ADD COLUMN site_lat REAL DEFAULT NULL")
        if 'site_lng' not in cols:
            cursor.execute("ALTER TABLE sites ADD COLUMN site_lng REAL DEFAULT NULL")
        dbcon.commit()

        # Pre-populate known site coordinates
        known_coords = {
            'Yanm': (16.4776833, 82.1021000),
            'Mach': (16.2425667, 81.2376167),
            'Wasi': (20.9343833, 72.7605333),
            'Jgri': (21.0394500, 71.8054833),
            'Puri': (19.8065500, 85.8641500),
            'Gopa': (19.3033167, 84.9658500),
            'Kalp': (12.4922167, 80.1590000),
            'Cuda': (11.6862333, 79.7733167),
            'Htby': (10.5923000, 92.5627667),
            'Ptbl': (11.5701333, 92.7376500),
        }
        for code, (lat, lng) in known_coords.items():
            cursor.execute("UPDATE sites SET site_lat=?, site_lng=? WHERE site_code=? AND site_lat IS NULL", (lat, lng, code))
        dbcon.commit()
        cursor.close()
    except Exception as e:
        print("Error migrating sites coords:", e)
    finally:
        if dbcon:
            dbcon.close()

migrate_sites_add_coords()

# Initialize attendance table
def init_attendance_table():
    dbcon = None
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS attendance (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            staff_name VARCHAR(100) NOT NULL,
            site_code VARCHAR(10) NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            timestamp DATETIME NOT NULL,
            distance_m REAL DEFAULT 0
        )''')
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_attendance_site ON attendance(site_code)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_attendance_time ON attendance(timestamp)")
        dbcon.commit()
        cursor.close()
    except Exception as e:
        print("Error initializing attendance table:", e)
    finally:
        if dbcon:
            dbcon.close()

init_attendance_table()

# Haversine formula to calculate distance between two GPS coordinates (in meters)
def haversine_distance(lat1, lon1, lat2, lon2):
    R = 6371000  # Earth radius in meters
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlam = math.radians(lon2 - lon1)
    a = math.sin(dphi/2)**2 + math.cos(phi1)*math.cos(phi2)*math.sin(dlam/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return R * c

# Create indexes on maintsu table for faster queries
def init_maintsu_indexes():
    dbcon = None
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        # Create indexes for faster lookups
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_maintsu_tsuid ON maintsu(tsuid)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_maintsu_tsudate ON maintsu(tsudate)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_maintsu_combined ON maintsu(tsuid, tsudate)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_maintsu_flg ON maintsu(flg)")
        # Index for perctsu table as well
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_perctsu_tsuid ON perctsu(tsuid)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_perctsu_tsudate ON perctsu(tsudate)")
        dbcon.commit()
        cursor.close()
        print("Database indexes created/verified")
    except Exception as e:
        print("Error creating indexes:", e)
    finally:
        if dbcon:
            dbcon.close()

# Initialize indexes on startup
init_maintsu_indexes()

# Get active sites from database (replaces hardcoded idlist)
def get_active_sites():
    try:
        srow, stat = dboper("SELECT site_code FROM sites WHERE is_active=1 ORDER BY display_order", 1)
        if srow:
            return [row[0] for row in srow]
    except:
        pass
    # Fallback to hardcoded list if database fails
    return ["Cuda","Kalp","Mach","Yanm","Wasi","Jgri","Gopa","Puri","Ptbl","Htby"]

# Get all sites (for admin)
def get_all_sites():
    try:
        srow, stat = dboper("SELECT id, site_code, display_order, is_active FROM sites ORDER BY display_order", 1)
        if srow:
            return [{'id': row[0], 'code': row[1], 'order': row[2], 'active': row[3]} for row in srow]
    except:
        pass
    return []

ent1=[{"yr":"2008","Cuda":"43","Kalp":"89","Mach":"85","Yanm":"29","Wasi":"","Jgri":"","Gopa":"","Puri":"","Ptbl":"","Htby":""},
    {"yr":"2009","Cuda":"82","Kalp":"91","Mach":"59","Yanm":"10","Wasi":"39","Jgri":"62","Gopa":"86","Puri":"90","Ptbl":"","Htby":""},
    {"yr":"2010","Cuda":"61","Kalp":"100","Mach":"82","Yanm":"45","Wasi":"73","Jgri":"89","Gopa":"45","Puri":"73","Ptbl":"70","Htby":"85"},
    {"yr":"2011","Cuda":"88","Kalp":"99","Mach":"90","Yanm":"63","Wasi":"75","Jgri":"87","Gopa":"76","Puri":"86","Ptbl":"74","Htby":"89"},
    {"yr":"2012","Cuda":"33","Kalp":"96","Mach":"67","Yanm":"63","Wasi":"80","Jgri":"79","Gopa":"52","Puri":"54","Ptbl":"53","Htby":"76"},
    {"yr":"2013","Cuda":"54","Kalp":"55","Mach":"69","Yanm":"28","Wasi":"91","Jgri":"41","Gopa":"50","Puri":"30","Ptbl":"62","Htby":"47"},
    {"yr":"2014","Cuda":"96","Kalp":"97","Mach":"85","Yanm":"81","Wasi":"71","Jgri":"83","Gopa":"2","Puri":"73","Ptbl":"66","Htby":"93"},
    {"yr":"2015","Cuda":"94","Kalp":"96","Mach":"66","Yanm":"78","Wasi":"36","Jgri":"82","Gopa":"96","Puri":"96","Ptbl":"97","Htby":"51"},
    {"yr":"2016","Cuda":"73","Kalp":"91","Mach":"74","Yanm":"53","Wasi":"56","Jgri":"76","Gopa":"58","Puri":"72","Ptbl":"70","Htby":"74"},
    {"yr":"2017","Cuda":"84","Kalp":"92","Mach":"78","Yanm":"60","Wasi":"60","Jgri":"72","Gopa":"65","Puri":"77","Ptbl":"77","Htby":"77"},
    {"yr":"2018","Cuda":"95","Kalp":"97","Mach":"94","Yanm":"86","Wasi":"96","Jgri":"89","Gopa":"89","Puri":"96","Ptbl":"97","Htby":"92"},
    {"yr":"2019","Cuda":"87","Kalp":"86","Mach":"96","Yanm":"79","Wasi":"97","Jgri":"90","Gopa":"83","Puri":"34","Ptbl":"99","Htby":"92"},
    {"yr":"2020","Cuda":"99","Kalp":"99","Mach":"57","Yanm":"80","Wasi":"94","Jgri":"91","Gopa":"76","Puri":"5","Ptbl":"100","Htby":"91"},
    {"yr":"2021","Cuda":"89","Kalp":"98","Mach":"78","Yanm":"69","Wasi":"99","Jgri":"74","Gopa":"76","Puri":"97","Ptbl":"94","Htby":"77"}]
lstmon = [31,28,31,30,31,30,31,31,30,31,30,31]
# idlist is now loaded from database dynamically
# Initialize with values from DB (falls back to hardcoded if DB fails)
idlist = get_active_sites()
lstmn1 = ["Jan","Feb","Mar","Apr","May","Jun","July","Aug","Sep","Oct","Nov","Dec"]
lstmn = ["January","February","March","April","May","June","July","August","September","October","November","December"]

# Function to refresh idlist from database (call after site changes)
def refresh_idlist():
    global idlist
    idlist = get_active_sites()

# Get site groups from site_pairs table for vector calculations
def get_site_groups():
    """Returns list of site groups configured in site_pairs table.
    Each group is a dict: {'group': 'Group1', 'sites': ['Cuda', 'Kalp']}
    If no groups configured, returns empty list (fallback to sequential pairing)."""
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("SELECT DISTINCT group_name FROM site_pairs ORDER BY group_name")
        groups = cursor.fetchall()

        result = []
        for group in groups:
            group_name = group[0]
            cursor.execute("SELECT site_code FROM site_pairs WHERE group_name=? AND site_code != '__placeholder__' ORDER BY site_code", (group_name,))
            sites = [row[0] for row in cursor.fetchall()]
            if len(sites) >= 1:  # Allow single-site groups too
                result.append({'group': group_name, 'sites': sites})
        dbcon.close()
        return result
    except Exception as e:
        print("Error getting site groups:", e)
        return []

@app.route("/")
def main():
    return render_template('gvmlogin.html')

@app.route("/authen", methods=['POST'])
def authen():
    styp = request.form['styp']
    srow1, stat1 = dboper("SELECT * from hfena where hfenadis='1'",1)
    srow, stat = dboper("SELECT * from user where username='"+request.form['username']+"' and password='"+request.form['password']+"' and usrtype='"+styp+"'",1)
    if len(srow) == 0:
        return render_template('gvmlogin.html',emsg="Incorrect username/password")
    else:
        if styp == "user":
            if len(srow1) == 0:
                return render_template('gvmlogin.html',emsg="Site deactivated. To enable this site please contact site administrators for the payment.")
            else:
                return send_file('templates/TsuHome.html')
        elif styp == "admin":
            return send_file('templates/admhome.html')

@app.route('/logout')
def logout():
    return render_template('gvmlogin.html')

@app.route('/aboutus')
def aboutus():
    return render_template('AboutUs.html')

@app.route("/upldTsuData", methods=['POST'])
@cross_origin()
def upldTsuData():
    data1 = request.get_json(silent=True)
    # Determine source type from request (defaults to AUTO if not specified)
    source_type = data1.get("source_type", "AUTO")
    if source_type not in ['AUTO', 'QR', 'Manual']:
        source_type = 'AUTO'

    logged_sites = set()
    for data in data1["cred1"]:
        srow, stat = dboper("SELECT * FROM maintsu where tsudate='"+data["dt"]+"' and tsuid ='"+data["id"]+"'",1)
        if len(srow) > 0:
            upd_qry("update maintsu set radialcnthex='"+data["rchex"]+"', radialcnt='"+data["rc"]+"',flg = 0 where tsudate='"+data["dt"]+"' and tsuid='"+data["id"]+"'")
        else:
            upd_qry("insert into maintsu (tsuid,tsudate,radialcnt,flg,radialcnthex) values('"+data["id"]+"','"+data["dt"]+"','"+data["rc"]+"',0,'"+data["rchex"]+"')")

        # Auto-log data source for each site+date (once per site per upload)
        site_date_key = data["id"].upper() + "_" + data["dt"]
        if site_date_key not in logged_sites:
            logged_sites.add(site_date_key)
            try:
                dbcon = sqlite3.connect(DB_PATH)
                cursor = dbcon.cursor()
                cursor.execute("SELECT id FROM data_source_log WHERE site_code=? AND log_date=?", (data["id"].upper(), data["dt"]))
                existing = cursor.fetchone()
                if existing:
                    cursor.execute("UPDATE data_source_log SET source_type=?, received_timestamp=datetime('now') WHERE site_code=? AND log_date=?",
                                 (source_type, data["id"].upper(), data["dt"]))
                else:
                    cursor.execute("INSERT INTO data_source_log (site_code, log_date, source_type, received_timestamp) VALUES (?, ?, ?, datetime('now'))",
                                 (data["id"].upper(), data["dt"], source_type))
                dbcon.commit()
                dbcon.close()
            except Exception as e:
                print("Error auto-logging data source:", e)

        # Save optional reason/comment per day entry
        reason = data.get("reason", "")
        if reason and reason.strip():
            try:
                dbcon = sqlite3.connect(DB_PATH)
                cursor = dbcon.cursor()
                cursor.execute("INSERT OR REPLACE INTO data_comments (site_code, comment_date, comment_text, created_by, created_at) VALUES (?, ?, ?, ?, datetime('now'))",
                             (data["id"].upper(), data["dt"], reason.strip(), source_type + " Upload"))
                dbcon.commit()
                dbcon.close()
            except Exception as e:
                print("Error saving day reason:", e)

    return json.dumps({'sel':'upldTsuData','stat':"success"})

def getTsuIds(sql):
    entries=[]
    srow, stat = dboper(sql,1)
    for row1 in srow:
        entries.append(row1[0])
    return entries

def getIdNo(lstent,tid):
    no = 0
    for val in lstent:
        if val.upper() == tid.upper():
            return no  # Found - return the index
        no = no + 1
    return -1  # Not found - return -1 instead of out-of-bounds index

def getTsuList(sql):
    """Optimized: SQL already filters by date, no need for Python filtering"""
    entries=[]
    srow,stat = dboper(sql,1)
    for row1 in srow:
        rchexval="0"
        if(row1[4]!=None):
            rchexval=str(row1[4])
        entries.append({"id":row1[0],"tsid":str(row1[1]),"tsdt":str(row1[2]),"rc":str(row1[3]),"rchex":rchexval})
    return entries

@app.route("/gethfdata",methods=['POST'])
@cross_origin()
def gethfdata():
    data1 = request.get_json(silent=True)
    entries=[]
    srow,stat = dboper("select * from maintsu where flg=0 and tsuid = '"+data1["id"]+"' and tsudate between '"+(data1["yr"]+"-"+data1["mon"]+"-01")+"' and '"+(data1["yr"]+"-"+data1["mon"]+"-31")+"' order by tsudate",1)
    for row1 in srow:
        entries.append({'id':row1[1],'dt':row1[2],'rc':row1[3]})
    return json.dumps({'sel':'getHFData','dat':entries})

@app.route("/getUnqIds", methods=['POST'])
def getUnqIds():
    data1 = request.get_json(silent=True)
    return json.dumps({'sel':'getUnqIds','dat':getTsuIds1("SELECT DISTINCT(tsuid) FROM maintsu where tsudate between '"+(data1["yr"]+"-"+data1["mon"]+"-01")+"' and '"+(data1["yr"]+"-"+data1["mon"]+"-31")+ "' and flg = 0")})

def getTsuIds1(sql):
    entries=[]
    srow,stat = dboper(sql,1)
    for row1 in srow:
        entries.append({'id':row1[0]})
    return entries



@app.route("/getalldatabymon", methods=['POST'])
def getalldatabymon():
    # Refresh site list from database before processing
    refresh_idlist()
    # Initialize all return variables before try block
    lstperc= []
    lstrcdata = []
    lstrcdata1=[]
    comments = {}
    sources = []
    data1 = request.get_json(silent=True)
    selmon = int(data1["mon"])
    selyr = int(data1["yr"])
    crows=getmthdays(selmon,selyr)
    try:
        ccols = int(len(idlist))
        tsulist = getTsuList("SELECT ID, tsuid, tsudate, radialcnt, radialcnthex FROM maintsu where tsudate between '"+str(selyr)+gettmon(selmon)+"-01"+"' and '"+str(selyr)+gettmon(selmon)+"-31"+"' order by tsudate")
        tsulen = len(tsulist)
        if tsulen > 0:
            for v in range(0,32):
                lstcol = []
                for k in range(0, ccols):
                    lstcol.append("0")
                lstrcdata.append(lstcol)

            # Collect all IDs for batch UPDATE
            all_ids = []
            for dat in tsulist:
                site_idx = getIdNo(idlist, dat["tsid"])
                if site_idx >= 0:  # Only process if site is found in current idlist
                    lstrcdata[((int(dat["tsdt"].split("-")[2]))-1)][site_idx] = dat["rc"]
                all_ids.append(str(dat["id"]))

            # BATCH UPDATE: Update all records in ONE query instead of loop
            if all_ids:
                try:
                    dbcon = sqlite3.connect(DB_PATH)
                    cursor = dbcon.cursor()
                    # Use IN clause for batch update
                    id_list_str = ",".join(all_ids)
                    cursor.execute("UPDATE maintsu SET flg=0 WHERE ID IN ({})".format(id_list_str))
                    dbcon.commit()
                    cursor.close()
                    dbcon.close()
                except Exception as ue:
                    print("Batch update error:", ue)

            lstperc= []
            perc_inserts = []  # Collect all inserts for batch
            for j in range(0,ccols):
                itot = 0;
                for i in range(0,(crows+1)):
                    if lstrcdata[i][j] != "":
                        itot = itot + int(lstrcdata[i][j])
                iperc = 0
                if (itot>0):
                    iperc = int(math.ceil((float((float(itot*100))/(float((24*crows)))))))
                lstperc.append(str(iperc))
                perc_inserts.append((idlist[j], str(selyr)+ "-"+str(selmon), str(iperc)))

            # BATCH INSERT: Insert all percentages in ONE transaction
            if perc_inserts:
                try:
                    dbcon = sqlite3.connect(DB_PATH)
                    cursor = dbcon.cursor()
                    cursor.executemany("INSERT INTO perctsu (tsuid,tsudate,radialperc) VALUES(?,?,?)", perc_inserts)
                    dbcon.commit()
                    cursor.close()
                    dbcon.close()
                except Exception as ie:
                    print("Batch insert error:", ie)

            lstrcdata1=[]
            idx=1
            for val in lstrcdata:
                if(idx<=crows):
                    lstrcdata1.append(val)
                idx=idx+1
            lstrcdata1.append(lstperc)

        # Fetch comments for this month
        try:
            first_day = "{}-{:02d}-01".format(selyr, selmon)
            last_day = "{}-{:02d}-31".format(selyr, selmon)
            dbcon = sqlite3.connect(DB_PATH)
            cursor = dbcon.cursor()
            cursor.execute('''
                SELECT site_code, comment_date, comment_text, created_by, created_at
                FROM data_comments
                WHERE comment_date BETWEEN ? AND ?
                ORDER BY site_code, comment_date
            ''', (first_day, last_day))
            rows = cursor.fetchall()
            for row in rows:
                site = row[0]
                day = int(row[1].split('-')[2])
                key = site.upper() + "_" + str(day)
                comments[key] = {
                    'text': row[2],
                    'by': row[3],
                    'at': row[4]
                }
            dbcon.close()
        except Exception as ce:
            print("Error fetching comments:", ce)

        # Fetch data source summary for this month (most recent per site)
        try:
            first_day = "{}-{:02d}-01".format(selyr, selmon)
            last_day = "{}-{:02d}-31".format(selyr, selmon)
            dbcon = sqlite3.connect(DB_PATH)
            cursor = dbcon.cursor()
            cursor.execute('''
                SELECT site_code, log_date, source_type, received_timestamp
                FROM data_source_log
                WHERE log_date BETWEEN ? AND ?
                AND (site_code, log_date) IN (
                    SELECT site_code, MAX(log_date)
                    FROM data_source_log
                    WHERE log_date BETWEEN ? AND ?
                    GROUP BY site_code
                )
                ORDER BY site_code
            ''', (first_day, last_day, first_day, last_day))
            rows = cursor.fetchall()
            for row in rows:
                sources.append({
                    'site': row[0],
                    'date': row[1],
                    'source': row[2],
                    'timestamp': row[3]
                })
            dbcon.close()
        except Exception as se:
            print("Error fetching data sources:", se)

    except Exception as e:
        print ("getalldatabymon",e)
    return json.dumps({'sel':'getalldatabymon','stat':"success",'rcnt':lstrcdata1,'perc':lstperc,'idlist':idlist,'hmon':str(lstmn[(selmon-1)]),'hyr':str(selyr),'hrows':str(crows),'comments':comments,'sources':sources})

def getdatabycriteria(selmon,selyr,cid):
    srow,stat = dboper("SELECT ID, tsuid, tsudate, radialperc FROM perctsu where tsuid='"+cid+"' and tsudate='"+(str(selyr)+ "-"+str(selmon))+"'",1)
    lstperc='0'
    for row1 in srow:
        lstperc=row1[3]
    return lstperc

# ============ OPTIMIZED BATCH QUERY FUNCTIONS ============

def batch_get_perctsu_data(start_year, start_month, end_year, end_month):
    """Fetch all percentage data for date range in ONE query.
    Returns dict: {(site, year, month): percentage}"""
    try:
        # Build date range
        start_date = "{}-{}".format(start_year, start_month)
        end_date = "{}-{}".format(end_year, end_month)

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("""
            SELECT tsuid, tsudate, radialperc
            FROM perctsu
            WHERE tsudate >= ? AND tsudate <= ?
        """, (start_date, end_date))
        rows = cursor.fetchall()
        dbcon.close()

        # Build lookup dict: key = (site_upper, year, month)
        result = {}
        for row in rows:
            site = row[0].upper() if row[0] else ""
            date_parts = row[1].split("-") if row[1] else ["0", "0"]
            yr = int(date_parts[0])
            mn = int(date_parts[1])
            perc = row[2] if row[2] else "0"
            result[(site, yr, mn)] = str(perc)
        return result
    except Exception as e:
        print("Error in batch_get_perctsu_data:", e)
        return {}

def batch_get_radialcnt_sums(start_year, start_month, end_year, end_month):
    """Fetch SUM of radialcnt grouped by site and month in ONE query.
    Returns dict: {(site, year, month): total_count}"""
    try:
        # Build date range strings
        start_date = "{}{}-01".format(start_year, gettmon(start_month))
        end_date = "{}{}-31".format(end_year, gettmon(end_month))

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("""
            SELECT UPPER(tsuid),
                   SUBSTR(tsudate, 1, 4) as yr,
                   SUBSTR(tsudate, 6, 2) as mn,
                   SUM(CAST(radialcnt AS INTEGER)) as total
            FROM maintsu
            WHERE tsudate >= ? AND tsudate <= ? AND flg = 0
            GROUP BY UPPER(tsuid), SUBSTR(tsudate, 1, 4), SUBSTR(tsudate, 6, 2)
        """, (start_date, end_date))
        rows = cursor.fetchall()
        dbcon.close()

        # Build lookup dict
        result = {}
        for row in rows:
            site = row[0] if row[0] else ""
            yr = int(row[1]) if row[1] else 0
            mn = int(row[2]) if row[2] else 0
            total = row[3] if row[3] else 0
            result[(site, yr, mn)] = str(total)
        return result
    except Exception as e:
        print("Error in batch_get_radialcnt_sums:", e)
        return {}

# Optimized version of getdatayr using batch queries
def getdatayr_optimized(indx2, data1):
    selmon = int(data1["mon"])
    selyr = int(data1["yr"])
    lsttyr = []
    lsttmt = []
    lstidpercbyyr = []
    lsttitleyr = []

    # Pre-fetch all data in batch
    if indx2 == 1:
        # Fetch from perctsu table
        perc_data = batch_get_perctsu_data(selyr-1, selmon, selyr, selmon)
    else:
        # Fetch radialcnt sums
        perc_data = batch_get_radialcnt_sums(selyr-1, selmon, selyr, selmon)

    # Build title list once (same for all sites)
    for i in range(selmon, 13):
        lsttitleyr.append(str(selyr-1) + ' ' + lstmn1[i-1])
        if indx2 == 2:
            lsttyr.append(str(selyr-1))
            lsttmt.append(str(i))
    for i in range(1, selmon+1):
        lsttitleyr.append(str(selyr) + ' ' + lstmn1[i-1])
        if indx2 == 2:
            lsttyr.append(str(selyr))
            lsttmt.append(str(i))

    # Build data for each site using pre-fetched data
    for val in idlist:
        lstidperc = []
        site_upper = val.upper()

        # Previous year months (from selmon to Dec)
        for i in range(selmon, 13):
            key = (site_upper, selyr-1, i)
            lstidperc.append(perc_data.get(key, '0'))

        # Current year months (from Jan to selmon)
        for i in range(1, selmon+1):
            key = (site_upper, selyr, i)
            lstidperc.append(perc_data.get(key, '0'))

        lstidpercbyyr.append(lstidperc)

    return (lstidpercbyyr, lsttitleyr, lsttyr, lsttmt, selmon, selyr)

# ============ END OPTIMIZED BATCH QUERY FUNCTIONS ============

@app.route("/getalldatabyyr", methods=['POST'])
def getalldatabyyr():
    # Refresh site list from database before processing
    refresh_idlist()
    # Use optimized batch query version
    lstidpercbyyr,lsttitleyr,lsttyr,lsttmt,selmon,selyr=getdatayr_optimized(1,request.get_json(silent=True))
    totent=getdatayr1(1,0,lsttitleyr,lstidpercbyyr,lsttmt,lsttyr,selmon,selyr)
    return json.dumps({'sel':'getalldatabyyr','stat':"success",'percdata':totent,'totpercdata':totent,'idlist':idlist,'cmon':str(lstmn[(selmon-1)]),'cyr':str(selyr),'pyr':str((selyr-1)),'titl':lsttitleyr})

@app.route("/getalldatabyyr1", methods=['POST'])
def getalldatabyyr1():
    # Refresh site list from database before processing
    refresh_idlist()
    # Use optimized batch query version
    lstidpercbyyr,lsttitleyr,lsttyr,lsttmt,selmon,selyr=getdatayr_optimized(2,request.get_json(silent=True))
    totent=getdatayr1(2,0,lsttitleyr,lstidpercbyyr,lsttmt,lsttyr,selmon,selyr)
    return json.dumps({'sel':'getalldatabyyr1','stat':"success",'percdata':totent,'totpercdata':totent,'idlist':idlist,'cmon':str(lstmn[(selmon-1)]),'cyr':str(selyr),'pyr':str((selyr-1)),'titl':lsttitleyr})

# SEND button endpoint - exports Monthly Report data using user-selected month/year
@app.route("/exportMonthlyData", methods=['POST'])
def exportMonthlyData():
    try:
        data1 = request.get_json(silent=True)
        selmon = int(data1["mon"])
        selyr = int(data1["yr"])
        # Call the GSheet function with user-selected month/year
        insert_data_to_gsheet(selmon, selyr)
        return json.dumps({'sel':'sendToGsheet', 'stat':'success', 'msg':'Data for '+lstmn[(selmon-1)]+' '+str(selyr)+' exported successfully!'})
    except Exception as e:
        print("Error exporting data:", e)
        return json.dumps({'sel':'sendToGsheet', 'stat':'error', 'msg':'Failed to export data. Please try again.'})

def getdatayr(indx2,data1):
    selmon = int(data1["mon"])
    selyr = int(data1["yr"])
    lsttyr=[]
    lsttmt=[]
    lstidpercbyyr = []
    for val in idlist:
        lstidperc = []
        lsttitleyr = []
        for i in range(selmon,13):
            if indx2==1:
                lstidperc.append(getdatabycriteria(i, (selyr-1), val))
            if indx2==2:
                lstidperc.append(getTotSumList(val,(selyr-1),i))
                lsttyr.append(str((selyr-1)))
                lsttmt.append(str(i))
            lsttitleyr.append((str((selyr-1))+ ' ' + lstmn1[(i-1)]))
        for i in range(1,(selmon+1)):
            if indx2==1:
                lstidperc.append(getdatabycriteria(i, (selyr), val))
            if indx2==2:
                lstidperc.append(getTotSumList(val,selyr,i))
                lsttyr.append(str((selyr)))
                lsttmt.append(str(i))
            lsttitleyr.append((str((selyr))+ ' ' + lstmn1[(i-1)]))
        lstidpercbyyr.append(lstidperc)
    return (lstidpercbyyr,lsttitleyr,lsttyr,lsttmt,selmon,selyr)

def getdatayr1(indx1,irng,lsttitleyr,lstidpercbyyr,lsttmt,lsttyr,selmon,selyr):
    lsttitleyr.append("Total %")
    idlen = len(idlist)
    entries1 = []
    indx=1
    bval=13
    if indx1==3:
        bval=irng+1

    # Check if site groups are configured
    site_groups = get_site_groups()

    if site_groups:
        # Use configured site groups for vector calculations
        for group in site_groups:
            group_name = group['group']
            group_sites = group['sites']
            entries = []

            # Add each site in the group
            for site_code in group_sites:
                if site_code.upper() in [s.upper() for s in idlist]:
                    site_idx = next((i for i, s in enumerate(idlist) if s.upper() == site_code.upper()), None)
                    if site_idx is not None:
                        lsttcol = []
                        for idx1 in range(0, bval):
                            ip = int(lstidpercbyyr[site_idx][idx1])
                            cval = getcolorcode(ip)
                            lsttcol.append(cval)
                        entries.append({'cid1': idlist[site_idx], 'perc1': lstidpercbyyr[site_idx], 'ccode1': lsttcol, 'nm': idlist[site_idx]})

            # Calculate vector for the group
            if len(entries) >= 1:
                lsttvec = []
                lsttcol3 = []
                for idx1 in range(0, bval):
                    if indx1 == 1 or indx1 == 3:
                        # Get minimum percentage across all sites in group
                        site_percs = []
                        for site_code in group_sites:
                            site_idx = next((i for i, s in enumerate(idlist) if s.upper() == site_code.upper()), None)
                            if site_idx is not None:
                                site_percs.append(int(lstidpercbyyr[site_idx][idx1]))
                        tvec = min(site_percs) if site_percs else 0
                    if indx1 == 2:
                        # For true totals, use radialcnthex AND operation across ALL sites in group
                        tmon = "-"
                        if int(lsttmt[idx1]) < 10:
                            tmon = tmon + "0"
                        # Get hex data for all sites in group and compute totals
                        site_idx_list = []
                        for site_code in group_sites:
                            site_idx = next((i for i, s in enumerate(idlist) if s.upper() == site_code.upper()), None)
                            if site_idx is not None:
                                site_idx_list.append(site_idx)
                        if len(site_idx_list) >= 2:
                            site_data_list = []
                            for sidx in site_idx_list:
                                site_data = getTsuList("SELECT ID, tsuid, tsudate, radialcnt, radialcnthex FROM maintsu where tsuid = '" + idlist[sidx] + "' and tsudate between '"+lsttyr[idx1]+tmon+lsttmt[idx1]+"-01' and '"+ lsttyr[idx1]+tmon+lsttmt[idx1]+"-31'",selmon,selyr)
                                site_data_list.append(site_data)
                            if len(site_data_list) == 2:
                                tvec = dicttoTotals(site_data_list[0], site_data_list[1])
                            else:
                                tvec = dicttoTotalsMultiple(site_data_list)
                        elif len(site_idx_list) == 1:
                            # Single site: total vector = site's own hex bit count
                            site_data = getTsuList("SELECT ID, tsuid, tsudate, radialcnt, radialcnthex FROM maintsu where tsuid = '" + idlist[site_idx_list[0]] + "' and tsudate between '"+lsttyr[idx1]+tmon+lsttmt[idx1]+"-01' and '"+ lsttyr[idx1]+tmon+lsttmt[idx1]+"-31'",selmon,selyr)
                            tvec = 0
                            for sd in site_data:
                                n = int(str(sd['rchex']), 16)
                                count = 0
                                while n:
                                    count += n & 1
                                    n >>= 1
                                tvec += count
                        else:
                            tvec = 0
                    lsttvec.append(tvec)
                    cval3 = getcolorcode(tvec)
                    lsttcol3.append(cval3)
                entries.append({'cid1': "Total Vector", 'perc1': lsttvec, 'ccode1': lsttcol3, 'nm': "Vector" + str(indx)})
                indx = indx + 1

            if entries:
                entries1.append(entries)

        # Add any sites not in any group as standalone
        grouped_sites = set()
        for group in site_groups:
            for site_code in group['sites']:
                grouped_sites.add(site_code.upper())

        for idx, site_code in enumerate(idlist):
            if site_code.upper() not in grouped_sites:
                lsttcol = []
                for idx1 in range(0, bval):
                    ip = int(lstidpercbyyr[idx][idx1])
                    cval = getcolorcode(ip)
                    lsttcol.append(cval)
                entries1.append([{'cid1': site_code, 'perc1': lstidpercbyyr[idx], 'ccode1': lsttcol, 'nm': site_code}])

    else:
        # Fallback: Process sites in sequential pairs (original behavior)
        idx = 0
        while idx < idlen:
            entries = []
            lsttvec = []
            lsttcol1 = []
            lsttcol2 = []
            lsttcol3 = []

            # Check if we have a pair or just one site left
            has_pair = (idx + 1) < idlen

            for idx1 in range(0,bval):
                ip1 = int(lstidpercbyyr[idx][idx1])
                ip2 = int(lstidpercbyyr[(idx+1)][idx1]) if has_pair else 0
                if indx1==1 or indx1==3:
                    if has_pair:
                        if ip1>ip2:
                            tvec = ip2
                        else:
                            tvec = ip1
                    else:
                        tvec = ip1  # No pair, just use first site's value
                if indx1==2:
                    tmon = "-"
                    if int(lsttmt[idx1]) < 10:
                        tmon  = tmon + "0"
                    a=getTsuList("SELECT ID, tsuid, tsudate, radialcnt, radialcnthex FROM maintsu where tsuid = '" + idlist[idx] + "' and tsudate between '"+lsttyr[idx1]+tmon+lsttmt[idx1]+"-01' and '"+ lsttyr[idx1]+tmon+lsttmt[idx1]+"-31'",selmon,selyr)
                    if has_pair:
                        b=getTsuList("SELECT ID, tsuid, tsudate, radialcnt, radialcnthex FROM maintsu where tsuid = '" + idlist[(idx+1)] + "' and tsudate between '"+lsttyr[idx1]+tmon+lsttmt[idx1]+"-01' and '"+ lsttyr[idx1]+tmon+lsttmt[idx1]+"-31'",selmon,selyr)
                        tvec=dicttoTotals(a,b)
                    else:
                        tvec = 0  # No pair for vector calculation
                lsttvec.append(tvec)
                cval1 = getcolorcode(ip1)
                lsttcol1.append(cval1)
                cval2 = getcolorcode(ip2) if has_pair else '#0066CC'
                lsttcol2.append(cval2)
                cval3 = getcolorcode(tvec)
                lsttcol3.append(cval3)
            entries.append({'cid1':idlist[idx],'perc1':lstidpercbyyr[idx],'ccode1':lsttcol1,'nm':idlist[idx]})
            if has_pair:
                entries.append({'cid1':idlist[(idx+1)],'perc1':lstidpercbyyr[(idx+1)],'ccode1':lsttcol2,'nm':idlist[(idx+1)]})
                indx=indx+1
                entries.append({'cid1':"Total Vector",'perc1':lsttvec,'ccode1':lsttcol3,'nm':("Vector"+str(indx))})
            else:
                # Odd site - show it standalone without vector row
                pass
            entries1.append(entries)
            idx += 2

    totent=[]
    for item1 in entries1:
        for item2 in item1:
            itot1 = 0
            ent1 = item2.get('perc1')
            for iv in ent1:
                itot1=itot1+float(iv)
            itotperc1 =  0
            if itot1 > 0:
                itotperc1 = int(math.ceil(float(itot1/bval)))
            totent.append({'cid1':item2.get('cid1'),'pdata':item2,'perc1':str(itotperc1),'cod1':getcolorcode(itotperc1),'nm1':item2.get('nm')})
    return totent

def gettmon(i):
    tmon = "-"
    if i < 10:
        tmon = "-0"
    return tmon+str(i)

def getmthdays(i,selyr):
    crows = int(lstmon[(i-1)])
    if i == 2 and is_leap_year(selyr):
        crows = 29
    return crows

def is_leap_year(year): #Determine whether a year is a leap year.
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)

def getTotSumList(val,selyr,i):
    crows=getmthdays(i,int(selyr))
    srow,stat = dboper("SELECT radialcnt FROM maintsu where lower(tsuid)='"+val.lower()+"' and tsudate between '"+str(selyr)+gettmon(i)+"-01"+"' and '"+str(selyr)+gettmon(i)+"-"+str(crows)+"' and flg = 0 order by tsudate",1)
    iperc = 0
    for row1 in srow:
        iperc=iperc+int(row1[0])
    return str(iperc)

@app.route("/getalldatabyyr2", methods=['POST'])
def getalldatabyyr2():
    # Refresh site list from database before processing
    refresh_idlist()
    data1 = request.get_json(silent=True)
    selmon = int(data1["mon"])
    selyr = int(data1["yr"])
    selmon1 = int(data1["mon1"])
    selyr1 = int(data1["yr1"])

    # OPTIMIZED: Batch fetch all percentage data for the date range
    perc_data = batch_get_perctsu_data(selyr, selmon, selyr1, selmon1)

    lstidpercbyyr = []
    lsttitleyr = []
    irng = 0

    # Build title list once (calculate irng)
    if selyr1 > selyr:
        irng = (12 - selmon + selmon1)
        for i in range(selmon, 13):
            lsttitleyr.append(str(selyr) + ' ' + lstmn1[i-1])
        if (selyr1 - selyr) > 1:
            irng = irng + ((selyr1 - selyr - 1) * 12)
            for yr in range(selyr + 1, selyr1):
                for j in range(1, 13):
                    lsttitleyr.append(str(yr) + ' ' + lstmn1[j-1])
        for i in range(1, selmon1 + 1):
            lsttitleyr.append(str(selyr1) + ' ' + lstmn1[i-1])
    else:
        irng = selmon1 - selmon
        for i in range(selmon, selmon1 + 1):
            lsttitleyr.append(str(selyr) + ' ' + lstmn1[i-1])

    # Build data for each site using pre-fetched data
    for val in idlist:
        lstidperc = []
        site_upper = val.upper()

        if selyr1 > selyr:
            for i in range(selmon, 13):
                lstidperc.append(perc_data.get((site_upper, selyr, i), '0'))
            if (selyr1 - selyr) > 1:
                for yr in range(selyr + 1, selyr1):
                    for j in range(1, 13):
                        lstidperc.append(perc_data.get((site_upper, yr, j), '0'))
            for i in range(1, selmon1 + 1):
                lstidperc.append(perc_data.get((site_upper, selyr1, i), '0'))
        else:
            for i in range(selmon, selmon1 + 1):
                lstidperc.append(perc_data.get((site_upper, selyr, i), '0'))

        lstidpercbyyr.append(lstidperc)

    totent = getdatayr1(3, irng, lsttitleyr, lstidpercbyyr, [], [], selmon, selyr)
    return json.dumps({'sel': 'getalldatabyyr2', 'stat': "success", 'percdata': totent, 'totpercdata': totent, 'idlist': idlist, 'cmon': str(lstmn[selmon-1]), 'cyr': str(selyr), 'cmon1': str(lstmn[selmon1-1]), 'cyr1': str(selyr1), 'pyr': str(selyr-1), 'titl': lsttitleyr, 'rng': (irng + 3)})

def dicttoTotals(a,b):
    Totsum=0
    if len(a)==len(b):
        for i in range(len(a)):
            bits=int(realTotals(a[i]['rchex'],b[i]['rchex']))
            Totsum=Totsum+bits
    return Totsum

def dicttoTotalsMultiple(site_data_list):
    """Calculate totals for 2 or more sites using chained AND operations.
    site_data_list: list of site data arrays from getTsuList"""
    if len(site_data_list) < 2:
        return 0

    # Start with first two sites
    Totsum = 0
    base_data = site_data_list[0]

    # Check all arrays have same length
    base_len = len(base_data)
    if not all(len(sd) == base_len for sd in site_data_list):
        return 0

    for i in range(base_len):
        # Start with first site's hex value
        result_hex = base_data[i]['rchex']
        # AND with each subsequent site's hex value
        for site_data in site_data_list[1:]:
            result_hex = hex(int(result_hex, 16) & int(site_data[i]['rchex'], 16))
        # Count bits in final result
        n = int(str(result_hex), 16)
        count = 0
        while n:
            count += n & 1
            n >>= 1
        Totsum += count
    return Totsum

@app.route("/getallyearlydata", methods=['POST'])
def getallyearlydata():
    data1 = request.get_json(silent=True)
    # Fetch fresh site list from database (not cached global)
    current_sites = get_active_sites()
    current_year = int(datetime.now().year)
    current_month = int(datetime.now().month)

    # OPTIMIZED: Batch fetch all radialcnt sums for years 2022+ in ONE query
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("""
            SELECT UPPER(tsuid), SUBSTR(tsudate, 1, 4) as yr, SUM(CAST(radialcnt AS INTEGER)) as total
            FROM maintsu
            WHERE tsudate >= '2022-01-01' AND flg = 0
            GROUP BY UPPER(tsuid), SUBSTR(tsudate, 1, 4)
        """)
        rows = cursor.fetchall()
        dbcon.close()

        # Build lookup: {(site, year): total_radialcnt}
        yearly_totals = {}
        for row in rows:
            site = row[0] if row[0] else ""
            yr = int(row[1]) if row[1] else 0
            total = row[2] if row[2] else 0
            yearly_totals[(site, yr)] = total
    except Exception as e:
        print("Error fetching yearly totals:", e)
        yearly_totals = {}

    entries = []
    for idx in range(2008, current_year + 1):
        ccolarr = []
        cavgrc = []
        for idstr in current_sites:
            ccmon = 12
            if idx == current_year:
                ccmon = current_month - 1

            # Use pre-fetched data for 2022+
            if idx >= 2022:
                site_upper = idstr.upper()
                iperc = yearly_totals.get((site_upper, idx), 0)
                itotperc1 = "0"
                if iperc > 0:
                    total_hours = 24 * getYrsList(ccmon, idx)
                    if total_hours > 0:
                        itotperc1 = str(int(math.ceil((float(iperc * 100) / float(total_hours)))))
                avgrc = itotperc1
            else:
                # Use hardcoded ent1 data for years before 2022
                avgrc = ""
                for val in ent1:
                    if val["yr"] == str(idx):
                        avgrc = val.get(idstr, "")
                        break

            cavgrc.append(avgrc)
            if avgrc == '':
                vcol = -1
            else:
                vcol = int(avgrc)
            ccolarr.append(getcolorcode(vcol))
        entries.append({"yr": "Year " + str(idx), "idval": idstr, "avgrcperc": cavgrc, "icol": ccolarr})
    return json.dumps({'sel': 'getallyearlydata', 'stat': "success", 'idlist': current_sites, 'entries': entries, 'rng': (len(current_sites) + 1)})

def getYrsList(imon,yr):
    crows1=0
    for idx in range(1,(int(imon)+1)):
        crows1=crows1+getmthdays(idx,int(yr))
    return crows1

def getPercList(sql,yr,istr):
    srow,stat = dboper(sql,1)
    iperc = 0
    for row1 in srow:
        iperc=iperc+int(row1[0])
    itotperc1 =  "0"
    if iperc > 0 and int(yr) >= 2022:
        imon=12
        if(int(yr)==int(datetime.now().year)):
            imon=int(datetime.now().month)-1
        itotperc1 = str(int(math.ceil((float((float(iperc*100))/(float((24*getYrsList(imon,yr)))))))))
    else:
        for val in ent1:
            if(val["yr"]==yr):
                itotperc1 = val[istr]
    return itotperc1

def getcolorcode(val):
    ccode = '#0066CC'
    if (val >= 70):
        ccode = '#80FF00'
    elif (val >= 50 and val < 70):
        ccode = '#FFFF66'
    elif (val < 50 and val >=0):
        ccode = '#FF9933'
    return ccode

@app.route("/credentials", methods=['POST'])
def credentials():
    data = request.json
    upd_qry("UPDATE user SET username='"+data["username"]+"', password='"+data["password"]+"' where usrtype='"+data["utype"]+"'")
    return json.dumps({'api':'credentials','sel':'cred','ret':'success','utype':data["utype"]})

@app.route("/lstcredentials", methods=['POST'])
def lstcredentials():
    entries=[]
    srow,stat = dboper("select * from user where usrtype='user' or usrtype='admin'",1)
    for row1 in srow:
        entries.append({'usernm': row1[0],'pwd': row1[1],'utype': row1[2]})
    return json.dumps({'sel':'lstcred','credentries':entries})

@app.route("/siteactivate", methods=['POST'])
def siteactivate():
    upd_qry("update hfena set hfenadis='1'")
    return json.dumps({'sel':'siteactivate','stat':"success"})

@app.route("/sitedeactivate", methods=['POST'])
def sitedeactivate():
    upd_qry("update hfena set hfenadis='0'")
    return json.dumps({'sel':'sitedeactivate','stat':"success"})

@app.route("/getactivationstat", methods=['POST'])
def getactivationstat():
    sitestat="Current Activation Status: Active"
    srow1, stat1 = dboper("SELECT * from hfena where hfenadis='1'",1)
    if len(srow1) == 0:
        sitestat="Current Activation Status: Non-Active"
    return json.dumps({'sel':'getactivationstat','stat':sitestat})

# ============ SITE MANAGEMENT APIs ============

# API for mobile app to get config (sites list)
@app.route("/getConfig", methods=['GET', 'POST'])
@cross_origin()
def getConfig():
    sites = get_active_sites()
    return json.dumps({'sel':'getConfig', 'sites': sites, 'version': 1})

# Admin API: Get all sites (including inactive)
@app.route("/admin/getSites", methods=['POST'])
def adminGetSites():
    sites = get_all_sites()
    return json.dumps({'sel':'adminGetSites', 'sites': sites})

# Admin API: Add new site
@app.route("/admin/addSite", methods=['POST'])
def adminAddSite():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip()

        if not site_code:
            return json.dumps({'sel':'adminAddSite', 'stat':'error', 'msg':'Site code is required'})

        # Check if site already exists
        srow, stat = dboper("SELECT * FROM sites WHERE site_code='"+site_code+"'", 1)
        if srow and len(srow) > 0:
            return json.dumps({'sel':'adminAddSite', 'stat':'error', 'msg':'Site code already exists'})

        # Get max display_order
        srow2, stat2 = dboper("SELECT MAX(display_order) FROM sites", 1)
        max_order = srow2[0][0] if srow2 and srow2[0][0] else 0

        # Insert new site
        upd_qry("INSERT INTO sites (site_code, display_order, is_active) VALUES ('"+site_code+"', "+str(max_order + 1)+", 1)")

        # Refresh idlist
        refresh_idlist()

        return json.dumps({'sel':'adminAddSite', 'stat':'success', 'msg':'Site added successfully'})
    except Exception as e:
        print("Error adding site:", e)
        return json.dumps({'sel':'adminAddSite', 'stat':'error', 'msg':'Failed to add site'})

# Admin API: Toggle site active/inactive
@app.route("/admin/toggleSite", methods=['POST'])
def adminToggleSite():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip()
        is_active = data.get('is_active', 1)

        if not site_code:
            return json.dumps({'sel':'adminToggleSite', 'stat':'error', 'msg':'Site code is required'})

        upd_qry("UPDATE sites SET is_active="+str(is_active)+" WHERE site_code='"+site_code+"'")

        # Refresh idlist
        refresh_idlist()

        status = "activated" if is_active == 1 else "deactivated"
        return json.dumps({'sel':'adminToggleSite', 'stat':'success', 'msg':'Site '+status+' successfully'})
    except Exception as e:
        print("Error toggling site:", e)
        return json.dumps({'sel':'adminToggleSite', 'stat':'error', 'msg':'Failed to update site'})

# Admin API: Edit site name (updates sites table AND maintsu historical records)
@app.route("/admin/editSite", methods=['POST'])
def adminEditSite():
    try:
        data = request.get_json(silent=True)
        old_code = data.get('old_code', '').strip()
        new_code = data.get('new_code', '').strip()

        if not old_code or not new_code:
            return json.dumps({'sel':'adminEditSite', 'stat':'error', 'msg':'Both old and new site codes are required'})

        # Check if new code already exists (but not same as old)
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("SELECT COUNT(*) FROM sites WHERE site_code=? AND site_code!=?", (new_code, old_code))
        count = cursor.fetchone()[0]
        dbcon.close()

        if count > 0:
            return json.dumps({'sel':'adminEditSite', 'stat':'error', 'msg':'Site code "'+new_code+'" already exists'})

        # Update sites table
        upd_qry("UPDATE sites SET site_code='"+new_code+"' WHERE site_code='"+old_code+"'")

        # Update maintsu table (historical records) - tsuid is stored in uppercase
        upd_qry("UPDATE maintsu SET tsuid='"+new_code.upper()+"' WHERE tsuid='"+old_code.upper()+"'")

        # Also update perctsu table if it exists
        try:
            upd_qry("UPDATE perctsu SET tsuid='"+new_code.upper()+"' WHERE tsuid='"+old_code.upper()+"'")
        except:
            pass  # perctsu table might not exist or have different structure

        # Refresh idlist
        refresh_idlist()

        return json.dumps({'sel':'adminEditSite', 'stat':'success', 'msg':'Site renamed from "'+old_code+'" to "'+new_code+'" successfully. All historical records updated.'})
    except Exception as e:
        print("Error editing site:", e)
        return json.dumps({'sel':'adminEditSite', 'stat':'error', 'msg':'Failed to edit site: '+str(e)})

# Admin API: Reorder sites
@app.route("/admin/reorderSites", methods=['POST'])
def adminReorderSites():
    try:
        data = request.get_json(silent=True)
        sites_order = data.get('sites', [])  # List of site codes in order

        for idx, site_code in enumerate(sites_order):
            upd_qry("UPDATE sites SET display_order="+str(idx + 1)+" WHERE site_code='"+site_code+"'")

        # Refresh idlist
        refresh_idlist()

        return json.dumps({'sel':'adminReorderSites', 'stat':'success', 'msg':'Sites reordered successfully'})
    except Exception as e:
        print("Error reordering sites:", e)
        return json.dumps({'sel':'adminReorderSites', 'stat':'error', 'msg':'Failed to reorder sites'})

# ============ SITE PAIRS MANAGEMENT APIs ============

# Initialize site_pairs table if it doesn't exist
def init_site_pairs_table():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS site_pairs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            group_name VARCHAR(50) NOT NULL,
            site_code VARCHAR(10) NOT NULL,
            UNIQUE(group_name, site_code)
        )''')
        dbcon.commit()
        dbcon.close()
    except Exception as e:
        print("Error initializing site_pairs table:", e)

# Call init on module load
init_site_pairs_table()

# Auto-populate site_pairs with default sequential pairs if table is empty
def auto_populate_default_pairs():
    try:
        sites = get_active_sites()
        if len(sites) < 1:
            return
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        group_num = 1
        idx = 0
        while idx < len(sites):
            if (idx + 1) < len(sites):
                group_name = "Group " + str(group_num)
                cursor.execute("INSERT OR IGNORE INTO site_pairs (group_name, site_code) VALUES (?, ?)", (group_name, sites[idx]))
                cursor.execute("INSERT OR IGNORE INTO site_pairs (group_name, site_code) VALUES (?, ?)", (group_name, sites[idx + 1]))
                idx += 2
            else:
                idx += 1
            group_num += 1
        dbcon.commit()
        dbcon.close()
    except Exception as e:
        print("Error auto-populating site pairs:", e)

# Admin API: Get all site pairs/groups + ungrouped sites
@app.route("/admin/getSitePairs", methods=['POST'])
def adminGetSitePairs():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        # Auto-populate defaults if table is empty
        cursor.execute("SELECT COUNT(*) FROM site_pairs")
        count = cursor.fetchone()[0]
        dbcon.close()
        if count == 0:
            auto_populate_default_pairs()

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("SELECT DISTINCT group_name FROM site_pairs ORDER BY group_name")
        groups = cursor.fetchall()

        result = []
        grouped_sites = set()
        for group in groups:
            group_name = group[0]
            cursor.execute("SELECT site_code FROM site_pairs WHERE group_name=? AND site_code != '__placeholder__' ORDER BY site_code", (group_name,))
            sites = [row[0] for row in cursor.fetchall()]
            result.append({'group': group_name, 'sites': sites})
            for s in sites:
                grouped_sites.add(s.upper())

        dbcon.close()

        # Find ungrouped active sites
        all_active = get_active_sites()
        ungrouped = [s for s in all_active if s.upper() not in grouped_sites]

        return json.dumps({'sel':'adminGetSitePairs', 'pairs': result, 'ungrouped': ungrouped})
    except Exception as e:
        print("Error getting site pairs:", e)
        return json.dumps({'sel':'adminGetSitePairs', 'pairs': [], 'ungrouped': []})

# Admin API: Add site to a group
@app.route("/admin/addSitePair", methods=['POST'])
def adminAddSitePair():
    try:
        data = request.get_json(silent=True)
        group_name = data.get('group_name', '').strip()
        site_code = data.get('site_code', '').strip()

        if not group_name or not site_code:
            return json.dumps({'sel':'adminAddSitePair', 'stat':'error', 'msg':'Group name and site code are required'})

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        try:
            cursor.execute("INSERT INTO site_pairs (group_name, site_code) VALUES (?, ?)", (group_name, site_code))
            # Remove placeholder if it exists for this group
            cursor.execute("DELETE FROM site_pairs WHERE group_name=? AND site_code='__placeholder__'", (group_name,))
            dbcon.commit()
            dbcon.close()
            return json.dumps({'sel':'adminAddSitePair', 'stat':'success', 'msg':'Site "'+site_code+'" added to group "'+group_name+'"'})
        except sqlite3.IntegrityError:
            dbcon.close()
            return json.dumps({'sel':'adminAddSitePair', 'stat':'error', 'msg':'Site is already in this group'})
    except Exception as e:
        print("Error adding site pair:", e)
        return json.dumps({'sel':'adminAddSitePair', 'stat':'error', 'msg':'Failed to add site to group'})

# Admin API: Add a new empty group with auto-generated name
@app.route("/admin/addNewGroup", methods=['POST'])
def adminAddNewGroup():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        # Find next available group number
        cursor.execute("SELECT group_name FROM site_pairs ORDER BY group_name")
        existing = cursor.fetchall()
        existing_names = set(row[0] for row in existing)
        group_num = 1
        while ("Group " + str(group_num)) in existing_names:
            group_num += 1
        new_name = "Group " + str(group_num)
        # Insert a placeholder row (will be replaced when site is added)
        # We use a special placeholder site_code that will be ignored
        cursor.execute("INSERT INTO site_pairs (group_name, site_code) VALUES (?, ?)", (new_name, "__placeholder__"))
        dbcon.commit()
        dbcon.close()
        return json.dumps({'sel':'adminAddNewGroup', 'stat':'success', 'group_name': new_name, 'msg':'Group "'+new_name+'" created'})
    except Exception as e:
        print("Error adding new group:", e)
        return json.dumps({'sel':'adminAddNewGroup', 'stat':'error', 'msg':'Failed to create group'})

# Admin API: Remove site from a group
@app.route("/admin/removeSitePair", methods=['POST'])
def adminRemoveSitePair():
    try:
        data = request.get_json(silent=True)
        group_name = data.get('group_name', '').strip()
        site_code = data.get('site_code', '').strip()

        if not group_name or not site_code:
            return json.dumps({'sel':'adminRemoveSitePair', 'stat':'error', 'msg':'Group name and site code are required'})

        upd_qry("DELETE FROM site_pairs WHERE group_name='"+group_name+"' AND site_code='"+site_code+"'")
        return json.dumps({'sel':'adminRemoveSitePair', 'stat':'success', 'msg':'Site "'+site_code+'" removed from group "'+group_name+'"'})
    except Exception as e:
        print("Error removing site pair:", e)
        return json.dumps({'sel':'adminRemoveSitePair', 'stat':'error', 'msg':'Failed to remove site from group'})

# Admin API: Delete entire group
@app.route("/admin/deleteGroup", methods=['POST'])
def adminDeleteGroup():
    try:
        data = request.get_json(silent=True)
        group_name = data.get('group_name', '').strip()

        if not group_name:
            return json.dumps({'sel':'adminDeleteGroup', 'stat':'error', 'msg':'Group name is required'})

        upd_qry("DELETE FROM site_pairs WHERE group_name='"+group_name+"'")
        return json.dumps({'sel':'adminDeleteGroup', 'stat':'success', 'msg':'Group "'+group_name+'" deleted'})
    except Exception as e:
        print("Error deleting group:", e)
        return json.dumps({'sel':'adminDeleteGroup', 'stat':'error', 'msg':'Failed to delete group'})

# ============ END SITE PAIRS APIs ============

# ============ DATA SOURCE & COMMENTS TABLES ============

# Initialize data_source_log table
def init_data_source_table():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS data_source_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            site_code VARCHAR(10) NOT NULL,
            log_date DATE NOT NULL,
            source_type VARCHAR(10) NOT NULL,
            received_timestamp DATETIME,
            UNIQUE(site_code, log_date)
        )''')
        dbcon.commit()
        dbcon.close()
    except Exception as e:
        print("Error initializing data_source_log table:", e)

# Initialize data_comments table
def init_data_comments_table():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS data_comments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            site_code VARCHAR(10) NOT NULL,
            comment_date DATE NOT NULL,
            comment_text TEXT,
            created_by VARCHAR(50),
            created_at DATETIME,
            UNIQUE(site_code, comment_date)
        )''')
        dbcon.commit()
        dbcon.close()
    except Exception as e:
        print("Error initializing data_comments table:", e)

# Call init on module load
init_data_source_table()
init_data_comments_table()

# API: Get data source summary for a month (most recent per site)
@app.route("/getDataSourceSummary", methods=['POST'])
def getDataSourceSummary():
    try:
        data = request.get_json(silent=True)
        selmon = int(data["mon"])
        selyr = int(data["yr"])

        # Build date range for the month
        first_day = "{}-{:02d}-01".format(selyr, selmon)
        last_day = "{}-{:02d}-31".format(selyr, selmon)

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        # Get most recent source entry per site for the month
        cursor.execute('''
            SELECT site_code, log_date, source_type, received_timestamp
            FROM data_source_log
            WHERE log_date BETWEEN ? AND ?
            AND (site_code, log_date) IN (
                SELECT site_code, MAX(log_date)
                FROM data_source_log
                WHERE log_date BETWEEN ? AND ?
                GROUP BY site_code
            )
            ORDER BY site_code
        ''', (first_day, last_day, first_day, last_day))

        rows = cursor.fetchall()
        dbcon.close()

        sources = []
        for row in rows:
            sources.append({
                'site': row[0],
                'date': row[1],
                'source': row[2],
                'timestamp': row[3]
            })

        return json.dumps({'sel': 'getDataSourceSummary', 'sources': sources})
    except Exception as e:
        print("Error getting data source summary:", e)
        return json.dumps({'sel': 'getDataSourceSummary', 'sources': []})

# API: Get comments for a month
@app.route("/getDataComments", methods=['POST'])
def getDataComments():
    try:
        data = request.get_json(silent=True)
        selmon = int(data["mon"])
        selyr = int(data["yr"])

        first_day = "{}-{:02d}-01".format(selyr, selmon)
        last_day = "{}-{:02d}-31".format(selyr, selmon)

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute('''
            SELECT site_code, comment_date, comment_text, created_by, created_at
            FROM data_comments
            WHERE comment_date BETWEEN ? AND ?
            ORDER BY site_code, comment_date
        ''', (first_day, last_day))

        rows = cursor.fetchall()
        dbcon.close()

        # Build a dict keyed by "SITE_DAY" for easy lookup
        comments = {}
        for row in rows:
            site = row[0]
            day = int(row[1].split('-')[2])  # Extract day from date
            key = site.upper() + "_" + str(day)
            comments[key] = {
                'text': row[2],
                'by': row[3],
                'at': row[4]
            }

        return json.dumps({'sel': 'getDataComments', 'comments': comments})
    except Exception as e:
        print("Error getting data comments:", e)
        return json.dumps({'sel': 'getDataComments', 'comments': {}})

# API: Save/update a comment
@app.route("/saveDataComment", methods=['POST'])
def saveDataComment():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip().upper()
        comment_date = data.get('comment_date', '').strip()  # Format: YYYY-MM-DD
        comment_text = data.get('comment_text', '').strip()
        created_by = data.get('created_by', 'web').strip()

        if not site_code or not comment_date:
            return json.dumps({'sel': 'saveDataComment', 'stat': 'error', 'msg': 'Site code and date are required'})

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        # Use INSERT OR REPLACE (upsert)
        cursor.execute('''
            INSERT OR REPLACE INTO data_comments (site_code, comment_date, comment_text, created_by, created_at)
            VALUES (?, ?, ?, ?, datetime('now'))
        ''', (site_code, comment_date, comment_text, created_by))

        dbcon.commit()
        dbcon.close()

        return json.dumps({'sel': 'saveDataComment', 'stat': 'success', 'msg': 'Comment saved'})
    except Exception as e:
        print("Error saving comment:", e)
        return json.dumps({'sel': 'saveDataComment', 'stat': 'error', 'msg': 'Failed to save comment'})

# API: Log data source (for mobile app)
@app.route("/logDataSource", methods=['POST'])
@cross_origin()
def logDataSource():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip().upper()
        log_date = data.get('log_date', '').strip()  # Format: YYYY-MM-DD
        source_type = data.get('source_type', '').strip()  # AUTO, QR, Manual

        if not site_code or not log_date or not source_type:
            return json.dumps({'sel': 'logDataSource', 'stat': 'error', 'msg': 'Site code, date, and source type are required'})

        # Validate source_type
        if source_type not in ['AUTO', 'QR', 'Manual']:
            return json.dumps({'sel': 'logDataSource', 'stat': 'error', 'msg': 'Invalid source type'})

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        # Use INSERT OR REPLACE (upsert) - but only if new value is not null
        # First check if entry exists
        cursor.execute('SELECT source_type FROM data_source_log WHERE site_code=? AND log_date=?', (site_code, log_date))
        existing = cursor.fetchone()

        if existing:
            # Update only if new source_type is provided
            cursor.execute('''
                UPDATE data_source_log
                SET source_type=?, received_timestamp=datetime('now')
                WHERE site_code=? AND log_date=?
            ''', (source_type, site_code, log_date))
        else:
            cursor.execute('''
                INSERT INTO data_source_log (site_code, log_date, source_type, received_timestamp)
                VALUES (?, ?, ?, datetime('now'))
            ''', (site_code, log_date, source_type))

        dbcon.commit()
        dbcon.close()

        return json.dumps({'sel': 'logDataSource', 'stat': 'success', 'msg': 'Data source logged'})
    except Exception as e:
        print("Error logging data source:", e)
        return json.dumps({'sel': 'logDataSource', 'stat': 'error', 'msg': 'Failed to log data source'})

# ============ END DATA SOURCE & COMMENTS APIs ============

# ============ DATABASE BACKUP & RESTORE APIs ============

# Admin API: Download database backup
@app.route("/admin/downloadBackup", methods=['POST'])
def adminDownloadBackup():
    try:
        backup_data = {}
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        # Backup user table
        cursor.execute("SELECT username, password, usrtype FROM user")
        users = cursor.fetchall()
        backup_data['users'] = [{'username': r[0], 'password': r[1], 'usrtype': r[2]} for r in users]

        # Backup sites table
        cursor.execute("SELECT site_code, display_order, is_active FROM sites ORDER BY display_order")
        sites = cursor.fetchall()
        backup_data['sites'] = [{'site_code': r[0], 'display_order': r[1], 'is_active': r[2]} for r in sites]

        # Backup site_pairs table
        cursor.execute("SELECT group_name, site_code FROM site_pairs ORDER BY group_name, site_code")
        pairs = cursor.fetchall()
        backup_data['site_pairs'] = [{'group_name': r[0], 'site_code': r[1]} for r in pairs]

        # Backup hfena (activation status) table
        cursor.execute("SELECT hfenadis FROM hfena")
        hfena = cursor.fetchall()
        backup_data['hfena'] = [{'hfenadis': r[0]} for r in hfena]

        # Backup data_source_log table
        try:
            cursor.execute("SELECT site_code, log_date, source_type, received_timestamp FROM data_source_log")
            source_logs = cursor.fetchall()
            backup_data['data_source_log'] = [{'site_code': r[0], 'log_date': r[1], 'source_type': r[2], 'received_timestamp': r[3]} for r in source_logs]
        except:
            backup_data['data_source_log'] = []

        # Backup data_comments table
        try:
            cursor.execute("SELECT site_code, comment_date, comment_text, created_by, created_at FROM data_comments")
            comments = cursor.fetchall()
            backup_data['data_comments'] = [{'site_code': r[0], 'comment_date': r[1], 'comment_text': r[2], 'created_by': r[3], 'created_at': r[4]} for r in comments]
        except:
            backup_data['data_comments'] = []

        dbcon.close()

        # Generate timestamp for filename
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        return json.dumps({
            'sel': 'adminDownloadBackup',
            'stat': 'success',
            'backup': backup_data,
            'timestamp': timestamp
        })
    except Exception as e:
        print("Error creating backup:", e)
        return json.dumps({'sel': 'adminDownloadBackup', 'stat': 'error', 'msg': 'Failed to create backup: ' + str(e)})

# Admin API: Restore database from backup
@app.route("/admin/restoreBackup", methods=['POST'])
def adminRestoreBackup():
    try:
        data = request.get_json(silent=True)
        backup_data = data.get('backup', {})

        if not backup_data:
            return json.dumps({'sel': 'adminRestoreBackup', 'stat': 'error', 'msg': 'No backup data provided'})

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        restored_tables = []

        # Restore users table
        if 'users' in backup_data and len(backup_data['users']) > 0:
            cursor.execute("DELETE FROM user")
            for user in backup_data['users']:
                cursor.execute("INSERT INTO user (username, password, usrtype) VALUES (?, ?, ?)",
                             (user['username'], user['password'], user['usrtype']))
            restored_tables.append('users')

        # Restore sites table
        if 'sites' in backup_data and len(backup_data['sites']) > 0:
            cursor.execute("DELETE FROM sites")
            for site in backup_data['sites']:
                cursor.execute("INSERT INTO sites (site_code, display_order, is_active) VALUES (?, ?, ?)",
                             (site['site_code'], site['display_order'], site['is_active']))
            restored_tables.append('sites')
            # Refresh idlist
            refresh_idlist()

        # Restore site_pairs table
        if 'site_pairs' in backup_data:
            cursor.execute("DELETE FROM site_pairs")
            for pair in backup_data['site_pairs']:
                cursor.execute("INSERT INTO site_pairs (group_name, site_code) VALUES (?, ?)",
                             (pair['group_name'], pair['site_code']))
            restored_tables.append('site_pairs')

        # Restore hfena table
        if 'hfena' in backup_data and len(backup_data['hfena']) > 0:
            cursor.execute("DELETE FROM hfena")
            for row in backup_data['hfena']:
                cursor.execute("INSERT INTO hfena (hfenadis) VALUES (?)", (row['hfenadis'],))
            restored_tables.append('hfena')

        # Restore data_source_log table
        if 'data_source_log' in backup_data:
            try:
                cursor.execute("DELETE FROM data_source_log")
                for log in backup_data['data_source_log']:
                    cursor.execute("INSERT INTO data_source_log (site_code, log_date, source_type, received_timestamp) VALUES (?, ?, ?, ?)",
                                 (log['site_code'], log['log_date'], log['source_type'], log['received_timestamp']))
                restored_tables.append('data_source_log')
            except Exception as e:
                print("Error restoring data_source_log:", e)

        # Restore data_comments table
        if 'data_comments' in backup_data:
            try:
                cursor.execute("DELETE FROM data_comments")
                for comment in backup_data['data_comments']:
                    cursor.execute("INSERT INTO data_comments (site_code, comment_date, comment_text, created_by, created_at) VALUES (?, ?, ?, ?, ?)",
                                 (comment['site_code'], comment['comment_date'], comment['comment_text'], comment['created_by'], comment['created_at']))
                restored_tables.append('data_comments')
            except Exception as e:
                print("Error restoring data_comments:", e)

        dbcon.commit()
        dbcon.close()

        return json.dumps({
            'sel': 'adminRestoreBackup',
            'stat': 'success',
            'msg': 'Backup restored successfully! Restored tables: ' + ', '.join(restored_tables)
        })
    except Exception as e:
        print("Error restoring backup:", e)
        return json.dumps({'sel': 'adminRestoreBackup', 'stat': 'error', 'msg': 'Failed to restore backup: ' + str(e)})

# Admin API: Pull latest code from GitHub (preserving database)
@app.route("/admin/pullFromGithub", methods=['POST'])
def adminPullFromGithub():
    try:
        # Get the current working directory (where app.py is located)
        repo_dir = os.path.dirname(os.path.abspath(__file__))

        # IMPORTANT: Stash the database file to prevent it from being overwritten
        # This preserves the current database with all user data
        stash_result = subprocess.run(
            ['git', 'stash', 'push', '-u', 'users.db'],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            timeout=10
        )

        # Execute git pull command (will not overwrite stashed users.db)
        result = subprocess.run(
            ['git', 'pull', 'origin', 'main'],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            timeout=30
        )

        # Restore the stashed database file (bring back current database)
        unstash_result = subprocess.run(
            ['git', 'stash', 'pop'],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            timeout=10
        )

        # Check if git pull was successful
        if result.returncode == 0:
            output = result.stdout + result.stderr

            return json.dumps({
                'sel': 'adminPullFromGithub',
                'stat': 'success',
                'msg': 'Code updated successfully from GitHub!',
                'git_output': output,
                'reload_pending': True
            })
        else:
            return json.dumps({
                'sel': 'adminPullFromGithub',
                'stat': 'error',
                'msg': 'Git pull failed: ' + result.stderr
            })
    except subprocess.TimeoutExpired:
        return json.dumps({
            'sel': 'adminPullFromGithub',
            'stat': 'error',
            'msg': 'Git pull timed out after 30 seconds'
        })
    except FileNotFoundError:
        return json.dumps({
            'sel': 'adminPullFromGithub',
            'stat': 'error',
            'msg': 'Git command not found. Please ensure git is installed.'
        })
    except Exception as e:
        print("Error pulling from GitHub:", e)
        return json.dumps({
            'sel': 'adminPullFromGithub',
            'stat': 'error',
            'msg': 'Failed to pull from GitHub: ' + str(e)
        })

# Admin API: Reload PythonAnywhere web app (called after user restores backup)
@app.route("/admin/reloadWebApp", methods=['POST'])
def adminReloadWebApp():
    import threading

    def do_reload():
        """Background thread to perform the reload after response is sent"""
        import time
        time.sleep(1)  # Wait for response to be sent
        try:
            if PYTHONANYWHERE_API_TOKEN:
                reload_url = f'https://www.pythonanywhere.com/api/v0/user/{PYTHONANYWHERE_USERNAME}/webapps/{PYTHONANYWHERE_DOMAIN}/reload/'
                headers = {'Authorization': f'Token {PYTHONANYWHERE_API_TOKEN}'}
                reload_response = requests.post(reload_url, headers=headers, timeout=30)
                print(f"Reload completed with status: {reload_response.status_code}")
        except Exception as e:
            print(f"Background reload error: {e}")

    try:
        if PYTHONANYWHERE_API_TOKEN:
            # Start reload in background thread (non-blocking)
            reload_thread = threading.Thread(target=do_reload)
            reload_thread.daemon = True
            reload_thread.start()

            # Return success immediately (before reload happens)
            return json.dumps({
                'sel': 'adminReloadWebApp',
                'stat': 'success',
                'msg': 'Reload initiated! The web app will restart in a few seconds. Please wait 15-30 seconds, then refresh this page.'
            })
        else:
            return json.dumps({
                'sel': 'adminReloadWebApp',
                'stat': 'error',
                'msg': 'API token not configured. Please reload manually from PythonAnywhere Web tab.'
            })
    except Exception as e:
        print("Error initiating reload:", e)
        return json.dumps({
            'sel': 'adminReloadWebApp',
            'stat': 'error',
            'msg': 'Failed to initiate reload: ' + str(e) + '. Please reload manually from PythonAnywhere Web tab.'
        })

# ============ END DATABASE BACKUP & RESTORE APIs ============

# ============ ATTENDANCE APIs ============

# Mark attendance (from mobile app)
@app.route("/markAttendance", methods=['POST'])
@cross_origin()
def markAttendance():
    try:
        data = request.get_json(silent=True)
        staff_name = data.get('staff_name', '').strip()
        site_code = data.get('site_code', '').strip()
        lat = data.get('latitude', 0)
        lng = data.get('longitude', 0)

        if not staff_name or not site_code:
            return json.dumps({'sel': 'markAttendance', 'stat': 'error', 'msg': 'Name and site are required'})

        if lat == 0 and lng == 0:
            return json.dumps({'sel': 'markAttendance', 'stat': 'error', 'msg': 'GPS coordinates are required'})

        # Check 10 per day limit (same person + same site + same date)
        today_str = date.today().strftime('%Y-%m-%d')
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute(
            "SELECT COUNT(*) FROM attendance WHERE staff_name=? AND site_code=? AND DATE(timestamp)=?",
            (staff_name, site_code, today_str)
        )
        day_count = cursor.fetchone()[0]

        if day_count >= 10:
            dbcon.close()
            return json.dumps({'sel': 'markAttendance', 'stat': 'error', 'msg': 'Attendance already marked 10 times today for this site.'})

        # Get site coordinates for distance calculation
        cursor.execute("SELECT site_lat, site_lng FROM sites WHERE site_code=?", (site_code,))
        site_row = cursor.fetchone()
        distance_m = 0.0
        if site_row and site_row[0] is not None and site_row[1] is not None:
            distance_m = round(haversine_distance(lat, lng, site_row[0], site_row[1]), 1)

        # Insert attendance record
        now_str = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        cursor.execute(
            "INSERT INTO attendance (staff_name, site_code, latitude, longitude, timestamp, distance_m) VALUES (?, ?, ?, ?, ?, ?)",
            (staff_name, site_code, lat, lng, now_str, distance_m)
        )
        dbcon.commit()

        # Enforce 50 records per site cap - delete oldest if exceeded
        cursor.execute("SELECT COUNT(*) FROM attendance WHERE site_code=?", (site_code,))
        total = cursor.fetchone()[0]
        if total > 50:
            excess = total - 50
            cursor.execute(
                "DELETE FROM attendance WHERE id IN (SELECT id FROM attendance WHERE site_code=? ORDER BY timestamp ASC LIMIT ?)",
                (site_code, excess)
            )
            dbcon.commit()

        dbcon.close()

        # Format distance for display
        if distance_m >= 1000:
            dist_str = str(round(distance_m / 1000, 1)) + " km"
        else:
            dist_str = str(int(distance_m)) + " m"

        return json.dumps({
            'sel': 'markAttendance',
            'stat': 'success',
            'msg': 'Attendance marked successfully!',
            'distance': dist_str,
            'distance_m': distance_m,
            'timestamp': now_str,
            'count_today': day_count + 1
        })
    except Exception as e:
        print("Error marking attendance:", e)
        return json.dumps({'sel': 'markAttendance', 'stat': 'error', 'msg': 'Failed to mark attendance: ' + str(e)})

# Get attendance records (admin - filter by site)
@app.route("/admin/getAttendance", methods=['POST'])
def adminGetAttendance():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip()

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        if site_code:
            cursor.execute(
                "SELECT id, staff_name, site_code, latitude, longitude, timestamp, distance_m FROM attendance WHERE site_code=? ORDER BY timestamp DESC LIMIT 50",
                (site_code,)
            )
        else:
            cursor.execute(
                "SELECT id, staff_name, site_code, latitude, longitude, timestamp, distance_m FROM attendance ORDER BY timestamp DESC LIMIT 50"
            )

        rows = cursor.fetchall()
        dbcon.close()

        records = []
        for row in rows:
            dist_m = row[6] if row[6] else 0
            if dist_m >= 1000:
                dist_str = str(round(dist_m / 1000, 1)) + " km"
            else:
                dist_str = str(int(dist_m)) + " m"
            records.append({
                'id': row[0],
                'staff_name': row[1],
                'site_code': row[2],
                'latitude': row[3],
                'longitude': row[4],
                'timestamp': row[5],
                'distance_m': dist_m,
                'distance_str': dist_str
            })

        return json.dumps({'sel': 'adminGetAttendance', 'stat': 'success', 'records': records})
    except Exception as e:
        print("Error getting attendance:", e)
        return json.dumps({'sel': 'adminGetAttendance', 'stat': 'error', 'records': []})

# Get attendance records for user view (filter by site, for verification)
@app.route("/getAttendance", methods=['POST'])
@cross_origin()
def getAttendance():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip()

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()

        if site_code:
            cursor.execute(
                "SELECT staff_name, site_code, latitude, longitude, timestamp, distance_m FROM attendance WHERE site_code=? ORDER BY timestamp DESC LIMIT 50",
                (site_code,)
            )
        else:
            cursor.execute(
                "SELECT staff_name, site_code, latitude, longitude, timestamp, distance_m FROM attendance ORDER BY timestamp DESC LIMIT 50"
            )

        rows = cursor.fetchall()
        dbcon.close()

        records = []
        for row in rows:
            dist_m = row[5] if row[5] else 0
            if dist_m >= 1000:
                dist_str = str(round(dist_m / 1000, 1)) + " km"
            else:
                dist_str = str(int(dist_m)) + " m"
            records.append({
                'staff_name': row[0],
                'site_code': row[1],
                'latitude': row[2],
                'longitude': row[3],
                'timestamp': row[4],
                'distance_str': dist_str
            })

        return json.dumps({'sel': 'getAttendance', 'stat': 'success', 'records': records})
    except Exception as e:
        print("Error getting attendance:", e)
        return json.dumps({'sel': 'getAttendance', 'stat': 'error', 'records': []})

# Get attendance summary - latest entry per site (for user view)
@app.route("/getAttendanceSummary", methods=['POST'])
@cross_origin()
def getAttendanceSummary():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        # Get all active sites
        cursor.execute("SELECT site_code, site_lat, site_lng FROM sites WHERE is_active=1 ORDER BY display_order")
        sites = cursor.fetchall()

        summary = []
        for site in sites:
            site_code = site[0]
            # Get most recent attendance for this site
            cursor.execute(
                "SELECT staff_name, latitude, longitude, timestamp, distance_m FROM attendance WHERE site_code=? ORDER BY timestamp DESC LIMIT 1",
                (site_code,)
            )
            row = cursor.fetchone()
            if row:
                dist_m = row[4] if row[4] else 0
                if dist_m >= 1000:
                    dist_str = str(round(dist_m / 1000, 1)) + " km"
                else:
                    dist_str = str(int(dist_m)) + " m"
                summary.append({
                    'site_code': site_code,
                    'staff_name': row[0],
                    'latitude': row[1],
                    'longitude': row[2],
                    'timestamp': row[3],
                    'distance_m': dist_m,
                    'distance_str': dist_str
                })
            else:
                summary.append({
                    'site_code': site_code,
                    'staff_name': '-',
                    'latitude': 0,
                    'longitude': 0,
                    'timestamp': 'No records',
                    'distance_m': 0,
                    'distance_str': '-'
                })

        dbcon.close()
        return json.dumps({'sel': 'getAttendanceSummary', 'stat': 'success', 'summary': summary})
    except Exception as e:
        print("Error getting attendance summary:", e)
        return json.dumps({'sel': 'getAttendanceSummary', 'stat': 'error', 'summary': []})

# Admin API: Update site coordinates
@app.route("/admin/updateSiteCoords", methods=['POST'])
def adminUpdateSiteCoords():
    try:
        data = request.get_json(silent=True)
        site_code = data.get('site_code', '').strip()
        site_lat = data.get('site_lat')
        site_lng = data.get('site_lng')

        if not site_code:
            return json.dumps({'sel': 'adminUpdateSiteCoords', 'stat': 'error', 'msg': 'Site code is required'})

        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("UPDATE sites SET site_lat=?, site_lng=? WHERE site_code=?", (site_lat, site_lng, site_code))
        dbcon.commit()
        dbcon.close()

        return json.dumps({'sel': 'adminUpdateSiteCoords', 'stat': 'success', 'msg': 'Coordinates updated for ' + site_code})
    except Exception as e:
        print("Error updating site coords:", e)
        return json.dumps({'sel': 'adminUpdateSiteCoords', 'stat': 'error', 'msg': 'Failed to update coordinates'})

# Admin API: Get sites with coordinates
@app.route("/admin/getSitesWithCoords", methods=['POST'])
def adminGetSitesWithCoords():
    try:
        dbcon = sqlite3.connect(DB_PATH)
        cursor = dbcon.cursor()
        cursor.execute("SELECT id, site_code, display_order, is_active, site_lat, site_lng FROM sites ORDER BY display_order")
        rows = cursor.fetchall()
        dbcon.close()

        sites = []
        for row in rows:
            sites.append({
                'id': row[0],
                'code': row[1],
                'order': row[2],
                'active': row[3],
                'lat': row[4],
                'lng': row[5]
            })

        return json.dumps({'sel': 'adminGetSitesWithCoords', 'stat': 'success', 'sites': sites})
    except Exception as e:
        print("Error getting sites with coords:", e)
        return json.dumps({'sel': 'adminGetSitesWithCoords', 'stat': 'error', 'sites': []})

# ============ END ATTENDANCE APIs ============

# DEBUG ENDPOINT: Check environment variables (TEMPORARY - for troubleshooting)
@app.route("/admin/debugEnv", methods=['GET'])
def adminDebugEnv():
    import os
    token = os.environ.get('PA_API_TOKEN', '')
    return json.dumps({
        'PA_USERNAME': os.environ.get('PA_USERNAME', 'NOT_SET'),
        'PA_DOMAIN': os.environ.get('PA_DOMAIN', 'NOT_SET'),
        'PA_API_TOKEN_LENGTH': len(token),
        'PA_API_TOKEN_FIRST_10': token[:10] if token else 'NOT_SET',
        'PYTHONANYWHERE_USERNAME': PYTHONANYWHERE_USERNAME,
        'PYTHONANYWHERE_DOMAIN': PYTHONANYWHERE_DOMAIN,
        'PYTHONANYWHERE_API_TOKEN_LENGTH': len(PYTHONANYWHERE_API_TOKEN),
        'PYTHONANYWHERE_API_TOKEN_FIRST_10': PYTHONANYWHERE_API_TOKEN[:10] if PYTHONANYWHERE_API_TOKEN else 'EMPTY'
    })

def realTotals(a,b):
    e=hex(int(a, 16) & int(b, 16))
    n=int(str(e),16)
    count = 0
    while (n):
        count += n & 1
        n >>= 1
    return count

if __name__ == "__main__":
    app.run(debug = True)