package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost

/**
 * Zaffai and the Tempests — Secrets of Strixhaven #246
 * {5}{U}{R} · Legendary Creature — Human Bard Sorcerer · 5/7
 *
 * Once during each of your turns, you may cast an instant or sorcery spell from your hand without
 * paying its mana cost.
 *
 * Modeled as [MayCastWithoutPayingManaCost] with the `oncePerTurn` gate (one free cast per your
 * turn, not necessarily your first spell) and `controllerOnly` (only you), filtered to instant and
 * sorcery spells. The free-cast permission is only ever offered for cards in hand, matching "from
 * your hand". Each use marks the source until end of turn.
 */
val ZaffaiAndTheTempests = card("Zaffai and the Tempests") {
    manaCost = "{5}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Bard Sorcerer"
    power = 5
    toughness = 7
    oracleText = "Once during each of your turns, you may cast an instant or sorcery spell from " +
        "your hand without paying its mana cost."

    staticAbility {
        ability = MayCastWithoutPayingManaCost(
            controllerOnly = true,
            oncePerTurn = true,
            spellFilter = GameObjectFilter.InstantOrSorcery,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Olivier Bernard"
        flavorText = "\"Leave everything on the stage and give them a performance they'll never " +
            "forget.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bdbf507-6fd7-49f6-b437-8f2ce2d0eb0f.jpg?1775938717"
    }
}
