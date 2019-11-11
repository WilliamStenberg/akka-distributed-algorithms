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
    pollstarts = df[df['type'] == 'startpoll'].groupby('from').min()
    fig = go.Figure()
    for i, poll_start in pollstarts.iterrows():
        poll_end = df[(df['type'] == 'set') & (df['ack'] == poll_start['seq']) & (df['from'] == poll_start.name)]
        start = poll_start['timestamp']
        end = poll_end['timestamp'].iloc[0]
        start_tuple = f'({poll_start["seq"]}, REQ)'
        end_seq = poll_end['seq'].iloc[0]
        end_val = poll_end['value'].iloc[0]
        end_tuple = f'({end_seq}, {end_val})'
        fig.add_trace(
               go.Scatter(
                   x=[start, end], 
                   y=[poll_start.name]*2,  # extracting i
                   line=dict(color='black'),
                   hovertext=[start_tuple, end_tuple],
                   hoverinfo='text'
                   ))
        responses = df[(df['type'] == 'poll') &
                (df['to'] == poll_start.name) &
                (df['ack'] == poll_start['seq'])]
        for _, row in responses.iterrows():
            fig.add_trace(
                   go.Scatter(
                       x=[start, row['timestamp']], 
                       y=[poll_start.name, row['from']],  
                       line=dict(color='grey'),
                       opacity=0.5,
                       hoverinfo='skip'))


    fig.update_layout(
        xaxis = dict(
            tickmode = 'linear',
            tick0 = 0,
            dtick = 1,
            tickformat = 'ms'
        ),
        yaxis = dict(
            tickmode = 'linear',
            tick0 = 0,
            dtick = 1
            ))
    fig.show()


df = import_log('log.txt') 
parse_operations(df)


