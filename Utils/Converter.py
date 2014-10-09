"""
here are all the classes used for reading or writing
or converting files with special formats
and also extracting statistics from data files

"""

import pickle
import StateFinder.LookupTable


class FromCSV:

    def convert(self,src,dst,func):
        """
        take a csv file and build triples by applying a given function
        """
        inputf = open(src, 'r')
        outputf = open(dst, 'w')
        last = []
        rows = inputf.readlines()
        for row in rows:
            row = row[:-1].split(',')
            if last:
                outputf.write("%s,%s,%s\n"%(last[0], row[0], func(last[1:])))
            last = row
        outputf.close()
        inputf.close()


class ToRaw:

    def convert(self, data, dst, model, level):
        """
        convert a symbolic time series back to raw values
        """
        inputf = open(model, 'r')
        lookup = pickle.load(inputf) 
        inputf.close()
        dat = []
        previous = None
        inputf = open(data, 'r')
        outputf = open(dst, 'w')
        if level == 2:
            level = len(lookup)-1
        rlv = level
        if isinstance(lookup[0], LookupTable.SpclustSymbolLookupTable):
            rlv = level-1
        for row in inputf.readlines():
            if row[:-1] == "null":
                continue
            row = row[:-1].split(',')
            buf = [row]
            for j in range(rlv+1):
                temp = []
                for sym in buf:
                    if isinstance(lookup[level-j],
                                  LookupTable.ClusterSparseLookupTable):
                        temp.extend(lookup[level-j].revert(sym, previous))
                        previous = int(float(sym[2]))
                    else:
                        temp.extend(lookup[level-j].revert(sym))
                buf = temp
            dat.extend(buf)
        if isinstance(lookup[0], LookupTable.SpclustSymbolLookupTable):    
            dat = lookup[0].revert(dat)
        for sym in dat:
            outputf.write("%s,%s,%s\n"%(str(sym[0]), str(sym[1]),
                                         ",".join(map(str, sym[2:]))))
        inputf.close()
        outputf.close()      

class ToSymbols:

    def convert(self, data, dst, model, level):
        """
        convert a symbolic time series back to lower level symbolic time series
        """
        inputf = open(model, 'r')
        lookup = pickle.load(inputf)
        inputf.close()
        dat = []
        previous = None
        inputf = open(data, 'r')
        outputf = open(dst, 'w')
        rlv = level
        for row in inputf.readlines():
            if row[:-1] == "null":
                continue
            row = row[:-1].split(',')
            buf = [row]
            for j in range(rlv):
                temp = []
                for sym in buf:
                    if isinstance(lookup[level-j],
                                  LookupTable.ClusterSparseLookupTable):
                        temp.extend(lookup[level-j].revert(sym, previous))
                        previous = int(float(sym[2]))
                    else:
                        temp.extend(lookup[level-j].revert(sym))
                buf = temp
            dat.extend(buf)
        for sym in dat:
            outputf.write("%s,%s,%s\n"%(str(sym[0]), str(sym[1]), str(sym[2])))
        inputf.close()
        outputf.close()

