package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Consign // Oblivion — Hour of Devastation #149
 *
 * Consign {1}{U} — Instant: Return target nonland permanent to its owner's hand.
 * Oblivion {4}{B} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Target opponent discards two cards.
 */
val ConsignOblivion = card("Consign // Oblivion") {
    layout = CardLayout.SPLIT
    colorIdentity = "UB"

    face("Consign") {
        manaCost = "{1}{U}"
        typeLine = "Instant"
        oracleText = "Return target nonland permanent to its owner's hand."

        spell {
            val permanent = target(TargetFilter.NonlandPermanent)
            effect = Effects.ReturnToHand(permanent)
        }
    }

    face("Oblivion") {
        manaCost = "{4}{B}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Target opponent discards two cards."
        keywords(Keyword.AFTERMATH)

        spell {
            val opponent = target(Targets.Opponent)
            effect = Patterns.Hand.discardCards(2, opponent)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c1ead90-10d8-4217-80e4-6f40320c5569.jpg?1783936007"
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
