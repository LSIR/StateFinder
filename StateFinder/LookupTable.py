"""
This file represents the symbol lookup table.
It supports storing different definitions of symbols.
"""

import Processing
from subprocess import call
import random


class SpclustSymbolLookupTable:

    def __init__(self, model):
        self.model = model

    def revert(self, symbols):
        """
        wrapper for reverting from the Spclust java applet
        """
        rnd = random.random()
        tmpf = open('temp'+str(rnd)+'.sym', 'w')
        for sym in symbols:
            tmpf.write("1,%d\n"%(int(float(sym[2]))))
        tmpf.close()
        try:
            call(["java", "-jar", "../SpClust/WvGetRaw.jar", self.model,
                  'temp'+str(rnd)+'.sym', 'temp'+str(rnd)+'.real'])
        except Exception as exp:
            print exp
            return symbols
        tmpf = open('temp'+str(rnd)+'.real', 'r')
        tab = tmpf.readlines()
        tmpf.close()
        out = []
        for i in range(len(tab)):
            row = tab[i][:-1].split(",")[1:]
            out.append([symbols[i][0], symbols[i][1]]+row)
        return out


class SymbolLookupTable:
    
    def __init__(self, separators, _min, _max):
        self.sep = separators
        self.sep.insert(0, _min)
        self.sep.append(_max)

    def revert(self, symbol):
        """
        reverting using a simple separator based lookup table
        """
        if int(float(symbol[2])) > len(self.sep)-2:
            return [symbol]
        return [[symbol[0], symbol[1], (self.sep[int(float(symbol[2]))]+
                                        self.sep[int(float(symbol[2]))+1])/2]]


class ClusterSparseLookupTable:

    def __init__(self, tab, rate):
        self.tab = {}
        for i in tab.keys():
            self.tab[i] = self.index(tab[i])
        self.rate = rate
        
    def index(self, tab):
        """
        transform the tuple indexing into a dict of dict
        """
        ret = {}
        for k,v in tab.iteritems():
            if not k[0] in ret:
                ret[k[0]] = {}
            ret[k[0]][k[1]] = v
        return ret

    def revert(self, symbol, previous=None):
        """
        revert from StateFinder states by using the transition matrix
        """
        if not int(float(symbol[2])) in self.tab:
            return [symbol]
        if int(symbol[1])-int(symbol[0]) < 0:
            return []
        mat = self.tab[int(float(symbol[2]))]
        sym = []
        if previous not in mat:
            (sval, slen) = self.selectStart(mat)
        else:
            (sval, slen) = self.selectNext(mat, previous)
        curt = int(symbol[0])
        while curt < int(symbol[1]):
            sym.append([int(curt), int(min(curt+(slen*self.rate),
                        int(symbol[1]))), sval])
            curt += (slen*self.rate)
            (sval, slen) = self.selectNext(mat, sval)
        if len(sym) > 0:
            sym[len(sym)-1][1] = int(symbol[1])
        else:
            sym.append([int(symbol[0]), int(symbol[1]), sval])
        prle = Processing.RLEProcess()
        return prle.batch_process(sym)

    def selectStart(self, mat):
        """
        if not provided guess the starting symbol from the 
        transition matrix by taking the one with lower in transition
        but non zero out transition
        """
        tmp = (0, sum([sum(x.values()) for x in mat.values()]))
        for k,v in mat.iteritems():
            diag = v[k] if k in v else 0
            if sum(v.values()) > diag:
                col = sum([x[k] for x in mat.values() if k in x]) - diag
                if col <= tmp[1]:
                    tmp = (k, col)
        
        return (tmp[0], 1.0/(1.0-diag)-1)


    def selectNext(self, mat, s):
        """
        compute the next symbol and its length from the current one
        """
        rnd = random.random()
        i = -1
        cnt = 0
        last = 0
        if not s in mat:
            return (s, 10000) #filling with symbol s
        if s in mat[s]:
            last = mat[s][s]
        k = mat[s].keys()
        while cnt < rnd and i < (len(k)-1):
            i += 1
            if not k[i] == s:
                cnt += mat[s][k[i]]/(1.0-last)
        if k[i] in mat:
            if k[i] in mat[k[i]]:
                return (k[i], 1.0/(1.0-mat[k[i]][k[i]])-1)
            else:
                return (k[i], 1)
        else:
            return (k[i], 10000)



class ExpandLookupTable:

    def __init__(self, rate):
        self.rate = rate

    def revert(self, symbol):
        """
        unroll RLE symbols
        """
        sym = []
        if int(symbol[1])-int(symbol[0]) < 0:
            return sym
        for i in range(int((int(symbol[1])-int(symbol[0]))/self.rate)):
            sym.append([int(symbol[0])+i*self.rate,
                        int(symbol[0])+(i+1)*self.rate, symbol[2]])
        if len(sym) > 0:
            sym[len(sym)-1][1] = int(symbol[1])
        else:
            sym.append([int(symbol[0]), int(symbol[1]), symbol[2]])
        return sym


