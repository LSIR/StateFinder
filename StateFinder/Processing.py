"""
These classes represents building block of our processing.
They should have two working modes, namely batch mode and real-time mode. 
In batch mode the full dataset is sent and processed before returning any data.
In real-time mode, data is sent one by one and results outputed each time
 (it can be None, if there is nothing to output).
Both these modes shouldn't be mixed,
 or special care has to be taken with internal variables.
"""

from collections import deque
import math


class ApplyWindowedFunction(object):
    """
    apply a function on a sliding window on the time series
    """

    def __init__(self, window, func):
        self.win = int(window)
        self.fun = func
        self.buf = deque([0] * int(window))        

    def batch_process(self,data):
        ret = []        
        half = self.win/2
        #prefill
        for i in range(half):
            self.process_next(data[0])
        for i in range(half):
            self.process_next(data[i])
        #process
        for i in range(half, len(data)):
            ret.append(self.process_next((data[i-half][0], data[i-half][1],
                                          int(data[i][2]))))
        #postfill
        for i in range(len(data)-half, len(data)):
            ret.append(self.process_next((data[i][0], data[i][1],
                                          int(data[len(data)-1][2]))))
        return ret

    def process_next(self, elem):
        self.buf.append(elem[2])
        self.buf.popleft()
        val = self.fun(self.buf)
        return [elem[0], elem[1], val]        


class MedianFilteringProcess(ApplyWindowedFunction):
    """
    returns a time series of values as the median of the sliding window
    """

    def __init__(self, window):
        self.win = int(window)
        self.buf = deque([0] * int(window))
        self.fun = self.median
        
    def median(self, tab):
        tmp = sorted(tab)
        if not self.win % 2:
            return int((tmp[self.win/2]+tmp[self.win/2-1])/2)
        return tmp[self.win/2]        
         

class RLEProcess(object):
    """
    Run Length Encoding
    Takes a triple and compress contiguous ones that are the same
    """
    
    def __init__(self):
        self.previous_t = None
        self.previous_v = None

    def batch_process(self, data):
        last = data[0][0]
        tab = []
        for i in range(len(data)-1):
            if data[i][2] != data[i+1][2]:
                tab.append([last, data[i][1], data[i][2]])
                last = data[i+1][0]
        tab.append([last, data[len(data)-1][1],
                    data[len(data)-1][2]])
        return tab

    def process_next(self, elem):
        toreturn = None
        if self.previous_t:
            if elem[2] != self.previous_v:
                toreturn = [self.previous_t, elem[1], self.previous_v]
                self.previous_t = elem[0]
                self.previous_v = elem[2]
        else:
            self.previous_t = elem[0]
            self.previous_v = elem[2]
        return toreturn
 

class SymbolizeProcess(object):
    """
    Conversion from raw values to symbol using separators
    Takes a triple and output the corresponding symbol with the same timestamps
    """

    def __init__(self, length, separators):
        self.lng = length
        self.sep = separators
        self.current_v = 0
        self.current_c = 0
        self.previous_t = None

    def batch_process(self, data):
        tab = []
        for i in range(0, len(data), self.lng):
            avg = reduce(lambda x, y: x[2]+y[2], data[i:i+self.lng],
                         [0, 0, 0])*1.0/self.lng
            j = 0
            while j < len(self.sep) and avg > self.sep[j]:
                j += 1
            tab.append([data[i][0], data[i][1], j])
        return tab

    def process_next(self, elem):
        toreturn = None
        self.current_v += elem[2]
        self.current_c += 1
        if not self.previous_t:
            self.previous_t = elem[0]

        if self.current_c >= self.lng:
            avg = self.current_v*1.0/self.current_c
            j = 0
            while j < len(self.sep) and avg > self.sep[j]:
                j += 1
            toreturn = [self.previous_t, elem[1], j]
            self.previous_t = elem[0]
            self.current_v = 0
            self.current_c = 0
        return toreturn



