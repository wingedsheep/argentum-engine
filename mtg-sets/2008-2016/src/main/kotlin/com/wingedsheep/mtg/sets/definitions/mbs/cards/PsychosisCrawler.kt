package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

// Oracle's modern creature type includes Phyrexian.
val PsychosisCrawler = card("Psychosis Crawler") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Phyrexian Horror"
    oracleText = "Psychosis Crawler's power and toughness are each equal to the number of cards in your hand.\n" +
        "Whenever you draw a card, each opponent loses 1 life."

    dynamicStats(DynamicAmounts.cardsInYourHand())

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Stephan Martiniere"
        flavorText = "\"If that brain can't figure out the secret of the serum, then add more brains.\"\n—Rhmir, Hand of the Augur"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dd84701-857e-4948-8cb8-39b8a321a177.jpg?1783941364"
        ruling("2011-06-01", "If an effect causes you to draw multiple cards, Psychosis Crawler will trigger that many times.")
    }
}
