package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Prismari Pledgemage — Strixhaven: School of Mages #215 (canonical printing)
 * {U/R}{U/R} · Creature — Orc Wizard · 3/3
 *
 * Defender
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature can attack
 * this turn as though it didn't have defender.
 */
val PrismariPledgemage = card("Prismari Pledgemage") {
    manaCost = "{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Orc Wizard"
    power = 3
    toughness = 3
    oracleText = "Defender\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature can " +
        "attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CanAttackDespiteDefenderThisTurn()
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this " +
            "creature can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Marta Nael"
        flavorText = "Many have the talent to create, but few have the courage to share their creations."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/404b4c7b-5d40-4ad1-bd40-3da1d08f5c78.jpg?1783927301"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
