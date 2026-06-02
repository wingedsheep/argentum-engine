package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Radiating Lightning
 * {3}{R}
 * Instant
 * Radiating Lightning deals 3 damage to target player and 1 damage to each creature
 * that player controls.
 */
val RadiatingLightning = card("Radiating Lightning") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Radiating Lightning deals 3 damage to target player and 1 damage to each creature that player controls."

    spell {
        val player = target("target player", Targets.Player)
        effect = Effects.DealDamage(3, player)
            .then(GroupPatterns.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.targetPlayerControls())))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Suzanne Helmigh"
        flavorText = "\"As the Cabal legions pushed into Shiv, they learned not to stand so close together.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94454128-92f1-475d-abc4-c235f501eeb6.jpg?1562739709"
    }
}
