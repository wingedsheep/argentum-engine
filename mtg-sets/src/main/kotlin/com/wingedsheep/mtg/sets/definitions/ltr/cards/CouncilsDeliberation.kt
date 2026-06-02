package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Council's Deliberation
 * {1}{U}
 * Instant
 *
 * Draw a card.
 * Whenever you scry, if you control an Island, you may exile this card from
 * your graveyard. If you do, draw a card.
 */
val CouncilsDeliberation = card("Council's Deliberation") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card.\n" +
        "Whenever you scry, if you control an Island, you may exile this card from your graveyard. " +
        "If you do, draw a card."

    spell {
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.WheneverYouScry
        triggerZone = Zone.GRAVEYARD
        triggerCondition = Conditions.YouControl(GameObjectFilter.Land.withSubtype(Subtype.ISLAND))
        effect = MayEffect(
            IfYouDoEffect(
                action = Effects.Move(EffectTarget.Self, Zone.EXILE),
                ifYouDo = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Viko Menezes"
        flavorText = "\"I will take the Ring to Mordor, though I do not know the way.\"\n—Frodo"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/651503a2-5d1e-408a-9cdc-0cee05ab3ef0.jpg?1686968054"
    }
}
