package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Primary Research
 * {4}{W}
 * Enchantment
 *
 * When this enchantment enters, return target nonland permanent card with mana value 3 or less
 * from your graveyard to the battlefield.
 * At the beginning of your end step, if a card left your graveyard this turn, draw a card.
 *
 * The ETB reanimates a nonland permanent card (MV ≤ 3) owned by you, putting it directly onto the
 * battlefield via [Effects.PutOntoBattlefield]. The end-step ability is an intervening-if trigger
 * (Rule 603.4): the [Conditions.CardsLeftGraveyardThisTurn] condition is checked both when the
 * trigger would fire and again on resolution — and the ETB reanimation itself counts as a card
 * leaving your graveyard.
 */
val PrimaryResearch = card("Primary Research") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, return target nonland permanent card with mana " +
        "value 3 or less from your graveyard to the battlefield.\n" +
        "At the beginning of your end step, if a card left your graveyard this turn, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val card = target(
            "card",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.NonlandPermanent.ownedByYou().manaValueAtMost(3),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.PutOntoBattlefield(card)
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CardsLeftGraveyardThisTurn(1)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Michal Ivan"
        flavorText = "Witnessing something firsthand is somehow both the best and the worst way to learn."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6fdb814-45c6-4d14-afff-7f5bd1bd10a1.jpg?1775937094"
    }
}
