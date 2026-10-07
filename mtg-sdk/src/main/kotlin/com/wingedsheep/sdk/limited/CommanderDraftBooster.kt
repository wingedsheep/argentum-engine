package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Commander Legends 2020 -shaped booster: 20 cards split into 13 commons, 3 uncommons,
 * 2 dedicated legendary slots, 1 non-legendary rare-or-mythic, and 1 bonus card of any rarity.
 *
 * The legendary slots hold a legendary creature or legendary planeswalker of any rarity. They
 * do not consume the non-legendary rare-or-mythic slot — both ship together, so a pack always
 * carries a non-leg R/M *and* two legendaries.
 *
 * The bonus slot replaces Commander Legends' traditional foil slot (the engine does not model
 * foils). Distribution is weighted to roughly mirror the foil-slot rarity mix: mostly common,
 * sometimes uncommon, occasionally rare or mythic.
 *
 * Sparse-legendary fallback: if the pool has fewer legendaries than required slots (or none at
 * all), the missing slots are filled with [piperFallback] when supplied, and otherwise demoted to
 * additional non-legendary uncommons. Callers that want to flag a Piper-substituted pool for
 * downstream commander selection should compare the resulting pack against the input pool.
 *
 * @param piperFallback A colourless legendary creature card (typically The Prismatic Piper) used
 *                      to back-fill legendary slots when the set has no legendaries. The fallback
 *                      is never drawn from the booster pool itself — supply it explicitly.
 */
data class CommanderDraftBooster(
    val commons: Int = 13,
    val uncommons: Int = 3,
    val legendaries: Int = 2,
    val nonLegendaryRareOrMythic: Int = 1,
    val bonus: Int = 1,
    val mythicChance: Double = 0.125,
    val piperFallback: CardDefinition? = null,
) : BoosterStrategy {

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val (legendaryPool, nonLegendaryPool) = pool.partition { it.isCommanderSlotEligible() }

        val nonLegPicker = RarityPicker(nonLegendaryPool, random)
        val legPicker = RarityPicker(legendaryPool, random)
        val booster = mutableListOf<CardDefinition>()

        repeat(commons) { nonLegPicker.pick(Rarity.COMMON)?.let(booster::add) }
        repeat(uncommons) { nonLegPicker.pick(Rarity.UNCOMMON)?.let(booster::add) }

        repeat(legendaries) {
            val pick = pickLegendary(legPicker, random)
                ?: piperFallback
                ?: nonLegPicker.pick(Rarity.UNCOMMON)
                ?: nonLegPicker.pick(Rarity.COMMON)
            pick?.let(booster::add)
        }

        repeat(nonLegendaryRareOrMythic) {
            val rolledMythic = nonLegPicker.hasAny(Rarity.MYTHIC) && random.nextDouble() < mythicChance
            val firstChoice = if (rolledMythic) Rarity.MYTHIC else Rarity.RARE
            val pick = nonLegPicker.pick(firstChoice)
                ?: nonLegPicker.pick(Rarity.RARE)
                ?: nonLegPicker.pick(Rarity.MYTHIC)
                ?: nonLegPicker.pick(Rarity.UNCOMMON)
                ?: nonLegPicker.pick(Rarity.COMMON)
            pick?.let(booster::add)
        }

        repeat(bonus) {
            val rarity = rollBonusRarity(random)
            val pick = nonLegPicker.pick(rarity)
                ?: nonLegPicker.pick(Rarity.RARE)
                ?: nonLegPicker.pick(Rarity.UNCOMMON)
                ?: nonLegPicker.pick(Rarity.COMMON)
            pick?.let(booster::add)
        }

        return booster
    }

    private fun pickLegendary(picker: RarityPicker, random: Random): CardDefinition? {
        val rolledMythic = picker.hasAny(Rarity.MYTHIC) && random.nextDouble() < mythicChance
        val firstChoice = if (rolledMythic) Rarity.MYTHIC else Rarity.RARE
        return picker.pick(firstChoice)
            ?: picker.pick(Rarity.RARE)
            ?: picker.pick(Rarity.MYTHIC)
            ?: picker.pick(Rarity.UNCOMMON)
            ?: picker.pick(Rarity.COMMON)
    }

    /** Approximates Commander Legends' foil-slot rarity mix: 50% C, 33% U, 14% R, 3% M. */
    private fun rollBonusRarity(random: Random): Rarity {
        val roll = random.nextDouble()
        return when {
            roll < 0.50 -> Rarity.COMMON
            roll < 0.83 -> Rarity.UNCOMMON
            roll < 0.97 -> Rarity.RARE
            else -> Rarity.MYTHIC
        }
    }

    private fun CardDefinition.isCommanderSlotEligible(): Boolean =
        typeLine.isLegendary && (typeLine.isCreature || CardType.PLANESWALKER in typeLine.cardTypes)
}
