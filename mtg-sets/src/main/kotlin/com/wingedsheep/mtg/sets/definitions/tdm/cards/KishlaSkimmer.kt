package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kishla Skimmer — Tarkir: Dragonstorm #201
 * {G}{U} · Creature — Bird Scout · 2/2
 *
 * Flying
 * Whenever a card leaves your graveyard during your turn, draw a card. This ability
 * triggers only once each turn.
 *
 * The leave-graveyard trigger already batches (fires once per event batch); oncePerTurn = true
 * additionally caps it to a single fire across the whole turn, matching the "only once each
 * turn" wording. "During your turn" is triggerCondition = Conditions.IsYourTurn.
 */
val KishlaSkimmer = card("Kishla Skimmer") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Bird Scout"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever a card leaves your graveyard during your turn, draw a card. " +
        "This ability triggers only once each turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard()
        triggerCondition = Conditions.IsYourTurn
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5f1acb0-d73e-4814-8158-3645daf5c4cc.jpg?1743204788"
    }
}
