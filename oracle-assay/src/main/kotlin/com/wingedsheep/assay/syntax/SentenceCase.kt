package com.wingedsheep.assay.syntax

/**
 * The one case rule Oracle-ese needs, applied at the text boundary rather than inside the grammar.
 *
 * An ability line is sentence-cased: `"Flying, first strike"` — the same keyword is capitalized
 * first and lowercase third. Templates are therefore written in their **mid-sentence** form
 * (`"flying"`, `"first strike"`, `"ward {2}"`), and this pass decapitalizes a line before parsing
 * and recapitalizes after printing.
 *
 * It is *not* a normalization pass in the [com.wingedsheep.assay.normalize] sense, and deliberately
 * so: it moves no information. It only lowercases a leading letter that Oracle templating
 * guarantees is uppercase, and refuses (rather than silently repairing) a line that starts with a
 * lowercase letter, since that would make the inverse a guess.
 *
 * Lines that start with a symbol or digit — `"{T}: Add {C}."`, `"1 or more"` — pass through
 * untouched in both directions, which is why the guard is on *lowercase* specifically rather than
 * on "not uppercase".
 *
 * ## A line has more than one sentence start
 *
 * `"{T}: Add {C}."` capitalizes "Add", and `"{2}, {T}: Draw a card."` capitalizes "Draw", because
 * an activated ability's effect clause begins a sentence after the cost colon. A full stop starts
 * one for the same reason: "Target creature gets +1/+3 until end of turn. Untap that creature." is
 * two sentences on one printed line. That is the same templating rule the line start obeys, applied
 * at every place Oracle applies it, so it belongs here rather than in a grammar combinator — the
 * alternative is every activated-ability and every second-clause rule spelling its verbs
 * capitalized, which is exactly the re-spelling that would stop
 * [com.wingedsheep.assay.grammar.Steps] being slottable into a new sentence context.
 *
 * The rule is Wizards' and the corpus states it: of 14,042 `": "` occurrences in Oracle text, 32
 * are followed by a lowercase letter, and all 32 are prose enumerations on the "hero's journey"
 * cards ("• Setting: a land") rather than ability costs. Of every `". "` in the corpus, 15 are
 * followed by a lowercase letter and every one is an Un-set joke card or an abbreviation
 * ("B.F.M.", "S.N.E.A.K.", "Ph.D."). Those lines decline, which is what [decapitalize] returning
 * null means, and they declined before this too.
 *
 * A **granted ability's opening quotation mark** is the fourth, and it arrives with the same
 * argument as the other three. `Enchanted creature has "Whenever this creature deals combat damage,
 * create a Blood token."` puts a capital after `has "` that no other break reaches, and the
 * alternative is a capitalized second copy of every trigger prefix in
 * [com.wingedsheep.assay.grammar.Triggers]. The corpus states this one too: of 368 `has "`
 * occurrences outside reminder text, 367 are followed by an uppercase letter or a symbol. The one
 * exception is Master of the Hunt's `has "bands with other creatures named Wolves of the Hunt."`,
 * which declined before this and declines after it.
 *
 * The break is `has "` and not a bare `"`, and not `have "` either. A bare quotation mark would also
 * match every *closing* one — harmless, since what follows is punctuation — but it would also reach
 * the 101 quotations in the corpus that are ordinary prose ("the \"legend rule\" doesn't apply"),
 * and a lowercase letter at a sentence start makes [decapitalize] refuse the whole line. `have "` is
 * the plural lord position and is genuinely the same shape, but five of its 221 occurrences are
 * "have \"bands with other legendary creatures\"" — so it would cost five lines to buy a family
 * nothing needs yet, and it stays out until something does.
 *
 * A **mode's bullet** is the third such place, and it arrived with the same argument the full stop
 * did. Normalization keeps a modal card's rows on one line (see
 * [com.wingedsheep.assay.normalize.Normalizer]), so "Choose one —\n• Destroy target artifact." puts
 * a capital after `\n• ` that no rule here would otherwise reach — and the alternative is a
 * capitalized second copy of every verb in [com.wingedsheep.assay.grammar.Steps], which is exactly
 * what this file exists to avoid. The corpus states this one too: of 2,117 bullet rows, every one
 * begins with an uppercase letter or a symbol, and none with a lowercase letter.
 */