class ClusterSparseProcess(object):
    """
    return the cluster number of the input segment
    represented as a sparse matrix (with dicts)
    nbr parameter is where to start numbering the clusters
    """

    def __init__(self, epsilon, nbr, minpt=1):
        self.eps = float(epsilon)
        self.nbr = int(nbr)
        self.lookup = {}
        self.count = {}        
        self.minpts = minpt

    def batch_process(self, segments, data):
        tab = []
        clu = []
        clusters = {}
        for seg in segments:
            sym = self.process_next(seg)[0]
            clu.append(sym[2]-self.nbr)
        j = 0
        for i in range(len(clu)):
            while j < len(data) and data[j][1] <= segments[i][0]:
                tab.append(data[j])
                j += 1
            if clu[i] >= 0 and self.count[clu[i]+self.nbr] >= self.minpts:
                tab.append([segments[i][0], segments[i][1], clu[i]+self.nbr])
                if not clu[i]+self.nbr in clusters:
                    clusters[clu[i]+self.nbr] = []
                last = j
                while j < len(data) and data[j][1] <= segments[i][1]:
                    j += 1
                clusters[clu[i]+self.nbr].append(data[last:j])
                    
        while j < len(data):
            tab.append(data[j])
            j += 1
        return (tab, self.lookup)

    def process_next(self, segment):
        dist = self.eps
        clu = -1
        for k, v in self.lookup.iteritems():
            d = self.cosine_distance(segment[2], v)
            if d < dist:
                dist = d
                clu = k
        if clu == -1:
            clu = len(self.lookup)+self.nbr
            self.lookup[clu] = segment[2]
            self.count[clu] = 1
        else:
            self.mean(clu, segment)
            self.count[clu] += segment[1] - segment[0]
        return ([segment[0], segment[1], clu], self.lookup)

    def mean(self, c, s):
        one = []
        two = []
        for k,v in s[2].iteritems():
            if not k[0] in one:
                one.append(k[0])
        for k,v in self.lookup[c].iteritems():
            if not k[0] in two:
                two.append(k[0])
            if k in s[2]:
                self.lookup[c][k] = ((self.lookup[c][k]*self.count[c]+
                                      s[2][k]*(s[1]-s[0]))/
                                     (self.count[c]+(s[1]-s[0])))
            else:
                if k[0] in one:
                    self.lookup[c][k] = ((self.lookup[c][k]*self.count[c])/
                                         (self.count[c] + (s[1]-s[0])))
        for k,v in s[2].iteritems():
            if not k in self.lookup[c]:
                if k[0] in two:
                    self.lookup[c][k] = ((s[2][k]*(s[1]-s[0]))/
                                         (self.count[c] + (s[1]-s[0])))
                else:
                    self.lookup[c][k] = s[2][k]

    def cosine_distance(self, s1, s2):
        """
        actually angular similarity
        """
        intersection = 0.0
        s1s = 0.0
        s2s = 0.0
        for k,v in s1.iteritems():
            if not v == 0:
                if k in s2 and not s2[k] == 0.0:
                    intersection += (s2[k]) * v
                s1s += v * v
        for v in s2.values():
            if not v == 0:
                s2s += v * v
        theta = intersection/(math.sqrt(s1s)*math.sqrt(s2s))
        return 1-theta


