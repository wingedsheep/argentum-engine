package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vat Emergence
 * {4}{B}
 * Sorcery
 * Put target creature card from a graveyard onto the battlefield under your control. Proliferate.
 */
val VatEmergence = card("Vat Emergence") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Put target creature card from a graveyard onto the battlefield under your control. Proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature, underYourControl = true) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Andreas Zafiratos"
        flavorText = "The first rebirth is a reward. The fifty-first is a punishment."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a2aeaeb-7853-4588-8a5e-cfd997ba8515.jpg?1783918038"
    }
}
