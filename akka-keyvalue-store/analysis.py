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
    legend_vote_set = False
    legend_poll_set = False
    for i, poll_start in pollstarts.iterrows():
        poll_end = df[(df['type'] == 'set') & (df['ack'] == poll_start['seq']) & (df['from'] == poll_start['from'])].iloc[0]
        start = poll_start['timestamp']
        end = poll_end['timestamp']
        start_tuple = f'({poll_start["seq"]}, REQ)'
        end_seq = poll_end['seq']
        end_val = poll_end['value']
        end_tuple = f'({end_seq}, {end_val})'
        fig.add_trace(
               go.Scatter(
                   x=[start, end], 
                   y=[poll_start['from']]*2,  # extracting i
                   line=dict(color='black'),
                   hovertext=[start_tuple, end_tuple],
                   hoverinfo='text',
                   showlegend=not legend_read_set,
                   name='Read'
                   ))
        legend_read_set = True
        poll_receivals= df[(df['type'] == 'poll') &
                (df['to'] == poll_start['from']) &
                (df['ack'] == poll_start['seq'])]

        poll_votes= df[(df['type'] == 'vote') &
                (df['from'] == poll_start['from']) &
                (df['ack'] == poll_start['seq'])]
        for i, row in poll_receivals.iterrows():

            fig.add_trace(
                   go.Scatter(
                       x=[start, row['timestamp']], 
                       y=[poll_start['from'], row['from']],  
                       line=dict(color='orange'),
                       opacity=0.5,
                       hoverinfo='skip',
                       showlegend=not legend_poll_set,
                       name='Send poll'))
            legend_poll_set = True

            for _, vote in poll_votes[poll_votes['to'] == row['from']].iterrows():
                fig.add_trace(
                       go.Scatter(
                           x=[row['timestamp'], vote['timestamp']], 
                           y=[row['from'], vote['from']],  
                           line=dict(color='blue'),
                           opacity=0.5,
                           hoverinfo='skip',
                           showlegend=not legend_vote_set,
                           name='Vote'))
                legend_vote_set = True

    fig.update_layout(
        title = 'Process activity for single read (N: 3, f: 1)',
        xaxis_title = 'Time (ms)',
        yaxis_title = 'Process identifier (i)',
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


