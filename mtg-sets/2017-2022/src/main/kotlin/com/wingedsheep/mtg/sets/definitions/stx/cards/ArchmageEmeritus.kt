package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Archmage Emeritus — Strixhaven: School of Mages #37
 * {2}{U}{U} · Creature — Human Wizard · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, draw a card.
 */
val ArchmageEmeritus = card("Archmage Emeritus") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, draw a card."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.DrawCards(1)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Caio Monteiro"
        flavorText = "Some emeritus professors return from their wanderings periodically to share tales of the mystical wonders of Arcavios."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/761df6cd-0928-4167-8902-58fdb50181a0.jpg?1783927381"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
