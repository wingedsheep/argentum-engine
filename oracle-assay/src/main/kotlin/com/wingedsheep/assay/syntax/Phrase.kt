package com.wingedsheep.assay.syntax

import java.util.concurrent.atomic.AtomicInteger

/**
 * The Assay kernel: a **bidirectional** grammar rule.
 *
 * A [Phrase] knows how to turn a span of Oracle English into an `mtg-sdk` model value *and* how to
 * turn that value back into English. Both halves are registered together and a rule cannot be
 * constructed with only one of them (see [PhraseBuilder.finish]), because the whole gate rests on
 * the round trip:
 *
 * ```
 * print(parse(t)) == normalize(t)      // or: declined, and counted
 * ```
 *
 * Parsing returns *every* reading rather than the first one, so ambiguity has a definition instead
 * of a feeling — see [ParseOutcome].
 *
 * Two properties are load-bearing and easy to break:
 *
 * - **A phrase never throws.** A leaf that reads a malformed symbol returns no parse; it does not
 *   propagate an exception. A grammar that crashes on the corpus cannot report fineness, and
 *   "declining is success" only holds if declining is always reachable.
 * - **[unparse] must reproduce the exact text [parseAt] consumed.** Anything else passes the
 *   round trip only by accident, and the touchstone stops being a proof.
 */
abstract class Phrase<T> {

    /** Human-readable rule name, used in decline diagnostics ("expected: <name>"). */
    abstract val name: String

    /**
     * `false` marks an **alternate phrasing**: it parses but never prints. Where two English forms
     * mean the same thing, exactly one is canonical, which is what keeps printing deterministic
     * when the language isn't. A non-canonical rule is exempt from supplying `match`, since it is
     * never asked to print.
     */
    open val canonical: Boolean get() = true

    /** Identity for memoization. Assigned once per rule instance; rules are constructed at init. */
    internal val id: Int = NEXT_ID.getAndIncrement()

    /**
     * What this rule is made of, for tooling that wants to *show* the grammar rather than run it.
     *
     * Read-only and never consulted by [parseHere] or [unparse], so it cannot affect a verdict —
     * which is the whole reason it is allowed to exist here. The alternative was for each grammar
     * family to publish its own rule list, and that would describe the grammar as *declared* rather
     * than as *wired*: a family written but never reached from [com.wingedsheep.assay.grammar.Grammar]
     * would still show up. Walking from the root entry point can only show rules that are live.
     */
    open val shape: RuleShape get() = RuleShape.Opaque

    /** Parse at [from], returning every reading. Implementations must not memoize; [parseAt] does. */
    protected abstract fun parseHere(ctx: ParseContext, from: Int): List<Parse<T>>

    /** Model → text, or null when this phrase cannot express [value]. */
    abstract fun unparse(value: T): String?

    internal fun parseAt(ctx: ParseContext, from: Int): List<Parse<T>> =
        ctx.memoized(this, from) { parseHere(ctx, from) }

    private companion object {
        val NEXT_ID = AtomicInteger(0)
    }
}

/** One successful reading: a value plus the offset just past the text it consumed. */
data class Parse<out T>(val value: T, val end: Int)

/**
 * The structure of a rule, as [Phrase.shape] reports it. One case per kernel combinator.
 *
 * Deliberately not a rendering: it hands back the child [Phrase]s themselves so a walker decides how
 * deep to go and what to do at each node. Nothing here is used to parse or print.
 */
sealed interface RuleShape {

    /** The rules this one is built from, in the order the surface form mentions them. */
    val children: List<Phrase<*>> get() = emptyList()

    /** A rule declared from a surface template — the ordinary case. */
    data class Template(val template: String, val slots: Map<String, Phrase<*>>) : RuleShape {
        override val children: List<Phrase<*>> get() = slots.values.toList()
    }

    /** [oneOf] — an alternation. */
    data class Choice(val alternatives: List<Phrase<*>>) : RuleShape {
        override val children: List<Phrase<*>> get() = alternatives
    }

