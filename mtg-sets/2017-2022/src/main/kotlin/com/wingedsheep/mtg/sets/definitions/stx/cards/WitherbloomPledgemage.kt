package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Witherbloom Pledgemage — Strixhaven: School of Mages #249 (canonical printing)
 * {3}{B/G}{B/G} · Creature — Treefolk Warlock · 5/5
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, you gain 1 life.
 */
val WitherbloomPledgemage = card("Witherbloom Pledgemage") {
    manaCost = "{3}{B/G}{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Treefolk Warlock"
    power = 5
    toughness = 5
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.GainLife(1)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, you gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Filip Burburan"
        flavorText = "\"Death is really just a transfer of energy. Those pests I harvested have the " +
            "honor of becoming pure magic. What could be more beautiful?\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6cc6914-2dcb-411d-90a5-01853ff2d5b8.jpg?1783927288"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
