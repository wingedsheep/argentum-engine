package com.wingedsheep.ai.audit

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.Speed
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.CardDefinition

/**
 * How the engine AI **rates** a card — the companion to [AiReadingAudit]'s "what does it read". Two
 * questions, each answered by the code the AI really calls, never by a re-implementation:
 *
 *  - **Draft and deckbuilding** — [LimitedCardRater.explain] (17Lands win rate, else a Draftsim-scale
 *    table, else the effect heuristic, with the heuristic's terms), and per set the card is in, the
 *    number the draft pick scorer starts from: that set's table, or the [LimitedCardRater] fallback
 *    `withFallbackRatings` fills in for an unrated nonland card.
 *  - **In game** — what [BoardPresence] prices the card at as a permanent with nothing around it
 *    (printed stats and keywords, or the [CardIntentAnalyzer] prior ladder), and the [CardIntent]
 *    the AI's timing, targeting and hold policies consult.
 *
 * For developer views only (the Argentum Assay explorer, through `game-server`). It runs once per
 * card on request; nothing in play reads it.
 */
object AiCardRating {

    /** One additive part of a number: "Flying +0.8", with the detail a tooltip shows. */
    data class Part(val label: String, val value: Double, val detail: String? = null)

    /**
     * The draft pick scorer's starting number for the card in one set — before colour commitment,
     * archetype fit and removal bonus, which depend on the picks so far.
     */
    data class SetPick(
        val set: String,
        /** Null for a land the set's table doesn't rate: Draftsim's own land handling picks it. */
        val rating: Double?,
        /** True when the set's own table rates it; false when `withFallbackRatings` filled it in. */
        val fromTable: Boolean,
        /** On the set's curated removal list, which the scorer gives a removal bonus. */
        val removal: Boolean,
        /** Archetype roles from the set's archetype table: "WU · payoff". */
        val archetypes: List<String>,
    )

    data class Limited(
        val explanation: LimitedCardRater.Explanation,
        val sets: List<SetPick>,
    )

    enum class BoardKind { CREATURE, VARIABLE_CREATURE, PERMANENT, PLANESWALKER, LAND, SPELL }

    /**
     * What [BoardPresence] prices the card at on an empty battlefield. [parts] sum to [value];
     * [value] is null when the price only exists in play — a spell, or `*`/`*` stats.
     */
    data class Board(val kind: BoardKind, val value: Double?, val parts: List<Part>, val rule: String? = null)

    /** One recognised thing the card does, in words. */
    data class Tag(val key: String, val label: String)

    data class Intent(val tags: List<Tag>, val speed: String, val facts: List<String>)

    data class Rating(
        val limited: Limited,
        val board: Board,
        val intent: Intent,
        /** The hand-written `CardAdvisor` the live AI consults for this card, by class name. */
        val advisor: String?,
    )

    /** Rate [card], which is printed in [setCodes] (the sets that author it or reprint it). */
    fun of(card: CardDefinition, setCodes: List<String>): Rating {
        val intent = CardIntentAnalyzer.analyze(card)
        return Rating(
            limited = Limited(LimitedCardRater.explain(card), setCodes.distinct().map { setPick(card, it) }),
            board = board(card, intent),
            intent = intent(intent),
            advisor = liveAdvisors.getAdvisor(card.name)?.let { it::class.simpleName },
        )
    }

    private fun setPick(card: CardDefinition, set: String): SetPick {
        val tables = DraftsimData.tablesFor(listOf(set))
        val key = DraftsimData.nameKey(card.name)
        val own = tables.ratings[key]
        val rating = own ?: if (card.typeLine.isLand) null else LimitedCardRater.rate(card)
        val removal = card.name.lowercase() in tables.removal ||
            card.name.substringBefore("//").trim().lowercase() in tables.removal
        val archetypes = tables.archetypes[key]?.archetypes.orEmpty().map { "${it.archetype} · ${it.role}" }
        return SetPick(set, rating, own != null, removal, archetypes)
    }

    // ── In game ─────────────────────────────────────────────────────────────

