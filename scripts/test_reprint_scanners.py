"""Regression guard for the reprint scripts' Kotlin-source scanners.

`generate-reprints.py` once declared its own naive `[^"]+` copies of these regexes and never
unescaped what they captured, so `card("Kongming, \"Sleeping Dragon\"")` scanned as the key
`Kongming, \`. That slugifies to a cache file that doesn't exist, so the card landed in the
"no fresh printing cache" bucket and was reported as *stale* — indistinguishable from
"we couldn't check this one" — rather than as covered. These tests fail if the naive bodies
come back, or if either call site stops unescaping.

Run from the repo root: `python3 -m unittest scripts.test_reprint_scanners`
"""

import importlib.machinery
import importlib.util
import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))


def _load(name: str, filename: str):
    loader = importlib.machinery.SourceFileLoader(name, str(SCRIPTS_DIR / filename))
    spec = importlib.util.spec_from_loader(name, loader)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    loader.exec_module(module)
    return module


generate_reprints = _load("generate_reprints", "generate-reprints.py")
# The module object `generate-reprints.py` itself imported — asserting against a separately loaded
# copy would compare two executions of the same file and prove nothing about the sharing.
missing_reprints = generate_reprints.missing_reprints

# Both real corpus names with an embedded quote: canonical in PTK, reprinted in C13 / PZ2.
KONGMING = 'Kongming, "Sleeping Dragon"'
PANG_TONG = 'Pang Tong, "Young Phoenix"'


class ScannerEscapeTest(unittest.TestCase):
    def test_card_dsl_re_spans_escaped_quotes(self):
        for module in (missing_reprints, generate_reprints):
            with self.subTest(module=module.__name__):
                m = module.CARD_DSL_RE.search(r'card("Kongming, \"Sleeping Dragon\"") {')
                self.assertIsNotNone(m)
                self.assertEqual(module.unescape_kotlin(m.group(1)), KONGMING)

    def test_printing_name_re_spans_escaped_quotes(self):
        for module in (missing_reprints, generate_reprints):
            with self.subTest(module=module.__name__):
                m = module.PRINTING_NAME_RE.search(r'    name = "Pang Tong, \"Young Phoenix\"",')
                self.assertIsNotNone(m)
                self.assertEqual(module.unescape_kotlin(m.group(1)), PANG_TONG)

    def test_generate_reprints_borrows_rather_than_redeclares(self):
        """Not just equivalent — the same objects, so a fix can't land in one script only.

        (`re` caches compiled patterns by pattern string, so identity on the regexes alone would
        pass even for two independent `re.compile` calls; `unescape_kotlin` is the real witness.)
        """
        self.assertIs(generate_reprints.CARD_DSL_RE, missing_reprints.CARD_DSL_RE)
        self.assertIs(generate_reprints.PRINTING_NAME_RE, missing_reprints.PRINTING_NAME_RE)
        self.assertIs(generate_reprints.unescape_kotlin, missing_reprints.unescape_kotlin)

    def test_unescaped_name_reaches_the_printings_cache_slug(self):
        """The whole point: the mangled key slugified to something no cache file matches."""
        self.assertEqual(generate_reprints.slugify(KONGMING), "kongming-sleeping-dragon")
        self.assertEqual(generate_reprints.slugify(PANG_TONG), "pang-tong-young-phoenix")
        self.assertEqual(generate_reprints.slugify(r"Kongming, \\"), "kongming")

    def test_ordinary_names_are_unchanged(self):
        m = generate_reprints.CARD_DSL_RE.search(r'card("Llanowar Elves") {')
        self.assertEqual(generate_reprints.unescape_kotlin(m.group(1)), "Llanowar Elves")


class CorpusTest(unittest.TestCase):
    """The two names as the corpus actually declares them, end to end through `scan_definitions`."""

    @classmethod
    def setUpClass(cls):
        cls.canonical, cls.reprints = generate_reprints.scan_definitions()

    def test_canonical_names_are_the_real_names(self):
        self.assertEqual(self.canonical.get(KONGMING), "ptk")
        self.assertEqual(self.canonical.get(PANG_TONG), "ptk")

    def test_no_truncated_keys_survive_anywhere(self):
        mangled = [n for n in self.canonical if n.endswith("\\")]
        self.assertEqual(mangled, [], f"truncated canonical keys: {mangled}")
        mangled_rows = [n for n in self.reprints if n.endswith("\\")]
        self.assertEqual(mangled_rows, [], f"truncated Printing-row keys: {mangled_rows}")

    def test_reprint_rows_are_found_under_the_real_names(self):
        self.assertIn("c13", self.reprints.get(KONGMING, set()))
        self.assertIn("pz2", self.reprints.get(PANG_TONG, set()))


if __name__ == "__main__":
    unittest.main()
