from http.server import BaseHTTPRequestHandler, HTTPServer 
import os
from os import path
import re
import json

WORK_DIR = os.environ['DSS_DIR']
RES_PATH = path.join(WORK_DIR, "workdir/result/testWarehouse/")

class server_handler(BaseHTTPRequestHandler):
    
    ext_to_type = {"html": "text/html", "js": "application/javascript", "css": "text/css", "png": "image/png", "gif": "image/gif", 
                   "json": "application/json", "txt": "text/plain", "ts": "application/x-linguist", 
                   "ico": "image/vnd.microsoft.icon", "map": "application/javascript"}
    
    def send_resource(self, resource_dir):
        ext_name = re.search(".([a-z|A-Z]*?)$", resource_dir).group(1)
        resource = open(resource_dir, "rb")
        self.send_response(200)
        self.send_header("Content-type", server_handler.ext_to_type[ext_name])
        self.send_header("charset", "utf-8")
        self.end_headers()
        self.wfile.write(resource.read()) 
        resource.close()
    
    def do_GET(self):
        if self.path == "/":
            self.send_resource("web/index.html")
        elif self.path == "/meta.json":
            self.send_resource(path.join(RES_PATH, self.path[1:]))
        elif self.path[:6] == "/frame":
            # parse parameters
            snapshot = self.get_snapshot_data(int(self.path[9:]))
            if not snapshot:
                snapshot = "{\"end\":1}"
            self.send_response(200)
            self.send_header("Content-type", 'text/plain')
            self.send_header("charset", "utf-8")
            self.end_headers()
            self.wfile.write(snapshot.encode())
        else:
            self.send_resource(path.join("./web", self.path[1:]))
            
    def do_POST(self):
        self.do_GET()
        
    def get_snapshot_data(self, t):
        frame = frame_file.readline()
        if not frame:
            return ""
        res = dict()
        res["frame"] = frame.strip()
        frame = frame.strip()
        if t % log_seg == 0:
            line = stats_file.readline()
            line = line.strip()
            ppr_rwr = line.split(' ')
            res["ppr"] = int(float(ppr_rwr[0]) * 100)
            res["rwr"] = int(float(ppr_rwr[1]) * 100)
            line = stats_file.readline()
            line = line.strip()
            breakdown = line.split(' ')
            res["area"] = [float(each) for each in breakdown]
            bar_data = []
            for _ in range(10):
                line = stats_file.readline()
                line = line.strip()
                one_rack = line.split(' ')
                bar_data.append([int(each) for each in one_rack])
            res["bar"] = bar_data
            
            heat_data = []
            for i in range(height):
                line = stats_file.readline()
                line = line.strip()
                freqs = line.split(' ')
                for j in range(len(freqs)):
                    heat_data.append([j, height - i, int(freqs[j])])
            res["heat"] = heat_data            
            
        return json.dumps(res)

def run():
    port = 5000
    print(f"Starting server at port {port}")
    server_address = ("localhost", port)
    
    httpd = HTTPServer(server_address, server_handler)
    httpd.serve_forever()


if __name__ == '__main__':
    frame_file = open(path.join(RES_PATH, "frames.txt"))
    stats_file = open(path.join(RES_PATH, "stats.txt"))
    print(path.join(RES_PATH, "stats.txt"))
    with open(path.join(RES_PATH, "meta.json")) as f:
        meta_file = json.load(f)
        log_seg = meta_file["stat_freq"]
        width = meta_file["width"]
        height = meta_file["height"]
    try:
        run()
    except Exception as e:
        print(e)
        frame_file.close()
        stats_file.close()