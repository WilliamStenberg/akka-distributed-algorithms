import pandas as pd
from pandas import DataFrame
import numpy as np
import matplotlib.pyplot as plt
import plotly.graph_objects as go
import typing



def import_log(filename) -> DataFrame:
    """
    Imports a log captured from standard output,
    filtering rows containing "###" and converting
    to DataFrame with preset columns.
    """
    df = pd.DataFrame(columns=['from', 'to', 'type', 
        'ack', 'seq', 'value', 'timestamp'])
    with open(filename, 'r') as f:
        i = 0
        for line in f.readlines():
            if '###' in line:
                # "the line after '###' without newline character"
                comma_separated = line.split('###')[1].replace('\n', '')
                seq = comma_separated.split(',')
                for col in [0,1]:
                    try:
                        seq[col] = int(seq[col][1:])
                    except:
                        seq[col] = None
                for col in [3, 4, 5, 6]:
                    try:
                        seq[col] = int(seq[col])  
                    except:
                        seq[col] = None
                try:
                    seq[6] = float(seq[6]) / 1e6  # to ms
                except:
                    seq[6] = None
                df.loc[i] = seq
                i += 1
    earliest_timestamp = df['timestamp'].min()
    df['timestamp'] = df['timestamp'] - earliest_timestamp
    return df

def parse_operations(df: DataFrame):
    # Messages starting a poll
    pollstarts = df[df['type'] == 'startpoll']
    fig = go.Figure()
    legend_read_set = False
    for i, poll_start in pollstarts.iterrows():
        poll_end = df[(df['type'] == 'set') & (df['ack'] == poll_start['seq']) & (df['from'] == poll_start['from'])].iloc[0]
        start = poll_start['timestamp']
        end = poll_end['timestamp']
        start_tuple = f'({poll_start["seq"]}, ?)'
        end_seq = poll_end['seq']
        end_val = poll_end['value']
        end_tuple = f'({end_seq}, {end_val})'
        fig.add_trace(
               go.Scatter(
                   x=[start, end], 
                   y=[poll_start['from']]*2,  # extracting i
                   line=dict(color='black'),
                   showlegend=not legend_read_set,
                   name='Read',
                   mode='lines+text',
                   textposition='top center',
                   text=[start_tuple, end_tuple]
                   ))
        legend_read_set = True
    writestarts = df[df['type'] == 'startwrite']
    legend_write_set = False
    for i, write_start in writestarts.iterrows():
        try:
            write_end = df[(df['type'] == 'writeset') & (df['ack'] == write_start['seq']) & (df['from'] == write_start['from'])].iloc[0]
        except:
            print('Could not finish write, ')
            print(write_start)
            continue
        start = write_start['timestamp']
        end = write_end['timestamp']
        start_tuple = f'({write_start["seq"]}, !)'
        end_seq = write_end['seq']
        end_val = write_end['value']
        end_tuple = f'({end_seq}, {end_val})'
        fig.add_trace(
               go.Scatter(
                   x=[start, end], 
                   y=[write_start['from']]*2,  # extracting i
                   line=dict(color='red'),
                   text=[start_tuple, end_tuple],
                   textposition='bottom center',
                   mode='lines+text',
                   showlegend=not legend_write_set,
                   name='Write'
                   ))
        legend_write_set = True
        
    fig.update_layout(
        title = 'Process activity for get and put operations (N: 3, f: 1)',
        xaxis_title = 'Time (ms)',
        yaxis_title = 'Process identifier (i)',
        xaxis = dict(
            tickformat = 'ms'
        ),
        yaxis = dict(
            tickmode = 'linear',
            tick0 = 0,
            dtick = 1
            ))
    fig.update_xaxes(tickvals=[i for i in range(int(df['timestamp'].max()))])
    fig.show()


df = import_log('log.txt') 
parse_operations(df)


