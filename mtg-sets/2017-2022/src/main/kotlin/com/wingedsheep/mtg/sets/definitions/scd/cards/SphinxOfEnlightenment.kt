package com.wingedsheep.mtg.sets.definitions.scd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sphinx of Enlightenment {4}{U}{U}
 * Creature — Sphinx
 * 5/5
 * Flying
 * When this creature enters, target opponent draws a card and you draw three cards.
 *
 * Canonical lives in SCD: its earlier Game Night 2019 printing is a box set, not a real release.
 */
val SphinxOfEnlightenment = card("Sphinx of Enlightenment") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhen this creature enters, target opponent draws a card and you draw three cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1, opponent) then Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "61"
        artist = "Johan Grenier"
        flavorText = "\"I would be a fool if I taught you everything I know.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c681f35-e584-45a0-aa4d-bc8955ab1a8a.jpg"
    }
}
