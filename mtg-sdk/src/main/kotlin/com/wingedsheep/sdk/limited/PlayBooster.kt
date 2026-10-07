package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Play Booster (Murders at Karlov Manor, February 2024, onward): the 14-card pack
 * that replaced both Draft and Set Boosters. Paper composition:
 *
 *  - Slots 1–7: 7 commons (slot 7 carries a 1.5% Special Guest chance — not modeled)
 *  - Slots 8–10: 3 uncommons
 *  - Slot 11: non-foil wildcard of any rarity
 *  - Slot 12: rare or mythic rare (12.5% mythic)
 *  - Slot 13: basic land
 *  - Slot 14: traditional-foil wildcard of any rarity
 *
 * Engine adaptation: foils, The List, and Special Guests are not modeled, so both
 * wildcard slots are plain weighted-rarity slots; the basic-land slot is omitted
 * because the generator strips basic lands from the pool and supplies them
 * separately at deck building. Net result: 13 playable non-land cards per pack.
 *
 * Wildcard rarity weights approximate WotC's published pack-level odds (~37% of
 * packs carry a second rare/mythic and ~4% a third → ~20% rare-or-mythic per
 * wildcard, split 7:1 rare:mythic).
 *
 * No card name is duplicated within a single pack.
 */
data class PlayBooster(
    val commons: Int = 7,
    val uncommons: Int = 3,
    val wildcards: Int = 2,
    val raresOrMythics: Int = 1,
    val mythicChance: Double = 0.125,
) : BoosterStrategy {

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val picker = RarityPicker(pool, random)
        val booster = mutableListOf<CardDefinition>()

        repeat(commons) { picker.pick(Rarity.COMMON)?.let(booster::add) }
        repeat(uncommons) { picker.pick(Rarity.UNCOMMON)?.let(booster::add) }
        repeat(raresOrMythics) {
            val rolledMythic = picker.hasAny(Rarity.MYTHIC) && random.nextDouble() < mythicChance
            val firstChoice = if (rolledMythic) Rarity.MYTHIC else Rarity.RARE
            val card = picker.pick(firstChoice)
                ?: picker.pick(Rarity.RARE)
                ?: picker.pick(Rarity.UNCOMMON)
                ?: picker.pick(Rarity.COMMON)
                ?: throw IllegalStateException("No cards available for booster generation")
            booster.add(card)
        }
        repeat(wildcards) {
            val pick = picker.pick(rollWildcardRarity(random))
                ?: picker.pick(Rarity.COMMON)
                ?: picker.pick(Rarity.UNCOMMON)
                ?: picker.pick(Rarity.RARE)
                ?: picker.pick(Rarity.MYTHIC)
            pick?.let(booster::add)
        }
        return booster
    }

    /** Wildcard slot rarity mix: 50% C, 30% U, 17.5% R, 2.5% M. */
    private fun rollWildcardRarity(random: Random): Rarity {
        val roll = random.nextDouble()
        return when {
            roll < 0.50 -> Rarity.COMMON
            roll < 0.80 -> Rarity.UNCOMMON
            roll < 0.975 -> Rarity.RARE
            else -> Rarity.MYTHIC
        }
    }
}
