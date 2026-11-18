package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Kruphix's Insight
 * {2}{G}
 * Sorcery
 *
 * Reveal the top six cards of your library. Put up to three enchantment cards from among them
 * into your hand and the rest of the revealed cards into your graveyard.
 *
 * Pieces of the Puzzle's shape: a public reveal of six, an up-to-three enchantment pick (the
 * non-enchantments are shown but unselectable), and everything not taken goes to the graveyard.
 */
val KruphixsInsight = card("Kruphix's Insight") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top six cards of your library. Put up to three enchantment cards from among " +
        "them into your hand and the rest of the revealed cards into your graveyard."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(6),
            filter = GameObjectFilter.Enchantment,
            prompt = "Put up to three enchantment cards from among them into your hand",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(3)),
            revealed = true,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Igor Kieryluk"
        flavorText = "The horizon is tantalizing in both its uncertainty and its potential."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94e14f09-9629-443a-bc55-03152eabb2e7.jpg?1783939413"
    }
}
