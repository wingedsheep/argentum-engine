package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Farsight Mask — Mirrodin #170
 * {5} · Artifact
 *
 * Whenever a source an opponent controls deals damage to you, if this artifact is untapped,
 * you may draw a card.
 *
 * "A source an opponent controls" is the trigger's `sourceFilter`
 * ([Triggers.damageDealtToYou]`(GameObjectFilter.Any.opponentControls())`) — any object, not just
 * creatures, matched relative to the Mask's controller. Per the 2004 rulings the ability watches
 * each *instance* of damage: two unblocked attackers, or one double strike, each trigger it
 * separately, and a single hit of any size still draws at most one card.
 *
 * "If this artifact is untapped" is an intervening-"if" (CR 603.4), which is checked twice — once
 * when the ability would trigger and again as it resolves ("Farsight Mask must be untapped both
 * when the damage is dealt and when you would draw the card"). The engine's `triggerCondition`
 * covers the first check only, so the resolution-time re-check is the explicit
 * [ConditionalEffect] around the draw; tapping the Mask in response correctly stops the draw.
 */
val FarsightMask = card("Farsight Mask") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a source an opponent controls deals damage to you, if this artifact is " +
        "untapped, you may draw a card."

    triggeredAbility {
        trigger = Triggers.damageDealtToYou(GameObjectFilter.Any.opponentControls())
        triggerCondition = Conditions.SourceIsUntapped
        effect = ConditionalEffect(
            condition = Conditions.SourceIsUntapped,
            effect = MayEffect(Effects.DrawCards(1)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Ben Thompson"
        flavorText = "It turns the adversity of the moment into the knowledge of a lifetime."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98312bc3-9d2a-480c-bcf0-db8d70d632b9.jpg?1783944521"
        ruling(
            "2004-12-01",
            "This triggers each time a source an opponent controls deals damage to you. If the same " +
                "source deals damage more than once in a turn, it triggers for each of those times."
        )
        ruling(
            "2004-12-01",
            "You draw no more than one card each time a source an opponent controls damages you, no " +
                "matter how much damage the source deals."
        )
        ruling(
            "2004-12-01",
            "Farsight Mask must be untapped both when the damage is dealt and when you would draw the card."
        )
    }
}
