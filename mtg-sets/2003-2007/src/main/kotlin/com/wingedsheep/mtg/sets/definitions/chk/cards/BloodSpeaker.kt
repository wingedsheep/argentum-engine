package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood Speaker
 * {3}{B}
 * Creature — Ogre Shaman
 * 3/2
 * At the beginning of your upkeep, you may sacrifice this creature. If you do, search your library
 * for a Demon card, reveal that card, put it into your hand, then shuffle.
 * Whenever a Demon you control enters, return this card from your graveyard to your hand.
 *
 * The upkeep trigger follows `drk/cards/SafeHaven.kt`: an optional trigger whose "If you do" is gated
 * on the sacrifice actually happening ([SuccessCriterion.PermanentsSacrificed]), so a Blood Speaker
 * that already left the battlefield before resolution finds nothing.
 *
 * The return trigger lives in the graveyard (`triggerZone = GRAVEYARD`); per the 2004-12-01 ruling it
 * works no matter how Blood Speaker got there. "A Demon" is any permanent with the subtype.
 */
val BloodSpeaker = card("Blood Speaker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Shaman"
    power = 3
    toughness = 2
    oracleText = "At the beginning of your upkeep, you may sacrifice this creature. If you do, search your " +
        "library for a Demon card, reveal that card, put it into your hand, then shuffle.\n" +
        "Whenever a Demon you control enters, return this card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            then = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.withSubtype("Demon"),
                reveal = true
            ),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "At the beginning of your upkeep, you may sacrifice this creature. If you do, " +
            "search your library for a Demon card, reveal it, put it into your hand, then shuffle."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Demon").youControl()).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        description = "Whenever a Demon you control enters, return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f218bb94-d5a2-41f6-8bca-b689dcd09a43.jpg?1783944317"
        ruling(
            "2004-12-01",
            "The return-to-hand triggered ability triggers while Blood Speaker is in your graveyard, " +
                "no matter how it got there."
        )
    }
}
