package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Witty Roastmaster
 * {2}{R}
 * Creature — Devil Citizen
 * 3 / 2
 * Alliance — Whenever another creature you control enters, this creature deals 1 damage to each
 * opponent.
 *
 * "Alliance" is a pure ability word: the trigger is the plain OTHER-bound enters trigger over
 * creatures you control.
 */
val WittyRoastmaster = card("Witty Roastmaster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil Citizen"
    oracleText = "Alliance — Whenever another creature you control enters, this creature deals 1 damage to each opponent."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Alliance — Whenever another creature you control enters, this creature deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Joe Slucher"
        flavorText = "If you're lucky, you'll walk out with just your ego singed."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71d13f19-482b-4a2e-9692-b7d7caf2f9f5.jpg"
    }
}
