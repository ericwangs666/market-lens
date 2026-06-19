#!/bin/sh
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
  eval "$(
    python3 - <<'PY'
import os
import shlex
from urllib.parse import unquote, urlparse

url = urlparse(os.environ["DATABASE_URL"])
port = url.port or 5432
database = url.path.lstrip("/")
query = f"?{url.query}" if url.query else ""

values = {
    "DB_URL": f"jdbc:postgresql://{url.hostname}:{port}/{database}{query}",
    "DB_USERNAME": unquote(url.username or ""),
    "DB_PASSWORD": unquote(url.password or ""),
}

for key, value in values.items():
    print(f"export {key}={shlex.quote(value)}")
PY
  )"
fi

exec java -jar /app/app.jar
