package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Dominaria / Kamigawa-style booster: every pack contains a legendary creature.
 *
 * The legendary occupies the slot matching its printed rarity:
 *   - Uncommon legendary → replaces one uncommon slot
 *   - Rare/mythic legendary → replaces the rare slot
 *
 * Falls back to [base] generation when the pool has no legendary creatures.
 */
data class GuaranteedLegendaryBooster(
    val base: StandardBooster = StandardBooster(),
) : BoosterStrategy {

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val legendaries = pool.filter { it.typeLine.isLegendary && it.typeLine.isCreature }
        if (legendaries.isEmpty()) return base.generate(pool, random)

        val legendary = legendaries.random(random)
        val poolWithoutLegendary = pool.filter { it.name != legendary.name }
        val picker = RarityPicker(poolWithoutLegendary, random)
        val booster = mutableListOf<CardDefinition>()

        repeat(base.commons) { picker.pick(Rarity.COMMON)?.let(booster::add) }

        val legendaryIsUncommon = legendary.metadata.rarity == Rarity.UNCOMMON
        val uncommonSlots = if (legendaryIsUncommon) (base.uncommons - 1).coerceAtLeast(0) else base.uncommons
        repeat(uncommonSlots) { picker.pick(Rarity.UNCOMMON)?.let(booster::add) }

        val legendaryIsRareOrMythic = legendary.metadata.rarity == Rarity.RARE ||
            legendary.metadata.rarity == Rarity.MYTHIC
        if (!legendaryIsRareOrMythic) {
            val rolledMythic = picker.hasAny(Rarity.MYTHIC) && random.nextDouble() < base.mythicChance
            val rareSlot = (if (rolledMythic) picker.pick(Rarity.MYTHIC) else picker.pick(Rarity.RARE))
                ?: picker.pick(Rarity.RARE)
                ?: picker.pick(Rarity.UNCOMMON)
                ?: picker.pick(Rarity.COMMON)
            rareSlot?.let(booster::add)
        }

        booster.add(legendary)
        return booster
    }
}
