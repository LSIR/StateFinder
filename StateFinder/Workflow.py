"""
this file define some standard workflow,
 combining functions from the other files
"""

import Processing as pr
import DataSource as ds
import Converter as cv
import LookupTable as lt
import Symbolizer as sbz
from subprocess import call
import pickle


def convert_median_filter(src, dst, win):
    """
    Apply median filtering to the time series
    """
    dat = ds.FileDataSource(src, dst)
    pmf = pr.MedianFilteringProcess(win)
    dat.load()
    dat.data = pmf.batch_process(dat.data)
    dat.save()

def convert_rle(src, dst):
    """
    apply RLE compression
    """
    dat = ds.FileDataSource(src, dst)
    prle = pr.RLEProcess()
    dat.load()
    dat.data = prle.batch_process(dat.data)
    dat.save()

def convert_symbols_to_raw(src, dst, model, level, tolevel=0):
    """
    convert a symbolic time series to lower level or raw data
    """
    if level == "raw":
        return 
    if level == "symbol":
        level = 0
    if level == "rle":
        level = 1
    if level == "statecluster":
        level = 2
        prle = pr.RLEProcess()
        dat = ds.FileDataSource(src, src)
        dat.load()
        dat.data = prle.batch_process(dat.data)
        dat.save()
    if int(tolevel) == 0:
        ctr = cv.ToRaw()
        ctr.convert(src, dst, model, int(level))
    else:
        cts = cv.ToSymbols()
        cts.convert(src, dst, model, int(level))
        if int(tolevel) == 2:
            prle = pr.RLEProcess()
            dat = ds.FileDataSource(dst, dst)
            dat.load()
            dat.data = prle.batch_process(dat.data)
            dat.save()

def convert_from_csv(src, dst):
    """
    read a CSV file and generate a one dimensional time series
    various functions for aggregation can be defined here
    """
    cfc = cv.FromCSV()
    #f = lambda tab: int(1000*math.sqrt(float(tab)**2+float(tab)**2+float(tab)**2))
    #f = lambda tab: int(float(tab[0])*1000)
    fun = lambda tab: int(float(tab[0]))
    cfc.convert(src, dst, fun)


def find_states(dataset, inputfile, outputname, rate, smethod, snbr, ememory, ethreshold, minpts, mindist):
    """
    batch process using standard symbolization
    """
    src = ds.FileDataSource("../datasets/"+dataset+"/"+inputfile,
                            "../outputs/"+dataset+"/"+outputname+"-statefinder.csv")
    src.load()
    enc = sbz.UniformSymbolizer()
    if smethod == "1":
        enc = sbz.MedianSymbolizer()
    if smethod == "2":
        enc = sbz.DistinctMedianSymbolizer()
    enc.load(src.data)
    (sep, mini, maxi) = enc.get_separators(int(snbr))
    sym = pr.SymbolizeProcess(1, sep)
    rel = pr.RLEProcess()
    sem = pr.SegmentSparseProcess(rate, ethreshold, ememory)
    clu = pr.ClusterSparseProcess(mindist, int(snbr)+1)

    src.data = sym.batch_process(src.data)
    src.save_to("../outputs/"+dataset+"/"+outputname+"-symbol.csv")
    src.data = rel.batch_process(src.data)
    src.save_to("../outputs/"+dataset+"/"+outputname+"-rle.csv")
    segments = sem.batch_process(src.data)
    (src.data, lookup) = clu.batch_process(segments, src.data)
    src.save()
    lookups = {0:lt.SymbolLookupTable(sep, mini, maxi),
               1:lt.ExpandLookupTable(rate),
               2:(lt.ClusterSparseLookupTable(lookup, rate))}
    lkf = open("../outputs/"+dataset+"/"+outputname+"-model.mdl", 'w')
    pickle.dump(lookups, lkf)
    lkf.close()


def find_states_spclust(dataset, inputfile, outputname, rate, dimensions, wgrid, wnbr, ememory, ethreshold, sigma, mindist):
    """
    batch process using spclust symbolization
    """
    call(["java", "-jar", "../Spclust/WvComputeModel.jar", "../datasets/"+dataset+"/"+inputfile,
          dimensions, wgrid, wnbr, "../outputs/"+dataset+"/"+outputname+"-model.spc"])
    call(["java", "-jar", "../Spclust/WvComputeSymbols.jar", "../outputs/"+dataset+"/"+outputname+"-model.spc",
          "../datasets/"+dataset+"/"+inputfile, "../outputs/"+dataset+"/"+outputname+"-symbol.csv"])
    nbclusters = int(open("../outputs/"+dataset+"/"+outputname+"-model.spcn",'r').readline())

    src = ds.FileDataSource("../outputs/"+dataset+"/"+outputname+"-symbol.csv",
                            "../outputs/"+dataset+"/"+outputname+"-statecluster.csv")
    rel = pr.RLEProcess()
    sem = pr.SegmentSparseProcess(rate, ethreshold, ememory)
    clu = pr.ClusterSparseProcess(mindist, nbclusters)
    src.load()
    src.data = rel.batch_process(src.data)
    src.save_to("../outputs/"+dataset+"/"+outputname+"-rle.csv")
    src.data = rel.batch_process(src.data)
    segments = sem.batch_process(src.data)
    (src.data, lookup) = clu.batch_process(segments, src.data)
    src.save()
    lookups = {0:lt.SpclustSymbolLookupTable("../outputs/"+dataset+"/"+outputname+"-model.spc"),
               1:lt.ExpandLookupTable(rate),
               2:(lt.ClusterSparseLookupTable(lookup, rate))}
    lkf = open("../outputs/"+dataset+"/"+outputname+"-model.mdl", 'w')
    pickle.dump(lookups, lkf)
    lkf.close()

