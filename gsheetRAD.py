import gspread
import sqlite3
#from oauth2client.service_account import ServiceAccountCredentials
from oauth2client.service_account import ServiceAccountCredentials

from calendar import monthrange
from datetime import datetime, timedelta

# Function to insert data into Google Sheets based on the provided month and year
def insert_data_to_gsheet(month, year):
    # Google Sheets credentials data
    credentials_file = '/home/hfradarsite/deploy/bigbask-395319-a062acf42b85.json'

    # Authenticate using the credentials JSON data
    scope = ["https://spreadsheets.google.com/feeds", "https://www.googleapis.com/auth/drive"]
    credentials = ServiceAccountCredentials.from_json_keyfile_name(credentials_file, scope)
    client = gspread.authorize(credentials)

    # Open the Google Sheet named 'HF Radar Monthly Report 2022-25'
    sheet_name = 'HF Radar Monthly Report 2022-25'  # Name of the sheet to open
    sheet = client.open(sheet_name).worksheet('DB')  # Open the 'DB' sheet

    # SQLite database connection
    dbcon = sqlite3.connect('/home/hfradarsite/deploy/users.db')
    cursor = dbcon.cursor()

    # Get the first and last dates of the month
    first_day_of_month = "{}-{:02d}-01".format(year, month)
    last_day_of_month = "{}-{:02d}-{:02d}".format(year, month, monthrange(year, month)[1])

    # Query to fetch data from the maintsu table
    query = """
    SELECT dates.tsudate,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'cuda' THEN m.radialcnt END), 0) AS Cuda,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'kalp' THEN m.radialcnt END), 0) AS Kalp,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'mach' THEN m.radialcnt END), 0) AS Mach,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'yanm' THEN m.radialcnt END), 0) AS Yanm,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'wasi' THEN m.radialcnt END), 0) AS Wasi,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'jgri' THEN m.radialcnt END), 0) AS Jgri,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'gopa' THEN m.radialcnt END), 0) AS Gopa,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'puri' THEN m.radialcnt END), 0) AS Puri,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'ptbl' THEN m.radialcnt END), 0) AS Ptbl,
           COALESCE(MAX(CASE WHEN LOWER(m.tsuid) = 'htby' THEN m.radialcnt END), 0) AS Htby
    FROM ( -- Creating a table of all dates
        SELECT DISTINCT tsudate
        FROM maintsu
        WHERE tsudate BETWEEN '{}' AND '{}'
    ) AS dates
    LEFT JOIN maintsu AS m
        ON dates.tsudate = m.tsudate
        AND LOWER(m.tsuid) IN ('cuda', 'kalp', 'mach', 'yanm', 'wasi', 'jgri', 'gopa', 'puri', 'ptbl', 'htby')
    GROUP BY dates.tsudate
    ORDER BY dates.tsudate;
    """.format(first_day_of_month, last_day_of_month)

    # Execute the query and fetch data
    cursor.execute(query)
    result = cursor.fetchall()

    # Convert the result into the format required for Google Sheets
    data_to_insert = []
    for row in result:
        numeric_row = [int(value) if isinstance(value, int) else float(value) for value in row[1:]]
        data_to_insert.append(numeric_row)  # Exclude the date column, as it will be found in Sheets

    # Function to paste data next to the found date
    def paste_data_next_to_date(sheet, data, date_value):
        # Get all values from column A (the 'Date' column)
        date_column_values = sheet.col_values(1)

        # Check if the date is in the list and get the row index
        if date_value in date_column_values:
            row_index = date_column_values.index(date_value) + 1  # Add 1 to match 1-based index in Sheets

            # Calculate the range to update, dynamically adjusting for the number of columns in the data
            num_columns = len(data[0])  # Number of columns in the data (e.g., 10 columns)
            num_rows = len(data)

            # Calculate the last column to update (e.g., 'K' for 10 columns starting from 'B')
            end_col_index = chr(ord('C') + num_columns - 1)  # This will calculate the last column (e.g., 'K')

            # Build the range string (e.g., C1056:L1068)
            range_to_update = 'C{}:{}{}'.format(row_index, end_col_index, row_index + num_rows - 1)

            # Update the sheet with the data
            sheet.update(range_to_update, data)
            print("Data successfully inserted into range {}.".format(range_to_update))
        else:
            print("Date {} not found in the sheet.".format(date_value))

    # Format the date_value to match the desired format (e.g., '01/10/2024')
    date_value = "01/{:02d}/{}".format(month, year)

    # Call the function to insert the data
    paste_data_next_to_date(sheet, data_to_insert, date_value)

    # Close the database connection
    dbcon.close()

# Example usage of the function with month and year as inputs
#insert_data_to_gsheet(10, 2024)

def get_previous_month_and_year():
    today = datetime.today()
    first_day_of_current_month = today.replace(day=1)
    last_month = first_day_of_current_month - timedelta(days=1)
    previous_month = last_month.month
    previous_year = last_month.year
    return previous_month, previous_year

previous_month, previous_year = get_previous_month_and_year()
#insert_data_to_gsheet(previous_month, previous_year)
