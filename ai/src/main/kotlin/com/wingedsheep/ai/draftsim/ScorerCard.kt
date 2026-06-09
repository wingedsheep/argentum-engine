package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.sdk.model.CardDefinition

/**
 * Adapter the Draftsim scorer reads cards through. The bundle's spec defines its own "Card" type;
 * per the porting contract we do **not** introduce it — instead this interface exposes exactly the
 * fields the scorer needs (`SPEC_scoring.md` §1) off whatever card class a caller already holds.
 *
 * Two adapters ship:
 *  - [CardDefinition] (deckbuild path) — has colors / color identity / cmc / rarity natively.
 *  - [CardSummary] (draft path) — carries only the printed fields; [colors] / [colorIdentity] are
 *    left empty so the scorer's `colors()` helper derives them from [manaCost], exactly as the
 *    bundle does when `card.colors` is absent.
 */
interface ScorerCard {
    /** Printed name; matched against ratings/removal/archetypes via [DraftsimData.nameKey]. */
    val name: String

    /** Brace cost string, e.g. `"{1}{U}{B}"`; may be `"a // b"` for split/DFC; `""` if none. */
    val manaCost: String

    /** Lowercased-tested type line (substring checks for "land"/"creature"/…). */
    val typeLine: String

    /** Mana value. */
    val cmc: Double

    /** `"mythic"|"rare"|"uncommon"|"common"`, or null when unknown. */
    val rarity: String?

    /** USD price (hate-draft signal); null when unavailable — that branch then stays inert. */
    val priceUsd: Double?

    /** Declared colors (WUBRG letters). Empty ⇒ scorer parses [manaCost]. */
    val colors: List<String>

    /** Declared color identity (WUBRG letters); fallback color source for lands/fixing. */
    val colorIdentity: List<String>
}

/** Adapt a resolved [CardDefinition] (deckbuild path — full metadata available). */
fun CardDefinition.toScorerCard(): ScorerCard = CardDefinitionScorerCard(this)

/** Adapt a [CardSummary] (draft path — colors derived from the cost string by the scorer). */
fun CardSummary.toScorerCard(): ScorerCard = CardSummaryScorerCard(this)

private class CardDefinitionScorerCard(private val def: CardDefinition) : ScorerCard {
    override val name: String = def.name
    override val manaCost: String = if (def.manaCost.symbols.isEmpty()) "" else def.manaCost.toString()
    override val typeLine: String = def.typeLine.toString()
    override val cmc: Double = def.cmc.toDouble()
    // The Draftsim ladder only recognizes mythic/rare/uncommon/common; any other enum name (special,
    // bonus, …) lowercases to a key the ladder lacks and falls to the common floor. Acceptable —
    // those rarities don't appear in normal limited packs — but rate via the name table, not this.
    override val rarity: String = def.metadata.rarity.name.lowercase()
    override val priceUsd: Double? = null
    override val colors: List<String> = def.colors.map { it.symbol.toString() }
    override val colorIdentity: List<String> = def.colorIdentity.map { it.symbol.toString() }
}

private class CardSummaryScorerCard(private val summary: CardSummary) : ScorerCard {
    override val name: String = summary.name
    override val manaCost: String = summary.manaCost ?: ""
    override val typeLine: String = summary.typeLine ?: ""
    override val cmc: Double = DraftsimMana.cmc(summary.manaCost ?: "")
    override val rarity: String? = summary.rarity?.lowercase()
    override val priceUsd: Double? = null
    override val colors: List<String> = emptyList()
    override val colorIdentity: List<String> = emptyList()
}
