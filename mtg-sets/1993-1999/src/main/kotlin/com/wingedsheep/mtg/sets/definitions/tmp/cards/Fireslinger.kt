package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fireslinger
 * {1}{R}
 * Creature — Human Wizard
 * 1/1
 * {T}: This creature deals 1 damage to any target and 1 damage to you.
 */
val Fireslinger = card("Fireslinger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    oracleText = "{T}: This creature deals 1 damage to any target and 1 damage to you."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t) then
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Jeff Reitz"
        flavorText = "\"Remember the moral of the fireslinger fable: with power comes isolation.\"\n—Karn, silver golem"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de253d94-9968-47da-bb7a-9c8ebf50f4e0.jpg?1783946631"
    }
}
