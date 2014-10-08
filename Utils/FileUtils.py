"""
File processing and statistics computation
"""
import Levenshtein as lev

class PeriodicCutProcess(object):
    """
    prepare the files for being cut at specified time intervals
    """

    def __init__(self, period, offset):
        self.prd = int(period)
        self.off = int(offset)

    def batch_process(self, data):
        ret = []
        for dat in data:
            ret.extend(self.process_next(dat))
        return ret 
        

    def process_next(self, elem):
        nex = (((int(elem[0])-self.off)/int(self.prd))+1)*self.prd+self.off
        sym = elem[0]
        ret = []
        while nex < int(elem[1]):
            ret.append([sym, nex, elem[2]])
            sym = nex
            nex = nex + self.prd
        ret.append([sym, elem[1], elem[2]])
        return ret

class Splitter(object):
    """
    writes the data array in multiple files for the forecasting process
    """ 

    def __init__(self, data):
        self.data = data


    def splitFiles(self, folder, length, offset=0):
        i = 0
        j = 0
        outputf = open(folder + "file" + str(j), 'w')
        dat = int((int(self.data[0][0])-offset)/length)*length+offset
        while i < len(self.data):
            if self.data[i][0] >= dat + length:
                j += 1
                dat += length
                outputf.close()
                outputf = open(folder + "file" + str(j), 'w')
            outputf.write("%s,%s,%s\n"%(self.data[i][0], 
                                        self.data[i][1], str(self.data[i][2])))
            i += 1
        outputf.close()


class Get_Distance(object):
    """
    compute distances between two symbolic time series
    """

    def levenshtein(self, a0, a1, rate):
        s1 = ""
        for a in a0:
            s1 += (str(chr(int(a[2])+97)) * max(int((a[1]-a[0])/rate), 1))
        s2 = ""
        for a in a1:
            s2 += (str(chr(int(a[2])+97)) * max(int((a[1]-a[0])/rate), 1))
        return [lev.distance(s1, s2), len(s1)]
