package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Phase 1's whole grammar: keyword abilities, and nothing else.
 *
 * Every rule targets an `mtg-sdk` [KeywordAbility] directly — there is no Assay IR — and every
 * value is built through the SDK's own companion factories rather than raw constructors, for the
 * same reason cards are: the factories are the curated surface, and a rule that reaches past them
 * ages badly when the underlying shape changes.
 *
 * **Templates are written mid-sentence** (`"flying"`, not `"Flying"`). Sentence case is applied at
 * the line boundary by [com.wingedsheep.assay.syntax.SentenceCase], which is what lets one rule
 * serve both "Flying, vigilance" and "Vigilance, flying".
 *
 * ### What is deliberately absent
 *
 * The commonest keyword-only lines in the corpus that this grammar **declines** are declines for a
 * reason worth reading, because a decline names a missing capability in Argentum's own vocabulary:
 *
 * - `Enchant …` (1,289 cards) — the SDK models enchant as an aura's attachment restriction, not as
 *   a [KeywordAbility]. There is nothing here to parse *into*.
 * - `Equip {N}` (621 cards) — equip is a field on `CardDefinition` (`equipCost`), likewise not a
 *   [KeywordAbility]. Two keyword abilities of the same shape, modelled two different ways.
 * - `Devoid`, `Partner`, `Infect`, `Fuse`, `Exalted`, `Myriad`, `Melee`, `Skulk` — no [Keyword]
 *   enum constant exists, so the capability genuinely is not in the SDK yet.
 *
 * That list is the fineness report's top declines, and it is exactly the backlog signal the design
 * promises: "can Argentum express this?" collapses into "did it parse?".
 */
object Keywords {

    // ---------------------------------------------------------------------------------------
    // Rule shapes shared by whole families
    // ---------------------------------------------------------------------------------------

    /** "flashback {2}{R}" — a keyword whose only parameter is a mana cost. */
    private fun costKeyword(
        surface: String,
        toModel: (ManaCost) -> KeywordAbility,
        fromModel: (KeywordAbility) -> ManaCost?,
    ): Phrase<KeywordAbility> = phrase("$surface {cost}", name = "$surface <cost>") {
        slot("cost", Primitives.manaCost)
        build { toModel(it.value("cost")) }
        match { ability -> fromModel(ability)?.let { bind("cost" to it) } }
    }

    /**
     * "annihilator 2" — a keyword parameterized by a single integer, which the SDK models
     * uniformly as [KeywordAbility.Numeric]. The `onceEachTurn` variant ("Crew 1. Activate only
     * once each turn.") is a different surface form and is not matched here, so it declines
     * rather than printing back without its second sentence.
     */
    private fun numericKeyword(
        surface: String,
        keyword: Keyword,
        toModel: (Int) -> KeywordAbility,
    ): Phrase<KeywordAbility> = phrase("$surface {n}", name = "$surface <n>") {
        slot("n", Primitives.cardinal)
        build { toModel(it.int("n")) }
        match { ability ->
            (ability as? KeywordAbility.Numeric)
                ?.takeIf { it.keyword == keyword && !it.onceEachTurn }
                ?.let { bind("n" to it.n) }
        }
    }

    /**
     * "flying" — a keyword with no parameters at all, the bulk of the corpus by line count.
     *
     * A *spelling* rather than a rule, because the same vocabulary is needed twice over: as a
     * [KeywordAbility] when the word is the whole line, and as a bare [Keyword] when another
     * sentence names it ("Enchanted creature has flying."). Both lists are derived from this one, so
     * a keyword can never be readable in one context and not the other, and neither list can drift
     * to a different surface form for the same word.
     */
    private fun simple(keyword: Keyword, surface: String = keyword.displayName.lowercase()): Pair<Keyword, String> =
        keyword to surface

    // ---------------------------------------------------------------------------------------
    // Simple keywords
    // ---------------------------------------------------------------------------------------

