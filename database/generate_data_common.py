"""Shared PostgreSQL helpers for deterministic demo-data generators."""

from __future__ import annotations

import os
from contextlib import contextmanager
from typing import Iterator

import psycopg


DB_CONFIG = {
    "host": os.getenv("FASHION_DB_HOST", "localhost"),
    "port": int(os.getenv("FASHION_DB_PORT", "5432")),
    "dbname": os.getenv("FASHION_DB_NAME", "fashion"),
    "user": os.getenv("FASHION_DB_USER", "fashion"),
    "password": os.getenv("FASHION_DB_PASSWORD", "fashion"),
}

PASSWORD_HASH = os.getenv(
    "FASHION_DEMO_PASSWORD_HASH",
    "$2a$10$7EqJtq98hPqEX7fNZaFWoO5Y4fK2QX3QG7QmYq6X4QH6xv4u7GQ7e",
)


def connect() -> psycopg.Connection:
    return psycopg.connect(**DB_CONFIG)


@contextmanager
def transaction() -> Iterator[psycopg.Connection]:
    with connect() as connection:
        with connection.transaction():
            yield connection


def table_exists(connection: psycopg.Connection, table_name: str) -> bool:
    result = connection.execute(
        "SELECT to_regclass(%s) IS NOT NULL", (f"public.{table_name}",)
    ).fetchone()
    return bool(result and result[0])


def scalar(connection: psycopg.Connection, query: str, params: tuple = ()) -> int:
    value = connection.execute(query, params).fetchone()[0]
    return int(value or 0)


def slug(value: str) -> str:
    return "-".join(value.lower().split())
