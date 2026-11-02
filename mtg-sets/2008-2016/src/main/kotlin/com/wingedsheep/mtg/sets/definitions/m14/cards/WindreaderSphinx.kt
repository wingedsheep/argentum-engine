package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Windreader Sphinx
 * {5}{U}{U}
 * Creature — Sphinx
 * 3/7
 *
 * Flying
 * Whenever a creature with flying attacks, you may draw a card.
 *
 * Canonical printing: Magic 2014, the card's earliest real printing.
 *
 * The trigger watches every creature with flying, whoever controls it — the filter carries no
 * controller restriction — and fires once per such attacker, including the Sphinx itself.
 */
val WindreaderSphinx = card("Windreader Sphinx") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    oracleText = "Flying\nWhenever a creature with flying attacks, you may draw a card."
    power = 3
    toughness = 7

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)).attacks()
        optional = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "81"
        artist = "Min Yum"
        flavorText = "\"Your mind is too filled with foolish concerns to hear the subtle whispers that the breeze brings.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f566741d-a847-4f24-b6fc-7873f0797d59.jpg?1783939928"
        ruling("2018-07-13", "It doesn't matter who controls the creature with flying or which player or planeswalker that creature is attacking. Windreader Sphinx's controller is the player who may draw a card.")
        ruling("2018-07-13", "Windreader Sphinx's ability will trigger when it itself attacks.")
        ruling("2018-07-13", "The creature must have flying immediately after being declared as an attacking creature in order for Windreader Sphinx's ability to trigger. For example, attacking with Kitesail Corsair will cause the ability to trigger, and attacking with Pegasus Courser and a creature without flying will cause the ability to trigger only once.")
    }
}