    /** [separated] — a separator-joined run. */
    data class Run(val item: Phrase<*>, val separator: String, val min: Int) : RuleShape {
        override val children: List<Phrase<*>> get() = listOf(item)
    }

    /** [alternate] — a spelling that parses and never prints. */
    data class Alternate(val inner: Phrase<*>) : RuleShape {
        override val children: List<Phrase<*>> get() = listOf(inner)
    }

    /** [token] — a regex leaf, where the kernel cannot see inside either half. */
    data class Leaf(val pattern: String) : RuleShape

    /** A rule that does not describe itself. No kernel combinator produces this today. */
    data object Opaque : RuleShape
}

/** Why the grammar refused a span. Every decline is counted and ranked, never approximated. */
enum class DeclineReason {
    /** No rule matched. [ParseOutcome.Declined.position] is the token it died on. */
    NO_PARSE,

    /** The span produced more readings than [ParseContext.parseCap]; treated as a decline. */
    PARSE_CAP,

    /** A rule re-entered itself at the same offset. A grammar bug, surfaced rather than hung on. */
    LEFT_RECURSION,
}

/**
 * The four outcomes of a whole-span parse, per the design's ambiguity table.
 *
 * `>1 reading, same model` is not an outcome of its own: it collapses to [Accepted] with
 * [Accepted.redundantReadings] > 0, which is grammar redundancy worth reporting but not an error.
 * `>1 reading, different models` is [Ambiguous] — a hard error that names both readings and never
 * picks one.
 */
sealed interface ParseOutcome<out T> {

    data class Accepted<T>(val value: T, val redundantReadings: Int = 0) : ParseOutcome<T>

    data class Ambiguous<T>(val readings: List<T>) : ParseOutcome<T>

    data class Declined(
        val position: Int,
        val expected: List<String>,
        val reason: DeclineReason = DeclineReason.NO_PARSE,
    ) : ParseOutcome<Nothing>
}

/**
 * Renders a decline the way `assay explain` shows it: the text, a caret under the token it died
 * on, and what the grammar was looking for there.
 */
fun ParseOutcome.Declined.explain(text: String): String = buildString {
    appendLine(text)
    append(" ".repeat(position.coerceIn(0, text.length)))
    append("^ ")
    append(
        when (reason) {
            DeclineReason.NO_PARSE -> "expected ${expected.joinToString(" | ").ifEmpty { "<nothing>" }}"
            DeclineReason.PARSE_CAP -> "too many readings (parse cap); grammar needs left-factoring here"
            DeclineReason.LEFT_RECURSION -> "rule re-entered itself here (left recursion)"
        }
    )
}

/**
 * The token [position] points at — the key the fineness report groups declines by, so that
 * "Enchant" and "Equip" surface as families rather than as a thousand unrelated lines.
 */
fun ParseOutcome.Declined.deadToken(text: String): String {
    if (position >= text.length) return "<end of line>"
    // The caret can land on the space *before* the token that could not be continued through, so
    // skip whitespace rather than reporting an empty word.
    val rest = text.substring(position).trimStart()
    if (rest.isEmpty()) return "<end of line>"
    return rest.takeWhile { !it.isWhitespace() }
}

// ---------------------------------------------------------------------------------------------
// Parse context: memoization, the parse cap, and the furthest-failure trace
// ---------------------------------------------------------------------------------------------

/**
 * Per-parse state. Holds the memo table (keyed on rule + offset, which is what makes an
 * all-readings parser affordable), the recursion guard, and the furthest point any rule failed at
 * — the latter is the entire source of the "token it died on" diagnostic.
 */
class ParseContext(val input: String, val parseCap: Int = DEFAULT_PARSE_CAP) {

    private val memo = HashMap<Long, List<Parse<*>>>()
    private val active = HashSet<Long>()

    internal var furthest: Int = 0
        private set
    private val expectations = LinkedHashSet<String>()

    /** Set when any span blew the parse cap, or when a rule re-entered itself. */
    internal var declineOverride: DeclineReason? = null
        private set

    /** Record that [expected] was wanted at [at] and not found. Only the furthest point survives. */
    fun fail(at: Int, expected: String) {
        if (at > furthest) {
            furthest = at
            expectations.clear()
        }
        if (at == furthest) expectations.add(expected)
    }

