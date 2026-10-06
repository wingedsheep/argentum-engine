package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prickly Marmoset
 * {2}{R}
 * Creature — Monkey
 * 2/3
 * First strike
 * Whenever you cycle a card, this creature gets +2/+0 until end of turn.
 */
val PricklyMarmoset = card("Prickly Marmoset") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Monkey"
    oracleText = "First strike\n" +
        "Whenever you cycle a card, this creature gets +2/+0 until end of turn."
    power = 2
    toughness = 3

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.cycles()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you cycle a card, this creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Simon Dominic"
        flavorText = "\"It's either terrified or extremely mad at us, definitely one of the two.\"\n—Jonald, mission naturalist"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bad8512f-31b9-48ba-bb10-1497303dcfba.jpg?1783931046"
        ruling("2020-04-17", "Some cards with cycling have an ability that triggers when you cycle them, and some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
    }
}
