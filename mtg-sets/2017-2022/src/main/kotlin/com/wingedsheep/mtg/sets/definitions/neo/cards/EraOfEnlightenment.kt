package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Era of Enlightenment // Hand of Enlightenment
 * {1}{W} — Enchantment — Saga // Enchantment Creature — Human Monk 2/2
 *
 * Front — Era of Enlightenment:
 *   I — Scry 2.
 *   II — You gain 2 life.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Hand of Enlightenment:
 *   First strike
 */
private val HandOfEnlightenment = card("Hand of Enlightenment") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Monk"
    oracleText = "First strike"
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Amayagido"
        flavorText = "\"We have endured lifetimes of war and carved our way to peace by blade and " +
            "conquest. Now, united at last under Kyodai's guidance, we lay down our weapons and open " +
            "our minds.\"\n—Aoha, general of the First Emperor"
        imageUri = "https://cards.scryfall.io/normal/back/7/4/74f43827-2b9b-4fc9-a6e2-c682dfbf14f7.jpg?1783923930"
    }
}

private val EraOfEnlightenmentFront = card("Era of Enlightenment") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Scry 2.\n" +
        "II — You gain 2 life.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Effects.Scry(2)
    }

    sagaChapter(2) {
        effect = Effects.GainLife(2)
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Amayagido"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74f43827-2b9b-4fc9-a6e2-c682dfbf14f7.jpg?1783923930"
    }
}

val EraOfEnlightenment: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = EraOfEnlightenmentFront,
    backFace = HandOfEnlightenment,
)
