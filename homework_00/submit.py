#!/bin/python3
import asyncio
import argparse

host = "115.27.161.189"
port = 9876

async def submit(data):
    try:
        reader, writer = await asyncio.open_connection(host, port)
    except OSError: 
        print("Connection failed. Please contact TA!")
        return
    writer.write(f"{len(data)}\n".encode())
    writer.write(data.encode())
    await writer.drain()
    while not reader.at_eof():
        result = await reader.read(128)
        print(result.decode())

    writer.close()
    await writer.wait_closed()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('filename', type=str, help="the file to submit")
    args = parser.parse_args()
    with open(args.filename, "r") as fin:
        data = fin.read()

    print("Connecting to server, please wait...")
    asyncio.run(submit(data))
    print("This score is not recorded. Please keep the file until deadline.")