    internal fun expected(): List<String> = expectations.toList()

    @Suppress("UNCHECKED_CAST")
    internal fun <T> memoized(phrase: Phrase<T>, from: Int, compute: () -> List<Parse<T>>): List<Parse<T>> {
        val key = phrase.id.toLong() shl 32 or from.toLong()
        memo[key]?.let { return it as List<Parse<T>> }
        if (!active.add(key)) {
            // Left recursion. Report it rather than overflowing the stack: a grammar bug should
            // show up as a named decline in the report, not as a crashed corpus run.
            declineOverride = DeclineReason.LEFT_RECURSION
            return emptyList()
        }
        val result = try {
            compute()
        } finally {
            active.remove(key)
        }
        if (result.size > parseCap) {
            declineOverride = DeclineReason.PARSE_CAP
            val capped = result.take(parseCap)
            memo[key] = capped
            return capped
        }
        memo[key] = result
        return result
    }

    companion object {
        /**
         * Readings kept per (rule, offset). Oracle sentences are short and the grammar is
         * anchored, so a span that produces more than this is a left-factoring bug rather than
         * genuine ambiguity — and the design says to treat hitting the cap as a decline.
         */
        const val DEFAULT_PARSE_CAP = 64
    }
}

/**
 * Parse [text] in full: only readings that consume the whole span count.
 *
 * This is the only entry point that classifies; everything inside the grammar deals in partial
 * readings. Distinctness is by model equality, so two rules spelling one meaning are redundancy
 * ([ParseOutcome.Accepted.redundantReadings]) rather than ambiguity.
 */
fun <T> Phrase<T>.parseText(text: String, parseCap: Int = ParseContext.DEFAULT_PARSE_CAP): ParseOutcome<T> {
    val ctx = ParseContext(text, parseCap)
    val readings = parseAt(ctx, 0)
    val complete = readings.filter { it.end == text.length }

    if (complete.isEmpty()) {
        // A reading that stopped short is itself a diagnostic: the grammar understood a prefix and
        // wanted the line to end there. Fold those ends into the trace so the caret lands on the
        // first thing it could not continue through.
        readings.forEach { ctx.fail(it.end, "end of line") }
        return ParseOutcome.Declined(ctx.furthest, ctx.expected(), ctx.declineOverride ?: DeclineReason.NO_PARSE)
    }
    ctx.declineOverride?.let { return ParseOutcome.Declined(ctx.furthest, ctx.expected(), it) }

    val distinct = complete.map { it.value }.distinct()
    return if (distinct.size == 1) {
        ParseOutcome.Accepted(distinct.single(), redundantReadings = complete.size - 1)
    } else {
        ParseOutcome.Ambiguous(distinct)
    }
}

// ---------------------------------------------------------------------------------------------
// Templates
// ---------------------------------------------------------------------------------------------

/**
 * A template is the surface form, shared by both directions, so the printer cannot drift from the
 * parser: `"destroy target {obj}"`.
 *
 * **Slot syntax.** `{name}` where `name` starts with a *lowercase* letter. Every other brace run
 * is literal text — which is deliberate: Oracle-ese is full of `{2}`, `{U}`, `{T}` and `{E}`, and a
 * template must be able to spell them without escaping.
 */
internal sealed interface TemplatePart {
    @JvmInline
    value class Literal(val text: String) : TemplatePart

    @JvmInline
    value class SlotRef(val slot: String) : TemplatePart
}

private val SLOT_RE = Regex("""\{([a-z][A-Za-z0-9]*)}""")

internal fun compileTemplate(template: String): List<TemplatePart> {
    val parts = mutableListOf<TemplatePart>()
    var cursor = 0
    for (m in SLOT_RE.findAll(template)) {
        if (m.range.first > cursor) parts.add(TemplatePart.Literal(template.substring(cursor, m.range.first)))
        parts.add(TemplatePart.SlotRef(m.groupValues[1]))
        cursor = m.range.last + 1
    }
    if (cursor < template.length) parts.add(TemplatePart.Literal(template.substring(cursor)))
    return parts
}

