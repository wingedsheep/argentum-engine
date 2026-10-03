package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stalking Drone
 * {1}{G}
 * Creature — Eldrazi Drone
 * 2/2
 * Devoid (This card has no color.)
 * {C}: This creature gets +1/+2 until end of turn. Activate only once each turn. ({C} represents colorless mana.)
 */
val StalkingDrone = card("Stalking Drone") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "{C}: This creature gets +1/+2 until end of turn. Activate only once each turn. " +
        "({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{C}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.ModifyStats(1, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Slawomir Maniak"
        flavorText = "As the Eldrazi adapted to the jungles of Zendikar, some took on the tactics of more familiar predators."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12bc5b4c-a809-43f0-8848-38812ce865c2.jpg"
    }
}
