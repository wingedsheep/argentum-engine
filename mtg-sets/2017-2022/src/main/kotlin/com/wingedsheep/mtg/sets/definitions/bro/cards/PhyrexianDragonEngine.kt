package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion

/**
 * Phyrexian Dragon Engine — The Brothers' War #163
 * {3} · Artifact Creature — Phyrexian Dragon · Rare
 * 2/2
 *
 * Double strike
 * When this creature enters from your graveyard, you may discard your hand. If you do, draw three cards.
 * Unearth {3}{R}{R}
 * (Melds with Mishra, Claimed by Gix.)
 *
 * The meld itself is Mishra, Claimed by Gix's attack trigger; this half carries only the reminder
 * text. "From your graveyard" on a self-trigger is any graveyard-to-battlefield entry of this card
 * (a card only ever sits in its owner's graveyard) — unearth, or a reanimation spell.
 */
val PhyrexianDragonEngine = card("Phyrexian Dragon Engine") {
    manaCost = "{3}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Phyrexian Dragon"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "When this creature enters from your graveyard, you may discard your hand. If you do, draw three cards.\n" +
        "Unearth {3}{R}{R} ({3}{R}{R}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. " +
        "Unearth only as a sorcery.)\n" +
        "(Melds with Mishra, Claimed by Gix.)"

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters(from = Zone.GRAVEYARD)
        effect = Effects.May(
            Effects.IfYouDo(
                action = Patterns.Hand.discardHand(),
                then = Effects.DrawCards(3),
                // Discarding your hand can be done with no cards in it, so the draw still happens.
                successCriterion = SuccessCriterion.Always,
            )
        )
        description = "When this creature enters from your graveyard, you may discard your hand. " +
            "If you do, draw three cards."
    }

    unearth("{3}{R}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b826be-4256-4fd6-ad4d-6c80933ee940.jpg?1783920055"
    }
}
