package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Salvager of Ruin
 * {3}
 * Artifact Creature — Construct
 * 2/1
 * Sacrifice this creature: Choose target permanent card in your graveyard that was put
 * there from the battlefield this turn. Return it to your hand.
 */
val SalvagerOfRuin = card("Salvager of Ruin") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 1
    oracleText = "Sacrifice this creature: Choose target permanent card in your graveyard " +
        "that was put there from the battlefield this turn. Return it to your hand."

    activatedAbility {
        cost = Costs.SacrificeSelf
        val returnTarget = target(
            TargetFilter(
                GameObjectFilter.Permanent
                    .ownedByYou()
                    .putIntoGraveyardFromBattlefieldThisTurn(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Move(returnTarget, Zone.HAND, fromZone = Zone.GRAVEYARD)
        description = "Sacrifice this creature: Return target permanent card in your graveyard " +
            "that was put there from the battlefield this turn to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "James Paick"
        flavorText = "\"The next model should be able to achieve a rescue without self-destruction.\"\n—Sargis Haz, artificer"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd2ebbe-71c6-405b-bad0-5680f0ff575c.jpg?1783932941"
    }
}
