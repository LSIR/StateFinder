"""
Classes here deal with reading and writing time series as triples
 (timestart, timestop, value).
Value is always a float so as to accomodate for raw values as well as symbols.
Read/write from any text file, using "," as separators and one triple per line.
"""

class FileDataSource:

    def __init__(self, finput, foutput):
        """
        the in/out filenames can be None if you don't use the load or save
        """
        self.data = []
        self.finput = finput
        self.foutput = foutput


    def load(self, dim=1):
        """
        load the input file into the data array
        """
        inputf = open(self.finput, 'r')
        for row in inputf.readlines():
            row = row.split(',')
            self.data.append([int(row[0]),
                              int(row[1])]+[float(x) for x in row[2:2+dim]])
        inputf.close()
        self.data = sorted(self.data, key=lambda s: s[0])


    def save(self):
        """
        save the data array into the output file
        """
        inputf = open(self.foutput, 'w')
        for row in sorted(self.data, key=lambda s: s[0]):
            inputf.write("%d,%d,%d\n"%(row[0], row[1], int(row[2])))
        inputf.close()
    

    def save_to(self, filename, floating=False):
        """
        save the data array into the specified file
        """
        outputf = open(filename, 'w')
        if floating:
            for row in self.data:
                outputf.write("%d,%d,%f\n"%(row[0], row[1], row[2]))
        else:
            for row in self.data:
                outputf.write("%d,%d,%d\n"%(row[0], row[1], int(row[2])))
        outputf.close()
