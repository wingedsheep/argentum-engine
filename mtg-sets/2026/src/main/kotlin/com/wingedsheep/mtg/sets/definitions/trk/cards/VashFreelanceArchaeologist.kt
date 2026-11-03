package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vash, Freelance Archaeologist
 * {1}{B}{G}
 * Legendary Creature — Human Rogue Scientist
 * 2/4
 *
 * At the beginning of your upkeep, surveil 1.
 * Whenever a card leaves your graveyard, draw a card. This ability triggers only once each turn.
 *
 * Because the second ability triggers only once each turn, "whenever a card leaves" and the
 * batched `oneOrMore(...).leaveYourGraveyard()` trigger resolve identically (same shape as
 * Kishla Skimmer and Hologram Stalker).
 */
val VashFreelanceArchaeologist = card("Vash, Freelance Archaeologist") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Rogue Scientist"
    power = 2
    toughness = 4
    oracleText = "At the beginning of your upkeep, surveil 1. (Look at the top card of your library. " +
        "You may put it into your graveyard.)\n" +
        "Whenever a card leaves your graveyard, draw a card. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Surveil(1)
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Nicky Barkla"
        flavorText = "\"Still think I'm trouble?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1f51765-f336-4ca2-a0f5-e42dc352c0fa.jpg?1792508403"
    }
}
