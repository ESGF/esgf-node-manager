
class TimeStore():

    def __init__(self):
        self.filename = ""
        self.ts = 0

    def write(self):
        
        outf = open(self.filename, "w")
        outf.write(str(self.ts))
        outf.close()


    def restore(self):
        inf = open(self.filename)

        x = inf.read()
        
        if len(x) < 8:
            print "problen with timestamp.  Please fetch from distribution mirror"
            exit (1)

        self.ts = int(x)
        inf.close()
    
    
ts_instance = TimeStore()

def get_instance():
    return ts_instance

