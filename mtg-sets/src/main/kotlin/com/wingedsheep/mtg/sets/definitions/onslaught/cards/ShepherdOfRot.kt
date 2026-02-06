package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.Player

/**
 * Shepherd of Rot
 * {1}{B}
 * Creature — Zombie Cleric
 * 1/1
 * {T}: Each player loses 1 life for each Zombie on the battlefield.
 */
val ShepherdOfRot = card("Shepherd of Rot") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie Cleric"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        effect = LoseLifeEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Zombie")),
            EffectTarget.PlayerRef(Player.Each)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Carl Critchlow"
        flavorText = "The living want to join the dead, and the dead want the living to join them."
        imageUri = "https://cards.scryfall.io/large/front/9/5/952c021f-74c9-455f-9cd9-f0d354e8bea8.jpg?1562930144"
    }
}
