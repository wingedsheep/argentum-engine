package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dream Eater (GRN #38)
 * {4}{U}{U}  Creature — Nightmare Sphinx  4/3
 *
 * Flash
 * Flying
 * When this creature enters, surveil 4. When you do, you may return target nonland permanent an
 * opponent controls to its owner's hand.
 *
 * Modeling: a reflexive trigger whose action is the surveil itself, so the target is chosen after
 * surveilling (2018-10-05 ruling). Surveilling always happens — even with fewer than four cards
 * in the library — so the reflexive trigger is mandatory (`optional = false`); the target is a
 * plain "target", and the "you may" is the resolution-time choice to return it.
 */
val DreamEater = card("Dream Eater") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Nightmare Sphinx"
    power = 4
    toughness = 3
    oracleText = "Flash\nFlying\nWhen this creature enters, surveil 4. When you do, you may return " +
        "target nonland permanent an opponent controls to its owner's hand. (To surveil 4, look at " +
        "the top four cards of your library, then put any number of them into your graveyard and " +
        "the rest on top of your library in any order.)"

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(action = Effects.Surveil(4), optional = false) {
            val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
            effect = Effects.May(Effects.ReturnToHand(permanent))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f3a4677-7683-4f63-af31-b76491ec9f9c.jpg?1783934188"
        ruling(
            "2018-10-05",
            "Dream Eater's triggered ability goes on the stack without a target. While that ability is " +
                "resolving, after you've surveilled, the reflexive triggered ability triggers and you pick " +
                "a target nonland permanent to be returned to its owner's hand. This is different from " +
                "effects that say \"If you do . . .\" in that you choose the target after having surveilled."
        )
        ruling(
            "2018-10-05",
            "Dream Eater's reflexive triggered ability triggers even if you have fewer than four cards " +
                "in your library to surveil."
        )
    }
}
