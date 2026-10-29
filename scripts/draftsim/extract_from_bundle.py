#!/usr/bin/env python3
"""Pull Draftsim's pick-rating and removal tables out of its draft-app bundle.

Draftsim's draft app (https://draftsim.com/draft/) is a Vite build whose
``/draft-app/dist/assets/index-<hash>.js`` inlines every set's data:

* ``../data/<CODE>.txt`` -> a template literal holding a tab-separated table
  (``Name, Name_2, Casting Cost 1, Casting Cost 2, Card Type, Rarity, Rating,
  List[, Archetype, Fixing, Splashable, ...]``), one row per card.
* ``../data/removal/<CODE>.json`` -> an object literal ``{"Card Name": [tags]}``,
  the set's curated removal list.

Both are reached through ``Object.assign({"../data/<CODE>.txt": VAR, ...})``-style
glob maps, so this script reads those maps, resolves each minified variable to
its definition, and decodes the literal. It does not interpret the tables --
that is ``import_draftsim_sets.py``'s job.

Usage::

    extract_from_bundle.py <index-XXXX.js | saved.har> <out-dir>

writes ``<out-dir>/tables/<CODE>.txt`` and ``<out-dir>/removal/<CODE>.json``
(plus ``<out-dir>/SOURCE`` naming the bundle it came from). A HAR is accepted so
the bundle can be captured from a browser session when a plain fetch is not an
option; the largest ``index-*.js`` response in it is used.

Stdlib only. Exits non-zero if a mapped variable cannot be resolved
unambiguously, rather than silently dropping a set.
"""
from __future__ import annotations

import base64
import json
import re
import sys
from pathlib import Path

TXT_ENTRY = re.compile(r'"(?:\.\./)+data/([A-Za-z0-9_]+)\.txt":([A-Za-z_$][\w$]*)')
REMOVAL_ENTRY = re.compile(r'"(?:\.\./)+data/removal/([A-Za-z0-9_]+)\.json":([A-Za-z_$][\w$]*)')

TEMPLATE_ESCAPES = {"n": "\n", "r": "\r", "t": "\t", "b": "\b", "f": "\f", "v": "\v", "0": "\0"}


def load_bundle(path: Path) -> tuple[str, str]:
    """Return (bundle text, source label). Accepts a .js file or a HAR."""
    raw = path.read_text(encoding="utf-8")
    if path.suffix.lower() != ".har":
        return raw, path.name
    har = json.loads(raw)
    best: tuple[int, str, str] | None = None
    for entry in har.get("log", {}).get("entries", []):
        url = entry.get("request", {}).get("url", "")
        name = url.rsplit("/", 1)[-1].split("?", 1)[0]
        if not re.fullmatch(r"index-[\w-]+\.js", name):
            continue
        content = entry.get("response", {}).get("content", {})
        text = content.get("text")
        if text is None:
            continue
        if content.get("encoding") == "base64":
            text = base64.b64decode(text).decode("utf-8")
        if "data/" not in text or not TXT_ENTRY.search(text):
            continue
        if best is None or len(text) > best[0]:
            best = (len(text), text, name)
    if best is None:
        sys.exit(f"{path}: no index-*.js response carrying Draftsim tables in this HAR")
    return best[1], f"{best[2]} (from {path.name})"


def read_template(s: str, start: int) -> str:
    """Decode the JS template literal whose opening backtick is at s[start]."""
    assert s[start] == "`"
    out: list[str] = []
    i = start + 1
    while True:
        c = s[i]
        if c == "`":
            return "".join(out)
        if c == "$" and s[i + 1] == "{":
            raise ValueError(f"template literal at {start} has an interpolation; not a data table")
        if c == "\\":
            n = s[i + 1]
            if n in TEMPLATE_ESCAPES:
                out.append(TEMPLATE_ESCAPES[n])
                i += 2
            elif n == "u":
                if s[i + 2] == "{":
                    end = s.index("}", i)
                    out.append(chr(int(s[i + 3:end], 16)))
                    i = end + 1
                else:
                    out.append(chr(int(s[i + 2:i + 6], 16)))
                    i += 6
            elif n == "x":
                out.append(chr(int(s[i + 2:i + 4], 16)))
                i += 4
            elif n == "\n":  # line continuation
                i += 2
            else:  # \` \$ \\ and any other identity escape
                out.append(n)
                i += 2
            continue
        out.append(c)
        i += 1


def definitions(s: str, var: str) -> list[int]:
    """Offsets just past `var=` for every assignment of the minified name."""
    pat = re.compile(r"(?<![\w$.])" + re.escape(var) + r"\s*=(?!=)\s*")
    return [m.end() for m in pat.finditer(s)]


def resolve_table(s: str, var: str) -> str:
    found = []
    for pos in definitions(s, var):
        if s[pos] == "`":
            text = read_template(s, pos)
            if text.split("\n", 1)[0].count("\t") >= 5:  # some tables have no header row
                found.append(text)
    if len(found) != 1:
        raise ValueError(f"table variable {var}: {len(found)} candidate definitions")
    return found[0]


