import pandas as pd
from pandas import DataFrame
import numpy as np
import matplotlib.pyplot as plt
import typing



def import_log(filename) -> DataFrame:
    df = pd.DataFrame(columns=['from', 'to', 'type', 
        'ack', 'seq', 'value', 'timestamp'])
    with open(filename, 'r') as f:
        i = 0
        for line in f.readlines():
            if '###' in line:
                # "the line after '###' without newline character"
                comma_separated = line.split('###')[1].replace('\n', '')
                seq = comma_separated.split(',')
                df.loc[i] = seq
                i += 1
    return df

df = import_log('log.txt') 
