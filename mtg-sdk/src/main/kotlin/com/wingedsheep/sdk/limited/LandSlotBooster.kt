package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/**
 * Reserves named nonbasic lands for one dedicated land slot, then delegates the other slots to [base].
 * The land slot opens one of the available nonbasics with [nonbasicChance] — the set's published
 * odds, e.g. "a common dual land (50%) or a basic land (50%)" — and otherwise one of the
 * [basicLands] arts, which the generator removes from its ordinary pool. Basic lands still in [pool] —
 * basic *snow* lands, which are pack cards rather than freely added basics — join the basic group, so a
 * set whose land slot opens reprinted snow basics (Kaldheim) can pass an empty [basicLands]. Either
 * group falls back to the other when it is empty. Host-banned nonbasics are absent from [pool] and cannot enter the land
 * slot; basic lands remain available independently of the host ban list, as in limited deck building.
 */
data class LandSlotBooster(
    val base: BoosterStrategy,
    val nonbasicLandNames: Set<String>,
    val basicLands: List<CardDefinition>,
    val nonbasicChance: Double,
) : BoosterStrategy {
    init {
        require(basicLands.all { it.typeLine.isBasicLand }) { "Land-slot basics must be basic lands" }
        require(nonbasicChance in 0.0..1.0) { "nonbasicChance must be a probability" }
    }

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val nonbasics = pool.filter { it.name in nonbasicLandNames }.distinctBy { it.name }
        val regular = pool.filter { it.name !in nonbasicLandNames && !it.typeLine.isBasicLand }
        val booster = base.generate(regular, random)
        val used = booster.mapTo(hashSetOf()) { it.name }
        val availableNonbasics = nonbasics.filter { it.name !in used }
        val availableBasics = (basicLands + pool.filter { it.typeLine.isBasicLand }).filter { it.name !in used }
        val (first, second) = if (random.nextDouble() < nonbasicChance) {
            availableNonbasics to availableBasics
        } else {
            availableBasics to availableNonbasics
        }
        val land = (first.ifEmpty { second }).randomOrNull(random)
        return if (land == null) booster else booster + land
    }
}
