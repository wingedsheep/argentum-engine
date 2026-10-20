package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Booster with a dedicated double-faced slot (Kamigawa: Neon Dynasty): one double-faced common or
 * uncommon per pack, beside [commons] commons, [uncommons] uncommons and one rare or mythic.
 *
 * Double-faced commons and uncommons live only on their own sheet, so the ordinary common and
 * uncommon slots never hold one; double-faced rares and mythics stay in the rare slot. The
 * double-faced slot is an uncommon with [doubleFacedUncommonChance], otherwise a common. When the
 * rolled tier is empty (host bans, a thin pool) the other tier is used; with no double-faced
 * commons or uncommons at all the slot becomes one more ordinary common.
 */
data class DoubleFacedSlotBooster(
    val commons: Int = 9,
    val uncommons: Int = 3,
    val doubleFacedUncommonChance: Double = 1.0 / 3,
    val mythicChance: Double = 0.125,
) : BoosterStrategy {
    init {
        require(doubleFacedUncommonChance in 0.0..1.0) { "doubleFacedUncommonChance must be a probability" }
    }

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val onDoubleFacedSheet = { card: CardDefinition ->
            card.isDoubleFaced && (card.metadata.rarity == Rarity.COMMON || card.metadata.rarity == Rarity.UNCOMMON)
        }
        val (doubleFaced, ordinary) = pool.partition(onDoubleFacedSheet)

        val doubleFacedPicker = RarityPicker(doubleFaced, random)
        val wantsUncommon = random.nextDouble() < doubleFacedUncommonChance
        val doubleFacedCard = if (wantsUncommon) {
            doubleFacedPicker.pick(Rarity.UNCOMMON) ?: doubleFacedPicker.pick(Rarity.COMMON)
        } else {
            doubleFacedPicker.pick(Rarity.COMMON) ?: doubleFacedPicker.pick(Rarity.UNCOMMON)
        }

        val picker = RarityPicker(ordinary, random)
        val booster = mutableListOf<CardDefinition>()
        repeat(if (doubleFacedCard == null) commons + 1 else commons) {
            picker.pick(Rarity.COMMON)?.let(booster::add)
        }
        repeat(uncommons) { picker.pick(Rarity.UNCOMMON)?.let(booster::add) }
        doubleFacedCard?.let(booster::add)
        val rare = pickRareOrMythic(picker, random)
            ?: throw IllegalStateException("No cards available for booster generation")
        booster.add(rare)
        return booster
    }

    private fun pickRareOrMythic(picker: RarityPicker, random: Random): CardDefinition? {
        val rolledMythic = picker.hasAny(Rarity.MYTHIC) && random.nextDouble() < mythicChance
        return (if (rolledMythic) picker.pick(Rarity.MYTHIC) else picker.pick(Rarity.RARE))
            ?: picker.pick(Rarity.RARE)
            ?: picker.pick(Rarity.MYTHIC)
            ?: picker.pick(Rarity.UNCOMMON)
            ?: picker.pick(Rarity.COMMON)
    }
}
