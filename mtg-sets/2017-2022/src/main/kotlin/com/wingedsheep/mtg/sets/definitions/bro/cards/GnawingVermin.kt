package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Gnawing Vermin
 * {B}
 * Creature — Rat
 * 1/1
 * When this creature enters, target player mills two cards.
 * When this creature dies, target creature you don't control gets -1/-1 until end of turn.
 *
 * "You don't control" is `Not(ControlledByYou)`, not "an opponent controls" — the two separate
 * in multiplayer.
 */
val GnawingVermin = card("Gnawing Vermin") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, target player mills two cards.\n" +
        "When this creature dies, target creature you don't control gets -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ModifyStats(-1, -1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Raoul Vitale"
        flavorText = "\"Everyone warns about the sand and the food, but the rats are worse than both put together!\"\n—Sanwell, letter to his family"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fa2fa48-ddb7-45f0-b183-4186df98f7cc.jpg"
    }
}