object SentenceCase {

    /**
     * Where Oracle starts a sentence inside one ability line: the line itself, each clause after an
     * ability cost's `": "`, and each sentence after a full stop.
     *
     * Positions rather than a rewrite, because both directions need the same list and a
     * one-character-for-one-character substitution keeps every index stable between them.
     */
    private fun sentenceStarts(line: String): List<Int> =
        (listOf(0) + SENTENCE_BREAK.findAll(line).map { it.range.last + 1 }).filter { it < line.length }

    /** Line as the grammar sees it, or null when a leading character makes the inverse a guess. */
    fun decapitalize(line: String): String? {
        val chars = line.toCharArray()
        for (at in sentenceStarts(line)) {
            val c = chars[at]
            if (c.isLowerCase()) return null
            if (c.isUpperCase()) chars[at] = c.lowercaseChar()
        }
        return chars.concatToString()
    }

    /** Inverse of [decapitalize]: the printed line as Oracle templating spells it. */
    fun capitalize(line: String): String {
        val chars = line.toCharArray()
        for (at in sentenceStarts(line)) {
            val c = chars[at]
            if (c.isLowerCase()) chars[at] = c.uppercaseChar()
        }
        return chars.concatToString()
    }

    /**
     * The four places Oracle starts a new sentence *inside* one ability line: after an ability
     * cost's `": "`, after a full stop, at a mode's bullet, and inside a granted ability's opening
     * quotation mark.
     *
     * The full stop is what lets a line spelling two sentences — "Target creature gets +1/+3 until
     * end of turn. Untap that creature." — slot the ordinary effect vocabulary twice instead of
     * needing a capitalized copy of every verb. It is the same argument the cost colon carries, the
     * same one the bullet carries, and it is why this file exists rather than a `capitalized(...)`
     * combinator in the grammar.
     */
    private val SENTENCE_BREAK = Regex("""(?:: |\. |\n• |has ")""")
}

/**
 * Parse a whole sentence-cased ability line.
 *
 * ## A sentence start can be a proper noun, and the leaf is what knows
 *
 * Every sentence start is decapitalized, including one whose word is a creature type — "Sliver
 * creatures get +1/+0." and "{T}, Sacrifice a Goblin: Goblin creatures get +2/+0 until end of
 * turn." both put a proper noun where this pass lowercases. Undoing that here would mean guessing
 * which sentence starts were proper nouns, over an arbitrary number of them per line.
 *
 * It belongs to the leaf instead: [com.wingedsheep.assay.grammar.Primitives.subtype] reads a
 * lowercased subtype as well as a printed one, and only for a word the SDK names as a type, so
 * nothing is guessed and no common noun acquires a second reading. Printing is unaffected — the
 * leaf always writes the capital, and [printLine] leaves an already-capital sentence start alone.
 *
 * ## The residue, and why it is a decline rather than a retry
 *
 * That gate is a real one: the SDK publishes the creature and basic-land type lists and no others,
 * so "Equipment you control get +1/+1." names a subtype the leaf is not entitled to recognise from a
 * lowercase word, and the line declines.
 *
 * Retrying the line **as printed** was tried and removed, and the reason is worth keeping. It reads
 * position 0 with its capital intact, which makes every sentence-initial common word a candidate
 * proper noun: the differential caught it reading "**Other** creatures you control get +0/+1." as
 * creatures of a type called *Other*, byte-perfect in both directions and about a tribe Magic does
 * not have. That is the reversible-but-wrong class exactly, traded for a few dozen cards whose first
 * word is a subtype nobody published a list for. Declining is the better half of that trade, and it
 * stays the better half until the SDK publishes the remaining subtype lists.
 */
fun <T> Phrase<T>.parseLine(line: String, parseCap: Int = ParseContext.DEFAULT_PARSE_CAP): ParseOutcome<T> {
    val body = SentenceCase.decapitalize(line)
        ?: return ParseOutcome.Declined(0, listOf("a capitalized first word"), DeclineReason.NO_PARSE)
    return parseText(body, parseCap)
}

/** Print a whole sentence-cased ability line. */
fun <T> Phrase<T>.printLine(value: T): String? = unparse(value)?.let(SentenceCase::capitalize)
