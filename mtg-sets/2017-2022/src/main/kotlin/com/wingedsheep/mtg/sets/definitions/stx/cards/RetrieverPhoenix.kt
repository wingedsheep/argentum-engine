package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceKeywordAction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Retriever Phoenix — Strixhaven: School of Mages #113 (canonical printing)
 * {3}{R} · Creature — Phoenix · 2/2
 *
 * Flying, haste
 * When this creature enters, if you cast it, learn.
 * As long as this card is in your graveyard, if you would learn, you may instead return this card
 * to the battlefield.
 *
 * "If you cast it" is the intervening-if [Conditions.WasCast] (CR 603.4), so a Phoenix returned by
 * its own replacement — which wasn't cast — doesn't learn again.
 *
 * The last line is a [ReplaceKeywordAction] over [EventPattern.LearnEvent] that functions only
 * from the graveyard (`activeZones`, CR 113.6). The learn executor sweeps graveyards for it at the
 * moment the learn would happen — the Phoenix has to be there then (ruling) — and the return runs
 * rooted at the Phoenix, so `EffectTarget.Self` is the card in the graveyard.
 */
val RetrieverPhoenix = card("Retriever Phoenix") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\n" +
        "When this creature enters, if you cast it, learn. (You may reveal a Lesson card you own " +
        "from outside the game and put it into your hand, or discard a card to draw a card.)\n" +
        "As long as this card is in your graveyard, if you would learn, you may instead return " +
        "this card to the battlefield."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Patterns.Mechanic.learn()
    }

    replacementEffect(
        ReplaceKeywordAction(
            replacementEffect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self),
            appliesTo = EventPattern.LearnEvent(),
            optional = true,
            activeZones = setOf(Zone.GRAVEYARD)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dd6cb53-3c82-4ac3-be1e-311322729198.jpg?1783927349"
        ruling(
            "2021-04-16",
            "Retriever Phoenix must be in your graveyard at the moment you're instructed to learn if " +
                "you want to use the last ability to return it to the battlefield."
        )
        ruling(
            "2021-04-16",
            "If Retriever Phoenix somehow dies with its triggered ability on the stack, you may " +
                "return it to the battlefield with that triggered ability."
        )
    }
}
