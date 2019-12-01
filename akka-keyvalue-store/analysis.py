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

def draw_interval(fig, poll_start, end_tag, legend_set, operation, color='black', opacity=1, mirror=0):
    start = poll_start['timestamp']
    start_seq = poll_start['seq']
    start_val = poll_start['value']
    #start_tuple = f'({start_seq}, {start_val})'
    start_tuple = ''
    text_pos='bottom left' if operation=='Put' else 'top left'
    try:
        poll_end = df[(df['type'] == end_tag) & (df['ack'] == poll_start['seq']) & (df['from'] == poll_start['from'])].iloc[0]
        end = poll_end['timestamp']
        end_seq = poll_end['seq']
        end_val = poll_end['value']
        end_tuple = f'({end_seq}, {end_val})'
    except:
        end = start + 1
        end_tuple = f'({poll_start["seq"]}, ?)'
    if mirror:
        scatter_x = [end, end, start, start, end, end]
        l = poll_start['from']
        d = min(0.5, abs(mirror))
        scatter_y = [l-d, l+d, l+d, l-d, l-d, l+d]
    else:
        scatter_x = [start, end]
        scatter_y = [poll_start['from']]*2

    fig.add_trace(
           go.Scatter(
               x=scatter_x,
               y=scatter_y,
               opacity=opacity,
               line=dict(color=color),
               showlegend=not legend_set,
               name=operation
               #mode='lines+text',
               #textposition=text_pos,
               #text=[end_tuple, None, start_tuple]
               ))
    #fig.add_annotation(
    #    go.layout.Annotation(
    #            x=start,
    #            y=poll_start['from'],
    #            text=start_tuple,
    #            xref="x",
    #            yref="y",
    #            showarrow=True,
    #            arrowhead=7,
    #            ax=20,
    #            ay=0)
    #)
    fig.add_annotation(
        go.layout.Annotation(
                x=end,
                y=poll_start['from'],
                text=end_tuple,
                xref="x",
                yref="y",
                showarrow=True,
                arrowhead=7,
                ax=-30,
                ay=-20)
    )




def parse_operations(df: DataFrame):
    fig = go.Figure()
    # Messages starting a poll
    #pollstarts = df[df['type'] == 'startpoll']
    #legend_read_set = False
    #for i, poll_start in pollstarts.iterrows():
    #    draw_interval(fig, poll_start, 'set', legend_read_set, text='Internal read', color='blue', opacity=0.9)
    #    legend_read_set = True
    #writestarts = df[df['type'] == 'startwrite']
    #legend_write_set = False
    #for i, write_start in writestarts.iterrows():
    #    draw_interval(fig, write_start, 'writeset', legend_write_set, 'Internal write', color='orange', opacity=0.9)
    #    legend_write_set = True

    # Now draw the whole Get/Put operations
    getstarts = df[df['type'] == 'getstart']
    legend_get_set = False
    for i, get_start in getstarts.iterrows():
        draw_interval(fig, get_start, 'getstop', legend_get_set, 'Get', color='green', mirror=0.2)
        legend_get_set = True

    putstarts = df[df['type'] == 'putstart']
    legend_put_set = False
    for i, put_start in putstarts.iterrows():
        draw_interval(fig, put_start, 'putstop', legend_put_set, 'Put', color='red', mirror=0.2)
        legend_put_set = True


    fig.update_layout(
        title = 'Process activity for get and put operations (N: 10, f: 4, M: 3)',
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
    fig.update_xaxes(tickvals=[i for i in np.arange(0, int(df['timestamp'].max()), 10)])
    fig.show()


df = import_log('log.txt') 
parse_operations(df)


