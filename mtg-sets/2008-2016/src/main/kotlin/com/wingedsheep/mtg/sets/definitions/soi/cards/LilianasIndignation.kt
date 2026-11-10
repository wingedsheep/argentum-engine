package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Liliana's Indignation
 * {X}{B}
 * Sorcery
 * Mill X cards. Target player loses 2 life for each creature card put into your graveyard this way.
 *
 * Patient Rebuilding's shape on an X mill: the pipeline mill returns the cards milled by this
 * resolution, filtered to creature cards *currently in a graveyard* — so a card a replacement
 * effect sent elsewhere (Rest in Peace) isn't counted, and creature cards already in your
 * graveyard never are. X is read at resolution via [DynamicAmounts.xValue]; milling more cards
 * than your library holds just mills what's there (ruling).
 */
val LilianasIndignation = card("Liliana's Indignation") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Mill X cards. Target player loses 2 life for each creature card put into your graveyard this way."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val milled = mill(DynamicAmounts.xValue())
            val creatures = filter(milled, GameObjectFilter.Creature.currentlyIn(Zone.GRAVEYARD))
            run(Effects.LoseLife(creatures.count * 2, player))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Daarken"
        flavorText = "\"Days ago, you came to my door asking for help, Jace. Yet now here you are with accusations?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3817ea1a-b3d8-46f1-ac6c-69caf2466bab.jpg?1783937770"
        ruling("2016-04-08", "You don't lose the game for having zero cards in your library until you're instructed to draw a card. X can be greater than or equal to the number of cards in your library without causing you to lose the game.")
    }
}