/**
 * The values a template's slots bound, in both directions: `build` reads them, `match` produces
 * them. Untyped on purpose — a slot's type is the slot phrase's type, and the rule that registers
 * both halves is the one place that knows it.
 */
class Bindings private constructor(private val values: Map<String, Any?>) {

    operator fun contains(slot: String): Boolean = slot in values

    @Suppress("UNCHECKED_CAST")
    fun <V> value(slot: String): V {
        require(slot in values) { "no binding for slot '$slot'" }
        return values[slot] as V
    }

    fun int(slot: String): Int = value(slot)

    fun text(slot: String): String = value(slot)

    internal fun raw(slot: String): Any? = values[slot]

    companion object {
        val EMPTY = Bindings(emptyMap())
        internal fun of(values: Map<String, Any?>) = Bindings(values)
    }
}

/** Builds the [Bindings] a `match` half returns: `bind("n" to it.count)`. */
fun bind(vararg pairs: Pair<String, Any?>): Bindings = Bindings.of(pairs.toMap())

class PhraseBuilder<T> internal constructor(val template: String, val ruleName: String?) {

    private val slots = LinkedHashMap<String, Phrase<*>>()
    private val spellings = mutableListOf<Pair<String, String>>()
    private var builder: ((Bindings) -> T?)? = null
    private var matcher: ((T) -> Bindings?)? = null

    /** See [Phrase.canonical]: set `false` for an alternate phrasing that parses but never prints. */
    var canonical: Boolean = true

    /** Registers the sub-phrase a `{name}` placeholder stands for. Slots are phrases, recursively. */
    fun slot(name: String, phrase: Phrase<*>) {
        require(slots.put(name, phrase) == null) { "slot '$name' registered twice in \"$template\"" }
    }

    /**
     * Registers an **additional surface spelling** of this same rule: the same slots, the same
     * `build`, the same `match`, and it never prints.
     *
     * The kernel's answer to "one printed form per model" when the second form is not a second
     * *rule* but the same one with a word somewhere else. Writing it as a sibling [phrase] would
     * mean a second copy of `build` and `match` — two halves that agree until someone edits one of
     * them, which is the drift this whole module is built to make impossible. Sharing the closures
     * makes the two spellings the same rule by construction, so a change to what the sentence means
     * cannot reach one spelling and miss the other.
     *
     * It is deliberately *not* a duration, a word order, or any other piece of Oracle vocabulary:
     * [template] is public so a grammar family can derive the second spelling from the first and
     * own the derivation itself (see `Durations.fronted`). The kernel only knows that a rule may
     * have more than one surface form and that exactly one of them prints.
     */
    fun alsoSpelled(template: String, name: String) {
        spellings.add(template to name)
    }

    /** text → model. Returning null means the surface form is grammatical but denotes nothing. */
    fun build(f: (Bindings) -> T?) {
        builder = f
    }

    /** model → text. Returning null means this rule cannot express that value. */
    fun match(f: (T) -> Bindings?) {
        matcher = f
    }

    /** Every spelling of a rule binds the same slots, so each one is checked against them. */
    private fun compile(template: String): List<TemplatePart> {
        val parts = compileTemplate(template)
        val referenced = parts.filterIsInstance<TemplatePart.SlotRef>().map { it.slot }.toSet()
        val missing = referenced - slots.keys
        require(missing.isEmpty()) { "template \"$template\" references unregistered slot(s) $missing" }
        val unused = slots.keys - referenced
        require(unused.isEmpty()) { "slot(s) $unused registered but absent from template \"$template\"" }
        return parts
    }

