package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Which cards a [DedicatedSlot] prints on its own sheet. Pure data over [CardDefinition] fields; like
 * [BoosterStrategy], collation vocabulary rather than card vocabulary, so it stays outside the sealed SDK surface.
 */
interface SheetSelector {
    fun matches(card: CardDefinition): Boolean

    /** Double-faced cards (transforming, modal, battles — anything with a back face). */
    data object DoubleFaced : SheetSelector {
        override fun matches(card: CardDefinition) = card.isDoubleFaced
    }

    /** Battles. */
    data object Battle : SheetSelector {
        override fun matches(card: CardDefinition) = card.typeLine.isBattle
    }

    /** Legendary creatures. */
    data object LegendaryCreature : SheetSelector {
        override fun matches(card: CardDefinition) = card.typeLine.isLegendary && card.typeLine.isCreature
    }

    /** Planeswalkers — War of the Spark's one-planeswalker-per-pack slot. */
    data object Planeswalker : SheetSelector {
        override fun matches(card: CardDefinition) = CardType.PLANESWALKER in card.typeLine.cardTypes
    }

    /** Cards with [subtype] — Strixhaven's Lesson slot. */
    data class HasSubtype(val subtype: Subtype) : SheetSelector {
        override fun matches(card: CardDefinition) = card.typeLine.hasSubtype(subtype)
    }

    /** Cards named on the set's own collation list (reprint sheets, a named cycle). */
    data class Named(val names: Set<String>) : SheetSelector {
        override fun matches(card: CardDefinition) = card.name in names
    }

    /** Cards [matching] that are not [except]. */
    data class Except(val matching: SheetSelector, val except: SheetSelector) : SheetSelector {
        override fun matches(card: CardDefinition) = matching.matches(card) && !except.matches(card)
    }
}

/**
 * One booster slot fed by its own print sheet: the cards [sheet] selects, at the rarities this
 * slot can open.
 *
 * @param rarityWeights relative odds of each rarity when the slot doesn't hold the pack's rare.
 * @param rareChance chance that this slot holds the pack's one rare or mythic instead — the
 *   ordinary rare slot then opens an uncommon (see [RarityCountBooster.withRareSlotAsUncommon]).
 *   Zero for a slot whose rares, if any, are extras listed in [rarityWeights].
 * @param sharedRarities rarities whose sheet cards also stay on the ordinary sheets. By default a
 *   card on this sheet appears nowhere else in the pack.
 */
data class DedicatedSlot(
    val sheet: SheetSelector,
    val rarityWeights: Map<Rarity, Double>,
    val rareChance: Double = 0.0,
    val sharedRarities: Set<Rarity> = emptySet(),
    val mythicChance: Double = 0.125,
) {
    init {
        requireRarityWeights(rarityWeights, "rarityWeights")
        require(rareChance in 0.0..1.0) { "rareChance must be a probability" }
    }

    /** Rarities this slot can open; the sheet is the selected cards at these rarities. */
    private val rarities: Set<Rarity> =
        rarityWeights.keys + if (rareChance > 0.0) setOf(Rarity.RARE, Rarity.MYTHIC) else emptySet()

    fun onSheet(card: CardDefinition): Boolean = card.metadata.rarity in rarities && sheet.matches(card)

    internal fun onlyOnSheet(card: CardDefinition): Boolean = onSheet(card) && card.metadata.rarity !in sharedRarities
}

/**
 * A [base] booster plus [slots] that each draw from a dedicated print sheet — the double-faced
 * slots of Innistrad: Midnight Hunt, Crimson Vow, Kamigawa: Neon Dynasty and The Lost Caverns of
 * Ixalan; March of the Machine's double-faced and battle slots; the legendary slot of Dominaria
 * and Dominaria United; War of the Spark's planeswalker slot; Modern Horizons 3's new-to-Modern reprint slot.
 *
 * Cards on a dedicated sheet leave the base's pool (unless their rarity is shared), so the base
 * opens only ordinary cards. At most one slot holds the pack's rare: the slots' [DedicatedSlot.rareChance]s
 * are rolled together, and the winner opens a rare or mythic from its sheet while the base trades
 * its rare slot for an uncommon. A slot whose sheet has run dry (host bans, an unimplemented card)
 * opens the rolled rarity from the ordinary sheets instead, so the pack keeps its shape.
 *
 * [LandSlotBooster] reserves named lands for the land slot and [PlayBooster] varies wildcard odds;
 * neither moves the rare, which is the axis this type adds.
 */
data class DedicatedSlotBooster(
    val base: RarityCountBooster,
    val slots: List<DedicatedSlot>,
) : BoosterStrategy {
    init {
        require(slots.sumOf { it.rareChance } <= 1.0) { "Dedicated slots' rare chances must sum to at most 1" }
        require(slots.all { it.rareChance == 0.0 } || base.rareSlots > 0) { "A floating rare needs a base rare slot" }
    }

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val ordinary = pool.filter { card -> slots.none { it.onlyOnSheet(card) } }
        val sheets = slots.map { slot -> pool.filter(slot::onSheet) }

        val rareHolder = rollRareHolder(sheets, random)
        val baseCards = (if (rareHolder == null) base else base.withRareSlotAsUncommon()).generate(ordinary, random)

        val used = baseCards.mapTo(hashSetOf()) { it.name }
        val ordinaryPicker = RarityPicker(ordinary, random, used)
        val booster = baseCards.toMutableList()
        slots.forEachIndexed { index, slot ->
            val sheetPicker = RarityPicker(sheets[index], random, used)
            val card = if (index == rareHolder) {
                sheetPicker.pickRareOrMythic(slot.mythicChance)
            } else {
                val rolled = rollRarity(slot.rarityWeights, random)
                sheetPicker.pick(rolled)
                    ?: slot.rarityWeights.keys.firstNotNullOfOrNull { sheetPicker.pick(it) }
                    ?: ordinaryPicker.pick(rolled)
                    ?: ordinaryPicker.pick(Rarity.COMMON)
            }
            card?.let(booster::add)
        }
        return booster
    }

    /** The index of the slot holding the pack's rare, or null for the base's own rare slot. */
    private fun rollRareHolder(sheets: List<List<CardDefinition>>, random: Random): Int? {
        if (slots.all { it.rareChance == 0.0 }) return null
        var roll = random.nextDouble()
        slots.forEachIndexed { index, slot ->
            roll -= slot.rareChance
            if (roll < 0) {
                val hasRare = sheets[index].any { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC }
                return if (hasRare) index else null
            }
        }
        return null
    }
}
