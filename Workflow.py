"""
this file define some standard workflow,
 combining functions from the other files
"""

import StateFinder.Processing as pr
import Utils.DataSource as ds
import Utils.Converter as cv
import StateFinder.LookupTable as lt
import StateFinder.Symbolizer as sbz
import Utils.FileUtils as fu

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

def split_file_by(filename, folder, offset=0, duration=86400):
    """
    split the file for applying the forecasting algorithm
    """
    src = ds.FileDataSource(filename, None)
    cut = fu.PeriodicCutProcess(int(duration), int(offset))
    src.load()
    src.data = cut.batch_process(src.data)
    spl = fu.Splitter(src.data)
    spl.splitFiles(folder, int(duration), int(offset))

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

def get_distance(file0, file1, rate):
    dat0 = ds.FileDataSource(file0, None)
    dat1 = ds.FileDataSource(file1, None)
    dat0.load()
    dat1.load()
    gdt = fu.Get_Distance()
    dist = gdt.levenshtein(dat0.data, dat1.data, int(rate))
    print "total-distance (d) = %d"%(dist[0])
    print "total-time-length (l) = %d"%(dist[1])
    print "normalized-distance (d/l) = %f"%(dist[0]*1.0/dist[1])


def find_states(dataset, inputfile, outputname, rate, smethod, snbr, ememory, ethreshold, mindist):
    """
    batch process using standard symbolization
    """
    src = ds.FileDataSource("./datasets/"+dataset+"/"+inputfile,
                            "./outputs/"+dataset+"/"+outputname+"-statefinder.csv")
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
    src.save_to("./outputs/"+dataset+"/"+outputname+"-symbol.csv")
    src.data = rel.batch_process(src.data)
    src.save_to("./outputs/"+dataset+"/"+outputname+"-rle.csv")
    segments = sem.batch_process(src.data)
    (src.data, lookup) = clu.batch_process(segments, src.data)
    src.save()
    lookups = {0:lt.SymbolLookupTable(sep, mini, maxi),
               1:lt.ExpandLookupTable(rate),
               2:(lt.ClusterSparseLookupTable(lookup, rate))}
    lkf = open("./outputs/"+dataset+"/"+outputname+"-model.mdl", 'w')
    pickle.dump(lookups, lkf)
    lkf.close()


def find_states_spclust(dataset, inputfile, outputname, rate, dimensions, wgrid, wnbr, ememory, ethreshold, mindist):
    """
    batch process using spclust symbolization
    """
    call(["java", "-jar", "./Spclust/SpComputeModel.jar", "./datasets/"+dataset+"/"+inputfile,
          dimensions, wgrid, wnbr, "./outputs/"+dataset+"/"+outputname+"-model.spc"])
    call(["java", "-jar", "./Spclust/SpComputeSymbols.jar", "./outputs/"+dataset+"/"+outputname+"-model.spc",
          "./datasets/"+dataset+"/"+inputfile, "./outputs/"+dataset+"/"+outputname+"-symbol.csv"])
    nbclusters = int(open("./outputs/"+dataset+"/"+outputname+"-model.spcn",'r').readline())

    src = ds.FileDataSource("./outputs/"+dataset+"/"+outputname+"-symbol.csv",
                            "./outputs/"+dataset+"/"+outputname+"-statecluster.csv")
    rel = pr.RLEProcess()
    sem = pr.SegmentSparseProcess(rate, ethreshold, ememory)
    clu = pr.ClusterSparseProcess(mindist, nbclusters)
    src.load()
    src.data = rel.batch_process(src.data)
    src.save_to("./outputs/"+dataset+"/"+outputname+"-rle.csv")
    src.data = rel.batch_process(src.data)
    segments = sem.batch_process(src.data)
    (src.data, lookup) = clu.batch_process(segments, src.data)
    src.save()
    lookups = {0:lt.SpclustSymbolLookupTable("./outputs/"+dataset+"/"+outputname+"-model.spc"),
               1:lt.ExpandLookupTable(rate),
               2:(lt.ClusterSparseLookupTable(lookup, rate))}
    lkf = open("./outputs/"+dataset+"/"+outputname+"-model.mdl", 'w')
    pickle.dump(lookups, lkf)
    lkf.close()

