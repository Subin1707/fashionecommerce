#!/usr/bin/env python3
"""HEAD-check every product_images.image_url from PostgreSQL."""
import os
import argparse
from pathlib import Path
import subprocess
import sys
import urllib.error
import urllib.request
from urllib.parse import urljoin, urlparse, unquote

ROOT = Path(__file__).resolve().parents[1]
PUBLIC_ROOT = ROOT / "frontend" / "public"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--urls-file", type=Path, help="Text file containing one product_images.image_url per line")
    parser.add_argument("--base-url", default=os.environ.get("IMAGE_CHECK_ORIGIN", "http://localhost:8081"),
                        help="Origin used for local /assets URLs (default: IMAGE_CHECK_ORIGIN or localhost:8081)")
    parser.add_argument("--check-http", action="store_true",
                        help="Also HEAD-check local URLs against --base-url (file existence is always checked)")
    args = parser.parse_args()

    database_url = os.environ.get("DATABASE_URL")
    if args.urls_file:
        try:
            values = [line.strip() for line in args.urls_file.read_text(encoding="utf-8-sig").splitlines()
                      if line.strip() and not line.lstrip().startswith("#")]
        except OSError as error:
            print(f"ERROR: cannot read {args.urls_file}: {error}", file=sys.stderr)
            return 2
    else:
        psql_args = ["psql", "-X", "-A", "-t", "-v", "ON_ERROR_STOP=1"]
        psql_query = "SELECT DISTINCT image_url FROM product_images WHERE image_url IS NOT NULL ORDER BY image_url"
        if database_url:
            psql_args.extend([database_url])
        else:
            psql_args.extend(["-d", os.environ.get("PGDATABASE", "fashion")])
        psql_args.extend(["-c", psql_query])
        try:
            result = subprocess.run(psql_args, check=True, text=True, capture_output=True)
            values = [line.strip() for line in result.stdout.splitlines() if line.strip()]
        except FileNotFoundError:
            print("ERROR: psql missing; pass --urls-file or install PostgreSQL client.", file=sys.stderr)
            return 2
        except subprocess.CalledProcessError as error:
            print(error.stderr.strip() or "ERROR: failed to query product_images.", file=sys.stderr)
            return 2

    origin = args.base_url.rstrip("/")
    failed = False
    external_count = 0
    missing_count = 0
    total = len(values)
    for value in values:
        parsed = urlparse(value)
        if parsed.scheme in ("http", "https"):
            external_count += 1
            print(f"EXTERNAL {value}")
            continue

        local_path = unquote(parsed.path)
        candidate = (PUBLIC_ROOT / local_path.lstrip("/")).resolve()
        try:
            candidate.relative_to(PUBLIC_ROOT.resolve())
            file_exists = candidate.is_file()
        except ValueError:
            file_exists = False
        if not file_exists:
            missing_count += 1
            print(f"MISSING_FILE {value}")
            failed = True

        if not args.check_http:
            print(f"LOCAL_FILE_OK {value}" if file_exists else f"MISSING_FILE {value}")
            continue

        url = urljoin(origin + "/", value.lstrip("/"))
        request = urllib.request.Request(url, method="HEAD")
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                content_type = response.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
                if response.status != 200 or not content_type.startswith("image/"):
                    print(f"FAIL {response.status} {content_type or '<missing>'} {url}")
                    failed = True
                else:
                    print(f"OK {response.status} {content_type} {url}")
        except urllib.error.HTTPError as error:
            content_type = error.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
            print(f"FAIL {error.code} {content_type or '<missing>'} {url}")
            failed = True
        except (urllib.error.URLError, TimeoutError) as error:
            print(f"FAIL {error} {url}")
            failed = True
    print(f"SUMMARY total={total} http(s)={external_count} missing_files={missing_count}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
