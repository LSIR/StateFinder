"""
Different methods for transforming raw time series into symbolic time series
"""
#from math import sqrt
from scipy.stats.mstats_basic import mquantiles


class Symbolizer(object):
    """
    separator based method
    """

    def __init__(self):
        self.stats = []

    def add_stats(self, val):
        """
        collect values from the time series
        """
        self.stats.append(val)

    def load(self, data):
        """
        read the time series
        """
        for dat in data:
            self.add_stats(dat[2])

class UniformSymbolizer(Symbolizer):
    """
    separators at regular intervals
    """

    def __init__(self):
        self.name = "uniformGrid"
        self.stats = []

    def get_separators(self, nbr):
        """
        get the computed separators, min max coulc also be taken at 2 sigma
        """
        #summ = 0
        #sumq = 0
        #for sta in self.stats:
        #    summ += sta
        #    sumq += sta*sta
        #mean = summ/(len(self.stats) * 1.0)
        #sigma = sqrt(sumq/(len(self.stats) * 1.0) - mean * mean)
        #mini = mean - 2 * sigma       #95%
        #maxi = mean + 2 * sigma       #95%
        mini = min(self.stats)
        maxi = max(self.stats)
        return ([mini+(maxi-mini)*1.0/nbr*i for i in range(1, nbr)], mini, maxi)


class MedianSymbolizer(Symbolizer):
    """
    separators contains equal number of values
    """
    
    def __init__(self):
        self.name = "median"
        self.stats = []

    def get_separators(self, nbr):
        """
        get the computed separators, min max coulc also be taken at 2 sigma
        """
        mini = min(self.stats)
        maxi = max(self.stats)
        return (mquantiles(self.stats, 
                           prob=[i*1.0/nbr for i in range(1, nbr)]).tolist(), mini, maxi)


class DistinctMedianSymbolizer(Symbolizer):
    """
    separators contains equal number of distinct values
    """

    def __init__(self):
        self.name = "distinctmedian"
        self.stats = set()


    def get_separators(self, nbr):
        """
        get the computed separators, min max coulc also be taken at 2 sigma
        """
        mini = min(self.stats)
        maxi = max(self.stats)
        return (mquantiles(list(self.stats), 
                            prob=[i*1.0/nbr for i in range(1, nbr)]).tolist(), mini, maxi)

    def add_stats(self, val):
        self.stats.add(val)