class JsLiteral:
    """Minimal reader for the minified object/array literals the removal lists compile to.

    Vite/esbuild emits ``{"Abzan Charm":["removal"],Aethersnipe:GN,...}``: keys may be bare
    identifiers, and repeated tag arrays are hoisted into shared consts (``GN=["removal"]``),
    so a value can be a reference that has to be resolved against the bundle.
    """

    IDENT = re.compile(r"[A-Za-z_$][\w$]*")

    def __init__(self, s: str):
        self.s = s
        self.decoder = json.JSONDecoder()
        self.cache: dict[str, object] = {}

    def ws(self, i: int) -> int:
        while self.s[i] in " \t\r\n":
            i += 1
        return i

    def value(self, i: int) -> tuple[object, int]:
        i = self.ws(i)
        c = self.s[i]
        if c == "{":
            return self.obj(i)
        if c == "[":
            return self.arr(i)
        if c == '"':
            return self.decoder.raw_decode(self.s, i)
        if c == "'":
            end = i + 1
            while self.s[end] != "'":
                end += 2 if self.s[end] == "\\" else 1
            return json.loads('"' + self.s[i + 1:end].replace('"', '\\"').replace("\\'", "'") + '"'), end + 1
        if self.s.startswith("!0", i):
            return True, i + 2
        if self.s.startswith("!1", i):
            return False, i + 2
        m = self.IDENT.match(self.s, i)
        if m:
            return self.ref(m.group(0)), m.end()
        return self.decoder.raw_decode(self.s, i)  # number

    def key(self, i: int) -> tuple[str, int]:
        i = self.ws(i)
        if self.s[i] in "\"'":
            k, i = self.value(i)
            return str(k), i
        m = self.IDENT.match(self.s, i) or re.compile(r"\d+").match(self.s, i)
        if not m:
            raise ValueError(f"bad object key at {i}: {self.s[i:i + 40]!r}")
        return m.group(0), m.end()

    def obj(self, i: int) -> tuple[dict, int]:
        out: dict = {}
        i = self.ws(i + 1)
        while self.s[i] != "}":
            k, i = self.key(i)
            i = self.ws(i)
            if self.s[i] != ":":
                raise ValueError(f"expected ':' at {i}: {self.s[i:i + 40]!r}")
            v, i = self.value(i + 1)
            out[k] = v
            i = self.ws(i)
            if self.s[i] == ",":
                i = self.ws(i + 1)
        return out, i + 1

    def arr(self, i: int) -> tuple[list, int]:
        out: list = []
        i = self.ws(i + 1)
        while self.s[i] != "]":
            v, i = self.value(i)
            out.append(v)
            i = self.ws(i)
            if self.s[i] == ",":
                i = self.ws(i + 1)
        return out, i + 1

    def ref(self, var: str) -> object:
        if var not in self.cache:
            vals = []
            for pos in definitions(self.s, var):
                if self.s[pos] in "[{\"'":
                    try:
                        vals.append(self.value(pos)[0])
                    except (ValueError, IndexError):
                        continue
            if len(vals) != 1:
                raise ValueError(f"reference {var}: {len(vals)} literal definitions")
            self.cache[var] = vals[0]
        return self.cache[var]


def resolve_object(reader: JsLiteral, var: str) -> dict:
    obj = reader.ref(var)
    if not (isinstance(obj, dict) and all(isinstance(v, list) for v in obj.values())):
        raise ValueError(f"removal variable {var} is not a {{name: [tags]}} object")
    return obj


def collect(entries: re.Pattern, s: str) -> dict[str, str]:
    """code -> variable; every glob map in the bundle must agree."""
    out: dict[str, str] = {}
    for code, var in entries.findall(s):
        if out.setdefault(code, var) != var:
            raise ValueError(f"{code}: mapped to both {out[code]} and {var}")
    return out


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    bundle, label = load_bundle(Path(argv[1]))
    out = Path(argv[2])
    (out / "tables").mkdir(parents=True, exist_ok=True)
    (out / "removal").mkdir(parents=True, exist_ok=True)

    tables = collect(TXT_ENTRY, bundle)
    removals = collect(REMOVAL_ENTRY, bundle)
    if not tables:
        sys.exit("no ../data/<CODE>.txt map found -- has the bundle layout changed?")

    for code, var in sorted(tables.items()):
        text = resolve_table(bundle, var)
        with open(out / "tables" / f"{code}.txt", "w", encoding="utf-8", newline="") as fh:
            fh.write(text)
    reader = JsLiteral(bundle)
    for code, var in sorted(removals.items()):
        if code.startswith("_"):  # _overrides.json: Draftsim's own manual list, not a set table
            continue
        obj = resolve_object(reader, var)
        (out / "removal" / f"{code}.json").write_text(
            json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (out / "SOURCE").write_text(label + "\n", encoding="utf-8")
    print(f"{label}: {len(tables)} tables, {len([c for c in removals if not c.startswith('_')])} removal lists -> {out}")
    print("tables: " + " ".join(sorted(tables)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
