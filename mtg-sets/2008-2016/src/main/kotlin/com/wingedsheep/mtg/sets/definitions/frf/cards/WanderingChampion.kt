package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Wandering Champion
 * {1}{W}
 * Creature — Human Monk
 * 3/1
 * Whenever this creature deals combat damage to a player, if you control a blue or red permanent,
 * you may discard a card. If you do, draw a card.
 *
 * The colour check is an intervening "if" (CR 603.4): it gates the trigger and is checked again on
 * resolution, reading projected colours.
 */
val WanderingChampion = card("Wandering Champion") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, if you control a blue or red " +
        "permanent, you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.BLUE, Color.RED))
        effect = Effects.May(
            effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Willian Murai"
        flavorText = "\"Learn from your enemies, but do not tolerate them.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59086792-c048-455f-ac0d-1a4a994bd621.jpg?1783938709"
        ruling(
            "2018-12-07",
            "Wandering Champion's ability doesn't trigger if you don't control a blue or red permanent " +
                "immediately after it deals combat damage to a player."
        )
        ruling(
            "2018-12-07",
            "If the only blue or red permanent you control is a creature that's dealt lethal damage at the " +
                "same time as Wandering Champion deals combat damage, Wandering Champion's ability triggers."
        )
        ruling(
            "2018-12-07",
            "If you don't control a blue or red permanent as Wandering Champion's ability resolves, you can't " +
                "discard and draw a card. The blue or red permanent you control as the ability resolves doesn't " +
                "have to be one you controlled as the ability triggered."
        )
    }
}
