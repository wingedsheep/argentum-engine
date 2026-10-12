package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/** Ruinous Ultimatum — Ikoria: Lair of Behemoths #204. */
val RuinousUltimatum = card("Ruinous Ultimatum") {
    manaCost = "{R}{R}{W}{W}{W}{B}{B}"
    colorIdentity = "BRW"
    typeLine = "Sorcery"
    oracleText = "Destroy all nonland permanents your opponents control."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.NonlandPermanent.opponentControls())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Chase Stone"
        flavorText = "\"Strike those abominations down. Let their names rot with their remains.\"\n—General Kudro"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50c1d6ca-7789-46b5-bc89-85cc3915cb85.jpg?1783931018"
    }
}