    /**
     * Allowlisted rather than derived from [Keyword.entries], because the enum contains constants
     * that never stand alone as a line ("Ward", "Protection", "Craft") and constants whose printed
     * form is an ability-word prefix rather than a keyword ("Max speed — …"). Deriving would mint
     * rules for text that does not exist, and every one of them would be a chance to mis-print.
     *
     * Three keywords are absent on purpose because the SDK gives them a dedicated
     * [KeywordAbility] rather than [KeywordAbility.Simple]; they are registered below as
     * [dedicatedObjects]. Registering both spellings would be genuine ambiguity.
     */
    private val SIMPLE_KEYWORDS: List<Pair<Keyword, String>> = listOf(
        // Evasion
        simple(Keyword.FLYING),
        simple(Keyword.MENACE),
        simple(Keyword.INTIMIDATE),
        simple(Keyword.FEAR),
        simple(Keyword.SHADOW),
        simple(Keyword.HORSEMANSHIP),
        // Landwalk
        simple(Keyword.SWAMPWALK),
        simple(Keyword.FORESTWALK),
        simple(Keyword.ISLANDWALK),
        simple(Keyword.MOUNTAINWALK),
        simple(Keyword.PLAINSWALK),
        simple(Keyword.DESERTWALK),
        // Combat
        simple(Keyword.FIRST_STRIKE),
        simple(Keyword.DOUBLE_STRIKE),
        simple(Keyword.TRAMPLE),
        simple(Keyword.DEATHTOUCH),
        simple(Keyword.LIFELINK),
        simple(Keyword.VIGILANCE),
        simple(Keyword.REACH),
        simple(Keyword.PROVOKE),
        simple(Keyword.BANDING),
        simple(Keyword.FLANKING),
        simple(Keyword.WITHER),
        simple(Keyword.TRAINING),
        // Defense / speed
        simple(Keyword.DEFENDER),
        simple(Keyword.INDESTRUCTIBLE),
        simple(Keyword.HEXPROOF),
        simple(Keyword.SHROUD),
        simple(Keyword.HASTE),
        simple(Keyword.FLASH),
        // Cost reduction and casting
        simple(Keyword.CONVOKE),
        simple(Keyword.DELVE),
        simple(Keyword.IMPROVISE),
        simple(Keyword.STORM),
        simple(Keyword.CASCADE),
        simple(Keyword.REBOUND),
        // Static / triggered keyword abilities
        simple(Keyword.PROWESS),
        simple(Keyword.CHANGELING),
        simple(Keyword.SOULBOND),
        simple(Keyword.PERSIST),
        simple(Keyword.UNDYING),
        simple(Keyword.DECAYED),
        simple(Keyword.EXPLOIT),
        simple(Keyword.RIOT),
        simple(Keyword.ASCEND),
        simple(Keyword.DAYBOUND),
        simple(Keyword.NIGHTBOUND),
        simple(Keyword.START_YOUR_ENGINES, surface = "start your engines!"),
    )

    private val simpleKeywords: List<Phrase<KeywordAbility>> =
        SIMPLE_KEYWORDS.map { (keyword, surface) -> constant(surface, KeywordAbility.of(keyword)) }

    /**
     * The same words as a bare [Keyword] — what a sentence needs when it *names* a keyword instead
     * of having one ("Enchanted creature has flying.", `GrantKeyword(Keyword.FLYING)`).
     *
     * Only the parameterless keywords are here, and that is the honest boundary rather than an
     * omission: a parameterized keyword names a value the SDK's granted-keyword statics have nowhere
     * to put, since they carry a keyword and not a `KeywordAbility`. "Enchanted creature has ward
     * {2}." therefore declines, which is the correct answer until the SDK can hold the parameter.
     */
    val keyword: Phrase<Keyword> =
        oneOf("a keyword", SIMPLE_KEYWORDS.map { (keyword, surface) -> constant(surface, keyword) })

    /**
     * "trample", "lifelink and indestructible", "trample, hexproof, and indestructible" — the
     * keywords **one** grant clause hands out.
     *
     * A grant sentence names a list, not a keyword. CR 702's templating joins the members with the
     * ordinary English series comma and a final "and", and the SDK models each member as its own
     * `GrantKeyword` — so the printed run and the model's list are the same list, and every rule that
     * grants (to a target, to a group, to the source, to an enchanted permanent) slots this instead
     * of [keyword].
     *
     * This is [Primitives.scopeRun]'s shape one level up, with one member added: the run includes
     * the **singleton**, because a grant rule has exactly one keyword slot and the count is what
     * varies. The three alternatives take disjoint list sizes, so nothing is left for the printer to
     * choose — which is what lets one rule replace the "gains X", "gains X and Y" pairs the file used
     * to carry as separate rules, rather than adding a third.
     */
    private val keywordSingleton: Phrase<List<Keyword>> = phrase("{one}", name = "one keyword") {
        slot("one", keyword)
        build { listOf(it.value<Keyword>("one")) }
        match { it.singleOrNull()?.let { only -> bind("one" to only) } }
    }

