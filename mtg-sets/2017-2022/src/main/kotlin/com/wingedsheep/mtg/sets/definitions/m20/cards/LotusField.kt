package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Lotus Field — Core Set 2020 #249 (canonical printing)
 * (no mana cost) · Land
 *
 * Hexproof
 * This land enters tapped.
 * When this land enters, sacrifice two lands.
 * {T}: Add three mana of any one color.
 *
 * Original printing said "sacrifice two untapped lands"; current Oracle drops "untapped".
 * The sacrifice is the bare imperative ([Effects.SacrificeOwn]) over any land you control,
 * Lotus Field included — with fewer than two other lands you sacrifice every land you control,
 * Lotus Field among them (2019-07-12 ruling).
 */
val LotusField = card("Lotus Field") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "Hexproof\n" +
        "This land enters tapped.\n" +
        "When this land enters, sacrifice two lands.\n" +
        "{T}: Add three mana of any one color."

    keywords(Keyword.HEXPROOF)

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.SacrificeOwn(GameObjectFilter.Land, count = 2)
        description = "When this land enters, sacrifice two lands."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e013033-3995-4ba8-b0c3-0614c79aaaab.jpg?1783932935"
        ruling(
            "2019-07-12",
            "If Lotus Field enters the battlefield while you control fewer than two other lands, you must sacrifice each land you control, including Lotus Field."
        )
    }
}
