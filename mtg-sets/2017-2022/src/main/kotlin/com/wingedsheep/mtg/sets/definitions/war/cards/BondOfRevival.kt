package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bond of Revival — War of the Spark #80 (canonical printing)
 * {4}{B} · Sorcery
 *
 * Return target creature card from your graveyard to the battlefield. It gains haste until your
 * next turn.
 *
 * Scrap Welder's reanimate-then-grant shape: the target handle follows the card onto the
 * battlefield, and the haste lasts until your next turn (Offspring's Revenge's duration).
 */
val BondOfRevival = card("Bond of Revival") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield. " +
        "It gains haste until your next turn."

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature, Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Lake Hurwitz"
        flavorText = "\"Today we throw off the yoke of death. Either this is an affirmation of the " +
            "eternal cyclical nature of life, or it's just hilarious—depends on which of us you ask.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6482fb9-f874-4e33-aa74-7a0da4c9760c.jpg?1783933451"
    }
}