    private val keywordPair: Phrase<List<Keyword>> =
        phrase("{first} and {second}", name = "two keywords") {
            slot("first", keyword)
            slot("second", keyword)
            build { listOf(it.value<Keyword>("first"), it.value<Keyword>("second")) }
            match { keywords ->
                keywords.takeIf { it.size == 2 }?.let { bind("first" to it[0], "second" to it[1]) }
            }
        }

    /** "trample, hexproof, and indestructible" — three or more, with the printed Oxford comma. */
    private val keywordSeries: Phrase<List<Keyword>> =
        phrase("{most}, and {last}", name = "three or more keywords") {
            slot("most", separated("keywords", keyword, ", ", min = 2))
            slot("last", keyword)
            build { it.value<List<Keyword>>("most") + it.value<Keyword>("last") }
            match { keywords ->
                keywords.takeIf { it.size >= 3 }
                    ?.let { bind("most" to it.dropLast(1), "last" to it.last()) }
            }
        }

    /** One or more keywords, joined the way printed Oracle text joins them. See above. */
    val keywordRun: Phrase<List<Keyword>> =
        oneOf("one or more keywords", keywordSingleton, keywordPair, keywordSeries)

    /**
     * Two or more, for the one position where the singleton is spoken for: a static-ability *line*
     * already has a single-ability rule ([Statics.attachedKeyword]) reached through the one-element
     * lift, so admitting the singleton here would give one text two readings of the same model.
     */
    val severalKeywords: Phrase<List<Keyword>> =
        oneOf("two or more keywords", keywordPair, keywordSeries)

    /**
     * Keywords the SDK models as their own object rather than as [KeywordAbility.Simple].
     *
     * Flanking used to be here and is now an ordinary [simple] rule. The differential found the
     * hand-written corpus spelling it `Simple(FLANKING)` while this grammar emitted a dedicated
     * `Flanking` object, and the cards were right: the engine synthesizes flanking's trigger from
     * the projected *keyword* set, which a variant overriding no `keyword` never reaches. The
     * object is gone from `mtg-sdk`.
     */
    private val dedicatedObjects: List<Phrase<KeywordAbility>> = listOf(
        constant("conspire", KeywordAbility.conspire()),
        constant("increment", KeywordAbility.Increment),
    )

    // ---------------------------------------------------------------------------------------
    // Parameterized keywords
    // ---------------------------------------------------------------------------------------

    private val protection: Phrase<KeywordAbility> = phrase("protection from {scope}", name = "protection") {
        slot("scope", Primitives.protectionScope)
        build { KeywordAbility.Protection(it.value("scope")) }
        match { (it as? KeywordAbility.Protection)?.let { p -> bind("scope" to p.scope) } }
    }

    private val hexproofFrom: Phrase<KeywordAbility> = phrase("hexproof from {scope}", name = "hexproof from") {
        slot("scope", Primitives.protectionScope)
        build { KeywordAbility.Hexproof(it.value("scope")) }
        match { (it as? KeywordAbility.Hexproof)?.let { h -> bind("scope" to h.scope) } }
    }

    /**
     * "protection from black and from red" — **two** abilities, not one with two colours.
     *
     * CR 702.16g: *"'Protection from [quality A] and from [quality B]' is shorthand for 'protection
     * from [quality A]' and 'protection from [quality B]'; it behaves as two separate protection
     * abilities."* CR 702.11f says the same for hexproof, which is why one shape serves both.
     *
     * This is the rule the differential gate was built to find. The old reading — one
     * `Protection(Colors([BLACK, RED]))` — round-tripped byte-exact forever while disagreeing with
     * every hand-written card that spells it, so the touchstone structurally could not catch it and
     * only a comparison against the corpus could.
     *
     * A run yields several abilities from one phrase, which is why [Grammar] parses a keyword line
     * as a list of *groups* rather than a list of abilities.
     */
    private fun qualityRun(
        surface: String,
        toModel: (ProtectionScope) -> KeywordAbility,
        scopeOf: (KeywordAbility) -> ProtectionScope?,
    ): Phrase<List<KeywordAbility>> =
        phrase("$surface from {scopes}", name = "$surface from two or more qualities") {
            slot("scopes", Primitives.scopeRun)
            build { it.value<List<ProtectionScope>>("scopes").map(toModel) }
            match { abilities ->
                if (abilities.size < 2) return@match null
                val scopes = abilities.map { scopeOf(it) ?: return@match null }
                bind("scopes" to scopes)
            }
        }

