package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pinnacle of Rage — Born of the Gods #105 (canonical printing)
 * {4}{R}{R}
 * Sorcery
 * Pinnacle of Rage deals 3 damage to each of two targets.
 *
 * Furious Reprisal's shape: exactly two distinct targets, each dealt 3.
 */
val PinnacleOfRage = card("Pinnacle of Rage") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Pinnacle of Rage deals 3 damage to each of two targets."

    spell {
        target = AnyTarget(count = 2)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(3, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Noah Bradley"
        flavorText = "Mount Sulano erupted not in magma but in anger."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b26ff2fa-82de-4831-9341-a446abf9ed03.jpg?1783939543"
        ruling("2014-02-01", "You must choose two legal targets to cast Pinnacle of Rage.")
    }
}