    internal fun finish(): Phrase<T> {
        val parts = compile(template)

        val build = requireNotNull(builder) { "rule \"$template\" has no build { } — a phrase must parse" }
        // Bidirectional or it doesn't ship. The single exception is a non-canonical alternate,
        // which by construction is never asked to print.
        val match = matcher
        require(match != null || !canonical) {
            "rule \"$template\" has no match { } — a canonical phrase must print as well as parse. " +
                "Set `canonical = false` if it is an alternate phrasing that should never print."
        }
        val bound = slots.toMap()
        val rule = TemplatePhrase(ruleName ?: template, template, parts, bound, build, match, canonical)
        if (spellings.isEmpty()) return rule

        // An extra spelling is an alternate by construction — it shares this rule's `build` and is
        // never asked to print, which is what keeps the printed form determined by the model.
        val alternates = spellings.map { (alsoTemplate, alsoName) ->
            alternate(TemplatePhrase(alsoName, alsoTemplate, compile(alsoTemplate), bound, build, null, false))
        }
        val choice = oneOf(ruleName ?: template, listOf(rule) + alternates)
        return if (canonical) choice else alternate(choice)
    }
}

private class TemplatePhrase<T>(
    override val name: String,
    private val template: String,
    private val parts: List<TemplatePart>,
    private val slots: Map<String, Phrase<*>>,
    private val build: (Bindings) -> T?,
    private val match: ((T) -> Bindings?)?,
    override val canonical: Boolean,
) : Phrase<T>() {

    override val shape: RuleShape get() = RuleShape.Template(template, slots)

    override fun parseHere(ctx: ParseContext, from: Int): List<Parse<T>> {
        var frontier = listOf(emptyMap<String, Any?>() to from)
        for (part in parts) {
            if (frontier.isEmpty()) return emptyList()
            frontier = when (part) {
                is TemplatePart.Literal -> frontier.mapNotNull { (bound, pos) ->
                    if (ctx.input.startsWith(part.text, pos)) {
                        bound to pos + part.text.length
                    } else {
                        ctx.fail(pos, "\"${part.text}\"")
                        null
                    }
                }

                is TemplatePart.SlotRef -> frontier.flatMap { (bound, pos) ->
                    val slot = slots.getValue(part.slot)
                    val readings = slot.parseAt(ctx, pos)
                    if (readings.isEmpty()) ctx.fail(pos, slot.name)
                    readings.map { (bound + (part.slot to it.value)) to it.end }
                }
            }
        }
        return frontier.mapNotNull { (bound, end) ->
            build(Bindings.of(bound))?.let { Parse(it, end) }
        }
    }

    override fun unparse(value: T): String? {
        val matcher = match ?: return null
        val bindings = matcher(value) ?: return null
        return buildString {
            for (part in parts) {
                when (part) {
                    is TemplatePart.Literal -> append(part.text)
                    is TemplatePart.SlotRef -> {
                        val slot = slots.getValue(part.slot)
                        @Suppress("UNCHECKED_CAST")
                        val printed = (slot as Phrase<Any?>).unparse(bindings.raw(part.slot)) ?: return null
                        append(printed)
                    }
                }
            }
        }
    }

    override fun toString(): String = "phrase(\"$template\")"
}

/**
 * Declares a rule from its surface template.
 *
 * ```kotlin
 * val annihilator = phrase<KeywordAbility>("annihilator {n}") {
 *     slot("n", cardinal)
 *     build { KeywordAbility.Numeric(Keyword.ANNIHILATOR, it.int("n")) }
 *     match { (it as? KeywordAbility.Numeric)?.takeIf { k -> k.keyword == Keyword.ANNIHILATOR }
 *         ?.let { k -> bind("n" to k.n) } }
 * }
 * ```
 */
fun <T> phrase(template: String, name: String? = null, block: PhraseBuilder<T>.() -> Unit): Phrase<T> =
    PhraseBuilder<T>(template, name).apply(block).finish()

/**
 * A slotless rule for one fixed value — the shape almost every simple keyword takes
 * ("flying" ⇄ `Simple(FLYING)`). Equivalent to a [phrase] whose `build` is a constant and whose
 * `match` is an equality test, which is worth a helper because there are dozens of them and the
 * long form invites copy-paste drift between the two halves.
 */
fun <T> constant(template: String, value: T, canonical: Boolean = true): Phrase<T> =
    phrase(template) {
        this.canonical = canonical
        build { value }
        match { if (it == value) Bindings.EMPTY else null }
    }

