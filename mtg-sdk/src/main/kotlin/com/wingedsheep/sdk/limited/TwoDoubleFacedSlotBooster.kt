package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Booster with two double-faced slots (Innistrad: Midnight Hunt draft boosters): one
 * double-faced common, and one double-faced uncommon, rare or mythic paired with one single-faced
 * uncommon-or-rare slot, beside [commons] single-faced commons and `uncommons - 1` single-faced
 * uncommons.
 *
 * The pair always holds exactly one rare or mythic: with [doubleFacedRareChance] the double-faced
 * card is the rare/mythic and the single-faced card an uncommon, otherwise the other way round — so
 * every pack has [uncommons] uncommons and one rare/mythic, and double-faced cards never appear
 * outside their two slots. An empty tier (host bans, a thin pool) falls back to the single-faced
 * sheet of the same rarity; with no double-faced rares or mythics the pair always rolls single-faced
 * rare.
 */
data class TwoDoubleFacedSlotBooster(
    val commons: Int = 9,
    val uncommons: Int = 3,
    val doubleFacedRareChance: Double = 1.0 / 6,
    val mythicChance: Double = 0.125,
) : BoosterStrategy {
    init {
        require(uncommons >= 1) { "uncommons must include the paired uncommon-or-rare slot" }
        require(doubleFacedRareChance in 0.0..1.0) { "doubleFacedRareChance must be a probability" }
    }

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val (doubleFaced, singleFaced) = pool.partition { it.isDoubleFaced }
        val dfcPicker = RarityPicker(doubleFaced, random)
        val picker = RarityPicker(singleFaced, random)

        val booster = mutableListOf<CardDefinition>()
        (dfcPicker.pick(Rarity.COMMON) ?: picker.pick(Rarity.COMMON))?.let(booster::add)
        repeat(commons) { picker.pick(Rarity.COMMON)?.let(booster::add) }
        repeat(uncommons - 1) { picker.pick(Rarity.UNCOMMON)?.let(booster::add) }

        val dfcRareAvailable = dfcPicker.hasAny(Rarity.RARE) || dfcPicker.hasAny(Rarity.MYTHIC)
        if (dfcRareAvailable && random.nextDouble() < doubleFacedRareChance) {
            booster.add(pickRareOrMythic(dfcPicker, random)!!)
            (picker.pick(Rarity.UNCOMMON) ?: picker.pick(Rarity.COMMON))?.let(booster::add)
        } else {
            (dfcPicker.pick(Rarity.UNCOMMON) ?: picker.pick(Rarity.UNCOMMON))?.let(booster::add)
            val rare = pickRareOrMythic(picker, random)
                ?: picker.pick(Rarity.UNCOMMON)
                ?: picker.pick(Rarity.COMMON)
                ?: throw IllegalStateException("No cards available for booster generation")
            booster.add(rare)
        }
        return booster
    }

    private fun pickRareOrMythic(picker: RarityPicker, random: Random): CardDefinition? {
        val rolledMythic = picker.hasAny(Rarity.MYTHIC) && random.nextDouble() < mythicChance
        return (if (rolledMythic) picker.pick(Rarity.MYTHIC) else picker.pick(Rarity.RARE))
            ?: picker.pick(Rarity.RARE)
            ?: picker.pick(Rarity.MYTHIC)
    }
}
