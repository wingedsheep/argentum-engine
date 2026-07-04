package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveFlashback

/**
 * Iroh, Grand Lotus
 * {3}{G}{U}{R}
 * Legendary Creature — Human Noble Ally
 * 5/5
 * Firebending 2 (Whenever this creature attacks, add {R}{R}. Until end of combat, you don't lose
 *   this mana as steps and phases end.)
 * During your turn, each non-Lesson instant and sorcery card in your graveyard has flashback. The
 *   flashback cost is equal to that card's mana cost.
 * During your turn, each Lesson card in your graveyard has flashback {1}.
 *
 * The two graveyard clauses are whole-graveyard flashback grants ([GraveyardCardsHaveFlashback],
 * CR 702.34): a continuous static that hands flashback to *every* matching card in your graveyard
 * (not a single-card grant), gated to your turn. Non-Lesson instants/sorceries flash back for their
 * own mana cost (grant leaves the cost null); Lessons flash back for a fixed {1}.
 */
val IrohGrandLotus = card("Iroh, Grand Lotus") {
    manaCost = "{3}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 5
    toughness = 5
    oracleText = "Firebending 2\n" +
        "During your turn, each non-Lesson instant and sorcery card in your graveyard has flashback. " +
        "The flashback cost is equal to that card's mana cost. (You may cast a card from your " +
        "graveyard for its flashback cost. Then exile it.)\n" +
        "During your turn, each Lesson card in your graveyard has flashback {1}."

    firebending(2)

    // "During your turn, each non-Lesson instant and sorcery card in your graveyard has flashback.
    //  The flashback cost is equal to that card's mana cost." (cost = null -> the card's mana cost.)
    staticAbility {
        ability = GraveyardCardsHaveFlashback(
            filter = GameObjectFilter.InstantOrSorcery.notSubtype(Subtype.LESSON),
            duringYourTurnOnly = true,
        )
    }

    // "During your turn, each Lesson card in your graveyard has flashback {1}."
    staticAbility {
        ability = GraveyardCardsHaveFlashback(
            filter = GameObjectFilter.InstantOrSorcery.withSubtype(Subtype.LESSON),
            cost = ManaCost.parse("{1}"),
            duringYourTurnOnly = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Fahmi Fauzi"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/879b73d3-4552-4fdc-baee-c4d097ae9a4f.jpg?1764121663"
    }
}
