package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rage Thrower
 * {5}{R}
 * Creature — Human Shaman
 * 4/2
 * Whenever another creature dies, this creature deals 2 damage to target player or planeswalker.
 */
val RageThrower = card("Rage Thrower") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever another creature dies, this creature deals 2 damage to target player or planeswalker."
    power = 4
    toughness = 2
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(2, victim)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Peter Mohrbacher"
        flavorText = "\"Some lament these haunted times, but I'm a geistflame-tank-half-full kind of person.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f16db004-3e0c-491b-b8b6-0ae046d11761.jpg"
    }
}
