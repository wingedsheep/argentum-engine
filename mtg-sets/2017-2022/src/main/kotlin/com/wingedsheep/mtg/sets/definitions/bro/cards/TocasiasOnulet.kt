package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity

/**
 * Tocasia's Onulet
 * {5}
 * Artifact Creature — Construct
 * 4/4
 * When this creature leaves the battlefield, you gain 2 life.
 * Unearth {3}{W}
 */
val TocasiasOnulet = card("Tocasia's Onulet") {
    manaCost = "{5}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "When this creature leaves the battlefield, you gain 2 life.\n" +
        "Unearth {3}{W} ({3}{W}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.GainLife(2)
    }

    unearth("{3}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Eddie Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4abf2fb-f78a-4e75-b46f-524d3d7405db.jpg?1783920118"
    }
}