    /** The rules that denote more than one keyword ability. See [qualityRun]. */
    val runs: List<Phrase<List<KeywordAbility>>> = listOf(
        qualityRun("protection", { KeywordAbility.Protection(it) }) { (it as? KeywordAbility.Protection)?.scope },
        qualityRun("hexproof", { KeywordAbility.Hexproof(it) }) { (it as? KeywordAbility.Hexproof)?.scope },
    )

    private val wardMana: Phrase<KeywordAbility> = phrase("ward {cost}", name = "ward <cost>") {
        slot("cost", Primitives.manaCost)
        build { KeywordAbility.ward(it.value<ManaCost>("cost").toString()) }
        match { ability ->
            wardManaCost(ability)?.let { bind("cost" to it) }
        }
    }

    /** "Ward—Pay 2 life." — the em-dash forms are full sentences and carry a terminal period. */
    private val wardLife: Phrase<KeywordAbility> = phrase("ward—Pay {n} life.", name = "ward—pay life") {
        slot("n", Primitives.cardinal)
        build { KeywordAbility.wardLife(it.int("n")) }
        match { ability ->
            (ability as? KeywordAbility.Ward)?.cost
                ?.let { it as? com.wingedsheep.sdk.scripting.effects.WardCost.Life }
                ?.let { bind("n" to it.amount) }
        }
    }

    private val affinityForType: Phrase<KeywordAbility> = oneOf(
        "affinity for a card type",
        listOf(
            CardType.ARTIFACT, CardType.CREATURE, CardType.ENCHANTMENT, CardType.LAND,
            CardType.INSTANT, CardType.PLANESWALKER,
        )
            .map { type -> constant("affinity for ${type.displayName.lowercase()}s", KeywordAbility.Affinity(type)) },
    )

    private val affinityForSubtype: Phrase<KeywordAbility> =
        phrase("affinity for {subtype}", name = "affinity for a subtype") {
            slot("subtype", Primitives.pluralSubtype)
            build { KeywordAbility.AffinityForSubtype(it.value("subtype")) }
            match { (it as? KeywordAbility.AffinityForSubtype)?.let { a -> bind("subtype" to a.forSubtype) } }
        }

    private val suspend: Phrase<KeywordAbility> = phrase("suspend {n}—{cost}", name = "suspend") {
        slot("n", Primitives.cardinal)
        slot("cost", Primitives.manaCost)
        build { KeywordAbility.suspend(it.value<ManaCost>("cost").toString(), it.int("n")) }
        match {
            (it as? KeywordAbility.Suspend)?.let { s -> bind("n" to s.timeCounters, "cost" to s.cost) }
        }
    }

    private val impending: Phrase<KeywordAbility> = phrase("impending {n}—{cost}", name = "impending") {
        slot("n", Primitives.cardinal)
        slot("cost", Primitives.manaCost)
        build { KeywordAbility.impending(it.int("n"), it.value<ManaCost>("cost").toString()) }
        match { (it as? KeywordAbility.Impending)?.let { i -> bind("n" to i.time, "cost" to i.cost) } }
    }

    private val splice: Phrase<KeywordAbility> = phrase("splice onto Arcane {cost}", name = "splice onto Arcane") {
        slot("cost", Primitives.manaCost)
        build { KeywordAbility.splice(it.value<ManaCost>("cost").toString(), Subtype.ARCANE) }
        match {
            (it as? KeywordAbility.Splice)?.takeIf { s -> s.onto == Subtype.ARCANE }?.let { s -> bind("cost" to s.cost) }
        }
    }

