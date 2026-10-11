package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bitter Work
 * {1}{R}{G}
 * Enchantment
 *
 * Whenever you attack a player with one or more creatures with power 4 or greater, draw a card.
 * Exhaust — {4}: Earthbend 4. Activate only during your turn. (Activate each exhaust ability only once.)
 *
 * The attack trigger fires once per player you attack with a creature of power 4+ (`attacksAPlayer`). The
 * exhaust ability composes `isExhaust = true` (→ once per object) with an explicit
 * `ActivationRestriction.OnlyDuringYourTurn` for the printed "Activate only during your turn", over
 * an [Effects.Earthbend] on a target land you control.
 */
val BitterWork = card("Bitter Work") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "Whenever you attack a player with one or more creatures with power 4 or greater, draw a card.\n" +
        "Exhaust — {4}: Earthbend 4. Activate only during your turn. (Target land you control becomes a 0/0 creature with haste that's still a land. Put four +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped. Activate each exhaust ability only once.)"

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer(GameObjectFilter.Creature.powerAtLeast(4))
        effect = Effects.DrawCards(1)
        description = "Whenever you attack a player with one or more creatures with power 4 or greater, draw a card."
    }

    activatedAbility {
        isExhaust = true
        cost = Costs.Mana("{4}")
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(4, land)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Bun Toujo"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e8de6a9-9859-4ed2-8ece-ef80a7209be9.jpg?1764121491"
    }
}
