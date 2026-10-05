/**
 * Public facade for the deckbuilder query language.
 *
 * The deckbuilder calls `parseQuery(text)` once per text change. The result
 * carries both a `predicate` (used to filter the in-memory catalog) and an
 * `errors` array (rendered as inline diagnostics in the search bar).
 *
 * The grammar is documented in `parser.ts` and the supported keys in
 * `matchers.ts` (single source of truth — the help panel reads from
 * `ALL_KEYS`).
 */
import type { AtomNode, CardPredicate, Node, ParseResult } from './types'
import type { CardSummary, PrintedNamePrinting } from '../cardFilter'
import { nameTest } from './matchers'
import { parse } from './parser'
import { compile } from './evaluate'

export type { CardPredicate, ParseError, ParseResult, AtomNode, Node, Op, Span } from './types'
export { isAdvancedQuery } from './parser'
export { ALL_KEYS, MATCHERS } from './matchers'

const ALWAYS: CardPredicate = () => true

export function parseQuery(query: string): ParseResult {
  const trimmed = query.trim()
  if (!trimmed) {
    return { predicate: ALWAYS, errors: [], warnings: [], ast: null }
  }
  const { ast, errors: parseErrors } = parse(query)
  const { predicate, errors: compileErrors } = compile(ast)
  return {
    predicate,
    errors: [...parseErrors, ...compileErrors],
    warnings: [],
    ast,
  }
}

/**
 * Extract the dominant set filter from a parsed query, if any. Returns the uppercased
 * set code when the query contains a positive `s:CODE` (or `set:CODE`) atom inside an
 * AND-only branch — i.e. the filter unambiguously narrows the result set to one set.
 *
 * Returns `null` for queries with no set filter, with a regex value, or with `or`/`not`
 * in scope (where the filter wouldn't necessarily apply to every match — silently
 * swapping art under those would be misleading).
 *
 * This is the seam the deckbuilder uses to decide when to display a card via its
 * reprint art instead of the catalog default.
 */
export function extractSetFilter(ast: Node | null): string | null {
  if (!ast) return null
  const stack: Array<{ node: Node; underAnd: boolean }> = [{ node: ast, underAnd: true }]
  while (stack.length > 0) {
    const { node, underAnd } = stack.pop()!
    if (!underAnd) continue
    if (node.kind === 'atom') {
      if (
        !node.regex &&
        (node.key === 's' || node.key === 'set') &&
        (node.op === ':' || node.op === '=')
      ) {
        const v = node.value.trim().toUpperCase()
        if (v) return v
      }
    } else if (node.kind === 'and') {
      for (const child of node.children) stack.push({ node: child, underAnd: true })
    }
    // `or` / `not` branches are intentionally ignored — they break the "every result
    // is from this set" invariant the art override relies on.
  }
  return null
}

/**
 * Build a lookup for the printing a query matched by its *printed* name. Returns, per card, the
 * printing whose printed name satisfies the query's name atoms (bareword, `name:`, `!exact`) —
 * but only when the oracle name does not, so `ademi` shows the Through the Omenpaths art while
 * `spectacular` keeps the default. Null when the query has no name atom.
 *
 * `and` needs every name constraint, `or` any of them — so a pasted decklist
 * (`!"Leyline Weaver" or !"Cut Down"`) picks art per card. An `or` with a branch that names
 * nothing, and anything under `not`, leave the name unconstrained and never pick art.
 */
export function printedNameMatcher(
  ast: Node | null,
): ((card: CardSummary) => PrintedNamePrinting | null) | null {
  const test = ast ? nameConstraint(ast) : null
  if (!test) return null
  return (card) => {
    const printed = card.printedNamePrintings
    if (!printed || printed.length === 0 || test(card.name)) return null
    return printed.find((p) => test(p.name)) ?? null
  }
}

/** The name test a node imposes, or null when it doesn't constrain the name. */
function nameConstraint(node: Node): ((s: string) => boolean) | null {
  switch (node.kind) {
    case 'atom':
      return isNameAtom(node) ? nameTest(node) : null
    case 'not':
      return null
    case 'and': {
      const tests = node.children.map(nameConstraint).filter((t): t is (s: string) => boolean => t !== null)
      return tests.length === 0 ? null : (s) => tests.every((t) => t(s))
    }
    case 'or': {
      const tests = node.children.map(nameConstraint)
      if (tests.some((t) => t === null)) return null
      return (s) => tests.some((t) => t!(s))
    }
  }
}

function isNameAtom(atom: AtomNode): boolean {
  if (atom.key === null) return true
  return (atom.key === 'name' || atom.key === 'n') && (atom.op === ':' || atom.op === '=')
}