    private fun board(card: CardDefinition, intent: CardIntent): Board {
        val type = card.typeLine
        if (type.isLand) {
            return Board(
                BoardKind.LAND, BoardPresence.LAND_UNTAPPED, listOf(Part("Untapped land", BoardPresence.LAND_UNTAPPED)),
                "Every land is worth the same on the battlefield; a tapped one a little less.",
            )
        }
        if (!type.isPermanent) {
            return Board(
                BoardKind.SPELL, null, emptyList(),
                "Not a permanent — the AI prices it by simulating what it does to the board when it resolves.",
            )
        }
        if (type.isCreature) {
            val power = card.creatureStats?.basePower
            val toughness = card.creatureStats?.baseToughness
            if (power == null || toughness == null) {
                return Board(
                    BoardKind.VARIABLE_CREATURE, null, emptyList(),
                    "Its power and toughness are only known in play, so the AI prices it from the live numbers.",
                )
            }
            val none = emptySet<String>()
            val parts = buildList {
                add(Part("Power", BoardPresence.creatureBodyValue(power, 0, none), "$power × 1.0"))
                add(Part("Toughness", BoardPresence.creatureBodyValue(0, toughness, none), "$toughness × 0.4"))
                val base = BoardPresence.creatureBodyValue(power, toughness, none)
                for (keyword in card.keywords.sortedBy { it.name }) {
                    val v = BoardPresence.creatureBodyValue(power, toughness, setOf(keyword.name)) - base
                    if (v != 0.0) add(Part(keyword.displayName, v))
                }
            }
            return Board(
                BoardKind.CREATURE, parts.sumOf { it.value }, parts,
                "Abilities add nothing here: the AI values what they do when they trigger or are activated.",
            )
        }
        val ladder = CardIntentAnalyzer.priorLadder(intent)
        val best = ladder.maxByOrNull { it.value }
        val floor = when {
            CardType.PLANESWALKER in type.cardTypes -> 4.0
            type.isAura -> 1.5
            else -> CardIntent.UNKNOWN.staticPriorValue
        }
        val prior = best?.value ?: CardIntent.UNKNOWN.staticPriorValue
        val parts = if (prior >= floor) {
            listOf(Part(best?.claim?.replaceFirstChar { it.uppercase() } ?: "Unreadable permanent", prior))
        } else {
            listOf(Part(if (type.isAura) "Attached Aura floor" else "Planeswalker floor", floor))
        }
        val rule = when {
            CardType.PLANESWALKER in type.cardTypes ->
                "In play it scales with loyalty: about ${BoardPresence.LOYALTY_VALUE} per counter."
            intent.anthemBonus > 0 ->
                "Plus ${"%.2f".format(0.25 * intent.anthemBonus)} for each creature it pumps."
            best == null -> "The AI recognises nothing this permanent does, so it gets the generic price."
            else -> null
        }
        return Board(
            if (CardType.PLANESWALKER in type.cardTypes) BoardKind.PLANESWALKER else BoardKind.PERMANENT,
            parts.sumOf { it.value }, parts, rule,
        )
    }

    private fun intent(intent: CardIntent): Intent {
        val facts = buildList {
            if (IntentTag.FIGHT in intent.tags) add("Its removal reach is the fighting creature's power")
            else if (intent.removalReach != null) add("Its damage answers toughness ${intent.removalReach} or less")
            intent.cardsDrawn?.let { add("Draws $it card${if (it == 1) "" else "s"} at a time") }
            if (intent.repeatable) add("Can pay off more than once")
            if (intent.anthemBonus > 0) add("Gives each of your creatures +${intent.anthemBonus} total P/T")
            if (intent.pumpToughness > 0) add("A trick that adds ${intent.pumpToughness} toughness until end of turn")
            intent.counterTax?.let { add("A soft counter: the caster can pay {$it}") }
            if (intent.entersTapped) add("Always enters tapped")
            if (intent.flashPermanent) add("A flash permanent the AI may hold for an ambush")
            if (intent.selectsIntoHand) add("Selects a card into hand")
        }
        return Intent(
            tags = intent.tags.sortedBy { it.ordinal }.map { Tag(it.name, TAG_LABEL[it] ?: it.name) },
            speed = SPEED_LABEL.getValue(intent.speed),
            facts = facts,
        )
    }

    private val TAG_LABEL = mapOf(
        IntentTag.REMOVAL to "Removal",
        IntentTag.EXILE_REMOVAL to "Exile removal",
        IntentTag.SWEEPER to "Sweeper",
        IntentTag.NEUTRALIZE to "Neutralizes a creature",
        IntentTag.DRAW to "Card draw",
        IntentTag.TUTOR to "Tutor",
        IntentTag.RAMP to "Ramp",
        IntentTag.ANTHEM to "Anthem",
        IntentTag.PUMP to "Pump",
        IntentTag.COMBAT_TRICK to "Combat trick",
        IntentTag.FIGHT to "Fight",
        IntentTag.COUNTERSPELL to "Counterspell",
        IntentTag.LIFEGAIN to "Life gain",
        IntentTag.DISCARD to "Discard",
        IntentTag.RECURSION to "Recursion",
        IntentTag.TOKEN_MAKER to "Makes tokens",
        IntentTag.PROTECTION to "Protection",
        IntentTag.TAPPER to "Tapper",
        IntentTag.EVASION_GRANT to "Grants evasion",
        IntentTag.SACRIFICE_OUTLET to "Sacrifice outlet",
    )

    private val SPEED_LABEL = mapOf(
        Speed.SORCERY to "Sorcery speed",
        Speed.INSTANT to "Instant speed",
        Speed.STATIC to "Static",
        Speed.ACTIVATED to "Activated",
    )

    /** The advisors the profile live players face registers — the ones that change real games. */
    private val liveAdvisors: CardAdvisorRegistry by lazy {
        CardAdvisorRegistry().also { registry -> AiProfile.LIVE.advisorModules.forEach { it.register(registry) } }
    }
}