    private val numericKeywords: List<Phrase<KeywordAbility>> = listOf(
        numericKeyword("annihilator", Keyword.ANNIHILATOR, KeywordAbility::annihilator),
        numericKeyword("bushido", Keyword.BUSHIDO, KeywordAbility::bushido),
        numericKeyword("rampage", Keyword.RAMPAGE, KeywordAbility::rampage),
        numericKeyword("absorb", Keyword.ABSORB, KeywordAbility::absorb),
        numericKeyword("afflict", Keyword.AFFLICT, KeywordAbility::afflict),
        numericKeyword("toxic", Keyword.TOXIC, KeywordAbility::toxic),
        numericKeyword("crew", Keyword.CREW) { KeywordAbility.crew(it) },
        numericKeyword("saddle", Keyword.SADDLE, KeywordAbility::saddle),
        numericKeyword("modular", Keyword.MODULAR, KeywordAbility::modular),
        numericKeyword("fading", Keyword.FADING, KeywordAbility::fading),
        numericKeyword("vanishing", Keyword.VANISHING, KeywordAbility::vanishing),
        numericKeyword("renown", Keyword.RENOWN, KeywordAbility::renown),
        numericKeyword("fabricate", Keyword.FABRICATE, KeywordAbility::fabricate),
        numericKeyword("tribute", Keyword.TRIBUTE, KeywordAbility::tribute),
        numericKeyword("mobilize", Keyword.MOBILIZE, KeywordAbility::mobilize),
        numericKeyword("firebending", Keyword.FIREBENDING, KeywordAbility::firebending),
        numericKeyword("hideaway", Keyword.HIDEAWAY, KeywordAbility::hideaway),
    )

    private val costKeywords: List<Phrase<KeywordAbility>> = listOf(
        costKeyword("cycling", { KeywordAbility.cycling(it.toString()) }) { ability ->
            (ability as? KeywordAbility.Cycling)
                ?.takeIf { it.searchFilter == null && it.displayPrefix == "Cycling" }
                ?.cost
        },
        costKeyword("flashback", { KeywordAbility.flashback(it.toString()) }) { ability ->
            (ability as? KeywordAbility.Flashback)?.takeIf { it.additionalCost == null }?.cost
        },
        costKeyword("madness", { KeywordAbility.madness(it.toString()) }) { (it as? KeywordAbility.Madness)?.cost },
        costKeyword("foretell", { KeywordAbility.foretell(it.toString()) }) { (it as? KeywordAbility.Foretell)?.cost },
        costKeyword("plot", { KeywordAbility.plot(it.toString()) }) { (it as? KeywordAbility.Plot)?.cost },
        costKeyword("disturb", { KeywordAbility.disturb(it.toString()) }) { (it as? KeywordAbility.Disturb)?.cost },
        costKeyword("evoke", { KeywordAbility.evoke(it.toString()) }) { (it as? KeywordAbility.Evoke)?.cost },
        costKeyword("emerge", { KeywordAbility.emerge(it.toString()) }) { (it as? KeywordAbility.Emerge)?.cost },
        costKeyword("miracle", { KeywordAbility.miracle(it.toString()) }) { (it as? KeywordAbility.Miracle)?.cost },
        costKeyword("dash", { KeywordAbility.dash(it.toString()) }) { (it as? KeywordAbility.Dash)?.cost },
        costKeyword("warp", { KeywordAbility.warp(it.toString()) }) { (it as? KeywordAbility.Warp)?.cost },
        costKeyword("cleave", { KeywordAbility.cleave(it.toString()) }) { (it as? KeywordAbility.Cleave)?.cost },
        costKeyword("harmonize", { KeywordAbility.harmonize(it.toString()) }) { (it as? KeywordAbility.Harmonize)?.cost },
        costKeyword("mayhem", { KeywordAbility.mayhem(it.toString()) }) { (it as? KeywordAbility.Mayhem)?.cost },
        costKeyword("ninjutsu", { KeywordAbility.ninjutsu(it.toString()) }) { (it as? KeywordAbility.Ninjutsu)?.cost },
        costKeyword("sneak", { KeywordAbility.sneak(it.toString()) }) { (it as? KeywordAbility.Sneak)?.cost },
        costKeyword("web-slinging", { KeywordAbility.webSlinging(it.toString()) }) {
            (it as? KeywordAbility.WebSlinging)?.cost
        },
        costKeyword("morph", { KeywordAbility.morph(it.toString()) }) { ability ->
            (ability as? KeywordAbility.Morph)?.takeIf { it.faceUpEffect == null }?.let { manaOnly(it.morphCost) }
        },
        costKeyword("disguise", { KeywordAbility.disguise(it.toString()) }) { ability ->
            (ability as? KeywordAbility.Disguise)?.takeIf { it.faceUpEffect == null }
                ?.let { manaOnly(it.disguiseCost) }
        },
        costKeyword("kicker", { KeywordAbility.kicker(it) }) { ability ->
            optionalAdditionalManaCost(ability, prefix = "Kicker", multi = false)
        },
        costKeyword("multikicker", { KeywordAbility.multikicker(it.toString()) }) { ability ->
            optionalAdditionalManaCost(ability, prefix = "Multikicker", multi = true)
        },
        costKeyword("offspring", { KeywordAbility.offspring(it) }) { ability ->
            optionalAdditionalManaCost(ability, prefix = "Offspring", multi = false)
        },
    )