// ---------------------------------------------------------------------------------------------
// Combinators
// ---------------------------------------------------------------------------------------------

/**
 * Alternatives. Parsing tries every branch (so ambiguity is *detected*, not hidden by ordering);
 * printing uses the first **canonical** branch that can express the value.
 */
fun <T> oneOf(name: String, alternatives: List<Phrase<T>>): Phrase<T> = OneOfPhrase(name, alternatives)

fun <T> oneOf(name: String, vararg alternatives: Phrase<T>): Phrase<T> = OneOfPhrase(name, alternatives.toList())

private class OneOfPhrase<T>(override val name: String, private val alternatives: List<Phrase<T>>) : Phrase<T>() {

    init {
        require(alternatives.isNotEmpty()) { "oneOf(\"$name\") needs at least one alternative" }
    }

    override val shape: RuleShape get() = RuleShape.Choice(alternatives)

    override fun parseHere(ctx: ParseContext, from: Int): List<Parse<T>> =
        alternatives.flatMap { it.parseAt(ctx, from) }

    override fun unparse(value: T): String? =
        alternatives.asSequence().filter { it.canonical }.firstNotNullOfOrNull { it.unparse(value) }
}

/**
 * Marks any phrase as an **alternate spelling**: it parses, and never prints.
 *
 * The design's `canonical = false`, available to combinators as well as to templates. Where two
 * printed forms carry the same meaning, exactly one of them is what the printer emits — otherwise
 * printing is underdetermined and the round trip has nothing to compare against. A card printed in
 * the alternate form comes back as a *variant* rather than a failure: the model survives, only the
 * spelling is normalized.
 */
fun <T> alternate(inner: Phrase<T>): Phrase<T> = AlternatePhrase(inner)

private class AlternatePhrase<T>(private val inner: Phrase<T>) : Phrase<T>() {
    override val name: String get() = "${inner.name} (alternate)"
    override val canonical: Boolean get() = false
    override val shape: RuleShape get() = RuleShape.Alternate(inner)
    override fun parseHere(ctx: ParseContext, from: Int): List<Parse<T>> = inner.parseAt(ctx, from)
    override fun unparse(value: T): String? = null
}

/**
 * A [separator]-joined run of [item], at least [min] long. Parsing returns every prefix length,
 * which the whole-span filter in [parseText] resolves — only the run that reaches the end of the
 * line survives, so the redundancy costs nothing and the rule needs no lookahead.
 *
 * [min] exists for the alternate-separator case: a one-element run has no separator in it, so a
 * comma list and a semicolon list would both match it and every single keyword would report as
 * grammar redundancy. Requiring two makes the alternate rule apply only where it is distinguishable.
 */
fun <T> separated(name: String, item: Phrase<T>, separator: String = ", ", min: Int = 1): Phrase<List<T>> =
    SeparatedPhrase(name, item, separator, min)

private class SeparatedPhrase<T>(
    override val name: String,
    private val item: Phrase<T>,
    private val separator: String,
    private val min: Int,
) : Phrase<List<T>>() {

    init {
        require(min >= 1) { "separated(\"$name\") needs min >= 1" }
    }

    override val shape: RuleShape get() = RuleShape.Run(item, separator, min)

    override fun parseHere(ctx: ParseContext, from: Int): List<Parse<List<T>>> {
        val out = mutableListOf<Parse<List<T>>>()
        var frontier = item.parseAt(ctx, from).map { Parse(listOf(it.value), it.end) }
        if (frontier.isEmpty()) ctx.fail(from, item.name)
        while (frontier.isNotEmpty()) {
            frontier.filterTo(out) { it.value.size >= min }
            if (out.size > ctx.parseCap) break
            frontier = frontier.flatMap { prefix ->
                if (!ctx.input.startsWith(separator, prefix.end)) {
                    ctx.fail(prefix.end, "\"$separator\"")
                    emptyList()
                } else {
                    val next = prefix.end + separator.length
                    val readings = item.parseAt(ctx, next)
                    if (readings.isEmpty()) ctx.fail(next, item.name)
                    readings.map { Parse(prefix.value + it.value, it.end) }
                }
            }
        }
        return out
    }

    override fun unparse(value: List<T>): String? {
        if (value.size < min) return null
        val printed = value.map { item.unparse(it) ?: return null }
        return printed.joinToString(separator)
    }
}

