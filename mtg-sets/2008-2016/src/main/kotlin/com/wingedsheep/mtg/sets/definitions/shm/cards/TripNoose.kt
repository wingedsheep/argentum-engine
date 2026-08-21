package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trip Noose
 * {2}
 * Artifact
 *
 * {2}, {T}: Tap target creature.
 *
 * - Ostiary Thrull's shape on an artifact body: a mana + `{T}` composite cost and a plain
 *   [Effects.Tap] on the single target. The target may already be tapped — the printed line has no
 *   "untapped" restriction, so the ability just does nothing in that case.
 */
val TripNoose = card("Trip Noose") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        target = Targets.Creature
        effect = Effects.Tap(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "266"
        artist = "Randy Gallegos"
        flavorText = "A taut slipknot trigger is the only thing standing between you and standing."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b2b3dd8-cedb-4577-8897-967952dd3c13.jpg?1783942709"
    }
}
