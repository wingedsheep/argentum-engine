package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scrapwork Rager
 * {4}
 * Artifact Creature — Phyrexian Horror
 * 2/2
 * When this creature enters, you draw a card and you lose 1 life.
 * Unearth {3}{B}
 *
 * The enters trigger is Phyrexian Rager's spelling: draw, then lose 1 life.
 */
val ScrapworkRager = card("Scrapwork Rager") {
    manaCost = "{4}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Horror"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you draw a card and you lose 1 life.\n" +
        "Unearth {3}{B} ({3}{B}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    unearth("{3}{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Michal Ivan"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e28de770-7975-4aa9-b656-e7dbe6bff290.jpg?1783920077"
    }
}