class SegmentSparseProcess(object):
    """
    generate segments by applying a threshold on the forecasting error 
    from the iteratively updated length-frequencies
    The generated segments are stored as sparse matrices (dicts)
    """

    def __init__(self, rate, threshold, lmbda, wsize=1, minSeg=5, slack=2):
        self.rate = float(rate)
        self.previous_v = [0, 0, 0]
        self.thr = float(threshold)
        self.wsize = int(wsize)
        self.window = deque([0] * int(wsize))
        self.buf = []
        self.backbuf = []
        self.mat = {}
        self.lmbda = float(lmbda)
        self.min_seg = int(minSeg)
        self.slack = float(slack)

    def batch_process(self, data):
        tab = []
        for dat in data:
            res = self.process_next(dat)
            if res:
                tab.append(res)
        return tab        

    def process_next(self, elem):
        if not elem[1]-elem[0] > 0:
            return None
        ret = None
        steps = elem[1]-elem[0]
        dist = self.error(self.previous_v, elem)
        self.updateFreq(int(self.previous_v[2]), int(elem[2]), steps, self.lmbda)
        self.previous_v = elem
        self.window.append(dist)
        self.window.popleft()
        if sum(self.window) > self.wsize * self.thr:
            if len(self.buf) > (self.min_seg): #min size of segment
                ret = self.buildSegment()
                self.mat = {}
                self.backbuf = []
            else:
                self.backbuf.extend(self.buf) 
                #after a too small segment we add it to backbuf
                #only fill it while high error, keep when in low error,
                # flush after segment.
            self.buf = []
        self.buf.append(elem)
        return ret

    def updateFreq(self, e1, e2, steps, lmbda):
        if not e1 in self.mat:
            self.mat[e1] = {}
        if not e2 in self.mat[e1]:
            self.mat[e1][e2] = {}
        if not steps in self.mat[e1][e2]:
            self.mat[e1][e2][steps] = 0

        for kk in self.mat[e1].keys():
            for kkk in self.mat[e1][kk].keys():
                self.mat[e1][kk][kkk] *= lmbda
        self.mat[e1][e2][steps] += 1

    def error(self, e1, e2):
        steps = e2[1]-e2[0]
        if not int(e1[2]) in self.mat or not int(e2[2]) in self.mat[int(e1[2])]:
            return 1.0
        tab = self.mat[int(e1[2])][int(e2[2])]
        su0 = sum(tab.values())
        if su0 == 0:
            dist = 1.0
        else:
            su = sum([sum(x.values()) for x in self.mat[int(e1[2])].values()])
            su1 = sum([k*v for k, v in tab.iteritems()])
            su2 = sum([k*k*v for k, v in tab.iteritems()])
            var = su2/su0-(su1*su1)/(su0*su0)
            if var < 0:
                var = 0 # because of rounding errors !!!
            sigma = self.slack*0.9*min(math.sqrt(var),
                                       self.iqr(tab, su0)/1.34)*(su0 ** -0.2)
            if sigma < 1.0:
                sigma = 1.0
            dist = 1.0-(sum([v*(math.exp(-(k-steps)*(k-steps)/(2.0*sigma ** 2)))
                             for k, v in self.mat[int(e1[2])][int(e2[2])].iteritems()])/
                        (1.0*su))
        return dist

    def iqr(self, tab, su):
        i = -1
        k = sorted(tab.keys())
        s = 0
        while s < 0.25*su:
            i += 1
            s += tab[k[i]]
        q1 = k[i]
        while s < 0.75*su:
            i += 1
            s += tab[k[i]]
        q2 = k[i] 
        return q2 - q1


    def buildSegment(self):
        #------backtracking-------
        #rebuild mat for the segment
        self.buf.pop()
        self.backbuf.append(self.buf.pop(0)) #re-check first element
        self.mat = {}
        for i in reversed(range(1, len(self.buf))):
            self.updateFreq(int(self.buf[i][2]), int(self.buf[i-1][2]),
                            self.buf[i-1][1]-self.buf[i-1][0], self.lmbda)

        #add some symbols in front
        sym = self.buf[0]
        for i in reversed(range(len(self.backbuf))):
            dist = self.error(sym, self.backbuf[i])
            sym = self.backbuf[i]
            if dist > (self.thr):
                break
            else:
                self.buf.insert(0, sym)
        #--------------------------
        mat = {}
        counts = {}
        for i in range(len(self.buf)-1):
            if not (int(self.buf[i][2]), int(self.buf[i][2])) in mat:
                mat[(int(self.buf[i][2]), int(self.buf[i][2]))] = 0
            if not (int(self.buf[i][2]), int(self.buf[i+1][2])) in mat:
                mat[(int(self.buf[i][2]), int(self.buf[i+1][2]))] = 0
            if not int(self.buf[i][2]) in counts:
                counts[int(self.buf[i][2])] = 0
            counts[int(self.buf[i][2])] += (self.buf[i][1]-self.buf[i][0])/self.rate+1
            mat[(int(self.buf[i][2]), int(self.buf[i][2]))] += (self.buf[i][1]-self.buf[i][0])/self.rate
            mat[(int(self.buf[i][2]), int(self.buf[i+1][2]))] += 1
        if not (int(self.buf[-1][2]), int(self.buf[-1][2])) in mat:
            mat[(int(self.buf[-1][2]), int(self.buf[-1][2]))] = 0
        mat[(int(self.buf[-1][2]), int(self.buf[-1][2]))] += (self.buf[-1][1]-self.buf[-1][0])/self.rate
        if not int(self.buf[-1][2]) in counts:
            counts[int(self.buf[-1][2])] = 0
        counts[int(self.buf[-1][2])] += (self.buf[-1][1]-self.buf[-1][0])/self.rate
        for i in mat.keys():
            if counts[i[0]] > 0:
                mat[i] *= 1.0/counts[i[0]] 
        return [self.buf[0][0], self.buf[len(self.buf)-1][1],mat]

