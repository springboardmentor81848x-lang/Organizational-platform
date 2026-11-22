import pymysql

conn = pymysql.connect(host='localhost', port=3306, user='root', password='97@MySql#neha', database='workforce_knowledge_intelligence')
cur = conn.cursor()
cur.execute("SHOW TABLES LIKE '%message%';")
print("message tables:", cur.fetchall())
cur.execute("SHOW TABLES LIKE '%follow%';")
print("follow tables:", cur.fetchall())
cur.execute("SHOW TABLES LIKE '%session%';")
print("session tables:", cur.fetchall())
cur.execute("SHOW TABLES;")
all_tables = [r[0] for r in cur.fetchall()]
print(f"Total tables: {len(all_tables)}")
print("All tables:", all_tables)
conn.close()
