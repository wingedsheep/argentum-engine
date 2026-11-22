package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood Scrivener
 * {1}{B}
 * Creature — Zombie Wizard
 * 2/1
 * If you would draw a card while you have no cards in hand, instead you draw two cards and you
 * lose 1 life.
 *
 * Phial of Galadriel's empty-hand draw replacement with a life payment added. The replacement's
 * own two draws are new draw events this Scrivener can't replace again (CR 614.5), while a second
 * Scrivener still applies to the first of them — the cumulative behaviour the rulings describe.
 */
val BloodScrivener = card("Blood Scrivener") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie Wizard"
    power = 2
    toughness = 1
    oracleText = "If you would draw a card while you have no cards in hand, instead you draw two " +
        "cards and you lose 1 life."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.DrawCards(2) then Effects.LoseLife(1, EffectTarget.Controller),
            appliesTo = EventPattern.DrawEvent(),
            restrictions = listOf(Conditions.EmptyHand),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Peter Mohrbacher"
        flavorText = "Make sure you bleed the fine print."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ea8179a-d3c9-4cdc-a5b5-68cc73279050.jpg?1783940040"
        ruling("2013-04-15", "Any time you are instructed to draw more than one card, you draw them one at a time. For example, if you control Blood Scrivener and have no cards in hand and you're instructed to \"draw two cards,\" your first card draw is replaced by drawing two cards and losing 1 life, then you'll draw the second card from the original instruction. In total, you'll draw three cards and lose 1 life.")
        ruling("2013-04-15", "Each additional Blood Scrivener you control will effectively add one card and 1 life lost. Say you control two Blood Scriveners and would draw a card while you have no cards in hand. The effect of one Blood Scrivener will replace the event \"draw a card\" with \"draw two cards and lose 1 life.\" The effect of the other Blood Scrivener will replace the drawing of the first of those two cards with \"draw two cards and lose 1 life.\" You'll draw two cards and lose 1 life, then draw another card and lose another 1 life.")
    }
}
