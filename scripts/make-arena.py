#!/usr/bin/env python3
"""Write an empty GameTest arena structure (.nbt): one layer of stone floor, open air above.

Usage: python3 -I scripts/make-arena.py <out.nbt> <size_x> <size_y> <size_z> <data_version>
Data versions: 1.20.2 = 3578, 1.20.4 = 3700, 1.20.6 = 3839, 1.21.1 = 3955.
"""
import gzip
import struct
import sys

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(n: str) -> bytes:
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def t_int(n: str, v: int) -> bytes:
    return bytes([TAG_INT]) + name(n) + struct.pack(">i", v)


def t_string(n: str, v: str) -> bytes:
    return bytes([TAG_STRING]) + name(n) + name(v)


def t_list(n: str, elem_type: int, payloads: list) -> bytes:
    return bytes([TAG_LIST]) + name(n) + bytes([elem_type]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def int_payload(v: int) -> bytes:
    return struct.pack(">i", v)


def compound_payload(*tags: bytes) -> bytes:
    return b"".join(tags) + bytes([TAG_END])


def main() -> None:
    out, sx, sy, sz, dv = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5])
    blocks = [
        compound_payload(t_list("pos", TAG_INT, [int_payload(x), int_payload(0), int_payload(z)]), t_int("state", 0))
        for x in range(sx)
        for z in range(sz)
    ]
    root = bytes([TAG_COMPOUND]) + name("") + compound_payload(
        t_int("DataVersion", dv),
        t_list("size", TAG_INT, [int_payload(sx), int_payload(sy), int_payload(sz)]),
        t_list("palette", TAG_COMPOUND, [compound_payload(t_string("Name", "minecraft:stone"))]),
        t_list("blocks", TAG_COMPOUND, blocks),
        t_list("entities", TAG_END, []),
    )
    with gzip.open(out, "wb") as f:
        f.write(root)


if __name__ == "__main__":
    main()