    /**
     * Typecycling and basic landcycling — the same [KeywordAbility.Cycling] shape with a search
     * filter and a display prefix.
     *
     * The type is enumerated rather than parsed into a slot, and the reason is worth recording: a
     * `{type}cycling` slot needs the token to stop *before* the literal "cycling", which takes a
     * lookahead — and a leaf whose pattern only matches in context cannot verify its own printed
     * output the way [com.wingedsheep.assay.syntax.token] does. Eight constants keep that check
     * intact. A ninth cycling type is one line, and until it is written the card declines, which
     * is the correct behaviour rather than a silent approximation.
     */
    private val cyclingVariants: List<Phrase<KeywordAbility>> = buildList {
        add(
            costKeyword("basic landcycling", { KeywordAbility.basicLandcycling(it) }) { ability ->
                (ability as? KeywordAbility.Cycling)?.takeIf { it.displayPrefix == "Basic landcycling" }?.cost
            }
        )
        for (type in listOf("Plains", "Island", "Swamp", "Mountain", "Forest", "Wizard", "Sliver", "Halfling")) {
            add(
                costKeyword("${type.lowercase()}cycling", { KeywordAbility.typecycling(type, it) }) { ability ->
                    (ability as? KeywordAbility.Cycling)?.takeIf { it.displayPrefix == "${type}cycling" }?.cost
                }
            )
        }
    }

    private val casualty: Phrase<KeywordAbility> = phrase("casualty {n}", name = "casualty") {
        slot("n", Primitives.cardinal)
        build { KeywordAbility.casualty(it.int("n")) }
        match { (it as? KeywordAbility.Casualty)?.let { c -> bind("n" to c.threshold) } }
    }

    private val devour: Phrase<KeywordAbility> = phrase("devour {n}", name = "devour") {
        slot("n", Primitives.cardinal)
        build { KeywordAbility.devour(it.int("n")) }
        match {
            (it as? KeywordAbility.Devour)?.takeIf { d -> d.variant.isBlank() }
                ?.let { d -> bind("n" to d.multiplier) }
        }
    }

    /** Every keyword-ability rule, in one place. Order is irrelevant to parsing; all are tried. */
    val all: List<Phrase<KeywordAbility>> =
        simpleKeywords + dedicatedObjects + numericKeywords + costKeywords + cyclingVariants + listOf(
            protection,
            hexproofFrom,
            wardMana,
            wardLife,
            affinityForType,
            affinityForSubtype,
            suspend,
            impending,
            splice,
            casualty,
            devour,
        )

    // ---------------------------------------------------------------------------------------
    // Model-side helpers for the `match` halves
    // ---------------------------------------------------------------------------------------

    private fun wardManaCost(ability: KeywordAbility): ManaCost? {
        val cost = (ability as? KeywordAbility.Ward)?.cost as? com.wingedsheep.sdk.scripting.effects.WardCost.Mana
        if (cost == null || cost.waterbend) return null
        return runCatching { ManaCost.parse(cost.manaCost) }.getOrNull()
    }

    /** A [com.wingedsheep.sdk.scripting.costs.PayCost] that is nothing but mana, or null. */
    private fun manaOnly(cost: com.wingedsheep.sdk.scripting.costs.PayCost): ManaCost? {
        val atom = (cost as? com.wingedsheep.sdk.scripting.costs.PayCost.Atom)?.atom
        return (atom as? com.wingedsheep.sdk.scripting.costs.CostAtom.Mana)?.cost
    }

    private fun optionalAdditionalManaCost(
        ability: KeywordAbility,
        prefix: String,
        multi: Boolean,
    ): ManaCost? = (ability as? KeywordAbility.OptionalAdditionalCost)
        ?.takeIf { it.displayPrefix == prefix && it.multi == multi && it.additionalCost == null }
        ?.manaCost
}