/**
 * A leaf rule over a regular expression — cardinals, mana symbols, capitalized names. The kernel
 * cannot check these two halves against each other the way a template can, so [unparse] is
 * *verified against [read]* on every call: a leaf that prints something it would not read back is
 * a bug we refuse to let reach the touchstone, where it would surface as an unexplained mismatch
 * far from its cause.
 */
fun <T> token(
    name: String,
    pattern: Regex,
    read: (String) -> T?,
    write: (T) -> String?,
): Phrase<T> = TokenPhrase(name, pattern, read, write)

private class TokenPhrase<T>(
    override val name: String,
    private val pattern: Regex,
    private val read: (String) -> T?,
    private val write: (T) -> String?,
) : Phrase<T>() {

    override val shape: RuleShape get() = RuleShape.Leaf(pattern.pattern)

    override fun parseHere(ctx: ParseContext, from: Int): List<Parse<T>> {
        val m = pattern.matchAt(ctx.input, from) ?: run { ctx.fail(from, name); return emptyList() }
        // A leaf must never throw: malformed input is a decline, not a crashed corpus run.
        val value = runCatching { read(m.value) }.getOrNull() ?: run { ctx.fail(from, name); return emptyList() }
        return listOf(Parse(value, from + m.value.length))
    }

    override fun unparse(value: T): String? {
        val text = runCatching { write(value) }.getOrNull() ?: return null
        val m = pattern.matchAt(text, 0)
        if (m == null || m.value.length != text.length) return null
        return if (runCatching { read(text) }.getOrNull() == value) text else null
    }
}

/**
 * A rule that resolves [inner] on **first use** rather than at construction.
 *
 * Every grammar family here is an `object` whose rules are built during initialization, and that
 * makes the reference graph between families an initialization *order*: a family that slots another
 * one is constructed after it. Two families that each slot the other therefore cannot both be
 * second, and the JVM does not refuse — it hands the one still initializing back a half-built
 * object, so the slot reads `null` and the failure surfaces as a decline somewhere else entirely.
 * That is the trap [com.wingedsheep.assay.grammar.Primitives]' own note describes, one file wider.
 *
 * The cycle is not an accident of layout: a noun phrase can be qualified by a count
 * ("card with mana value less than or equal to **the number of lands you control**") and a count is
 * taken over a noun phrase ("the number of **lands you control**"), so English genuinely nests each
 * inside the other. Duplicating either vocabulary to break the cycle is the one thing this module
 * exists to prevent, so the kernel carries the indirection instead — exactly the reason parser
 * combinators over a recursive language usually have one.
 *
 * [inner] is called at most once and must not be called during the *caller's* own initialization,
 * which is the whole point; a slot declared this way is otherwise an ordinary sub-phrase. It is not
 * a way to write left recursion: re-entering a rule at the same offset is still
 * [DeclineReason.LEFT_RECURSION], detected by [ParseContext] as before.
 */
fun <T> deferred(name: String, inner: () -> Phrase<T>): Phrase<T> = DeferredPhrase(name, inner)

private class DeferredPhrase<T>(override val name: String, resolve: () -> Phrase<T>) : Phrase<T>() {
    private val inner: Phrase<T> by lazy(resolve)
    override val canonical: Boolean get() = inner.canonical

    // A one-branch alternation, which is operationally what a deferred reference is: parse it, print
    // it, nothing else. Reported as its own node rather than as the inner rule's so the grammar view
    // shows where the indirection is, and so the walker's visited set still terminates the cycle.
    override val shape: RuleShape get() = RuleShape.Choice(listOf(inner))
    override fun parseHere(ctx: ParseContext, from: Int): List<Parse<T>> = inner.parseAt(ctx, from)
    override fun unparse(value: T): String? = inner.unparse(value)
}
