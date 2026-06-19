package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Silverquill, the Disputant
 * {2}{W}{B}
 * Legendary Creature — Elder Dragon
 * 4/4
 *
 * Flying, vigilance
 * Each instant and sorcery spell you cast has casualty 1. (As you cast that spell, you may
 * sacrifice a creature with power 1 or greater. When you do, copy the spell and you may choose
 * new targets for the copy.)
 *
 * The casualty grant is a [GrantKeywordToOwnSpells] carrying the threshold via
 * [GrantKeywordToOwnSpells.keywordParameter]. The cast flow surfaces a "Cast (Casualty 1)" option
 * for matching instant/sorcery spells; paying the optional sacrifice (a creature with power >= 1)
 * queues a reflexive copy of the spell with new-target choice.
 */
val SilverquillTheDisputant = card("Silverquill, the Disputant") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "Each instant and sorcery spell you cast has casualty 1. (As you cast that spell, you may " +
        "sacrifice a creature with power 1 or greater. When you do, copy the spell and you may " +
        "choose new targets for the copy.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CASUALTY,
            spellFilter = GameObjectFilter.InstantOrSorcery,
            keywordParameter = 1
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "226"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1742c9cd-5ba0-4335-9999-acc7f9d4f73c.jpg?1775938577"
    }
}
