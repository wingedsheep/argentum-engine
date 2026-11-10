package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slaughter Drone
 * {1}{B}
 * Creature — Eldrazi Drone
 * 2/2
 * Devoid (This card has no color.)
 * {C}: This creature gains deathtouch until end of turn. (Any amount of damage it deals to a
 * creature is enough to destroy it. {C} represents colorless mana.)
 */
val SlaughterDrone = card("Slaughter Drone") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "{C}: This creature gains deathtouch until end of turn. (Any amount of damage it deals to a creature is enough to destroy it. {C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{C}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Dan Murayama Scott"
        flavorText = "It kills what is, making way for what should not be."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/275b1185-49aa-45ae-8e2d-12aa469a7d77.jpg"
    }
}
