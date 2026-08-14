package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * The Belligerent
 * {2}{U}{R}
 * Legendary Artifact — Vehicle
 * 5/5
 *
 * Whenever The Belligerent attacks, create a Treasure token. Until end of turn, you may look at the
 * top card of your library any time, and you may play lands and cast spells from the top of your
 * library.
 * Crew 3
 *
 * The attack trigger creates the Treasure token (a one-shot effect). The "until end of turn" play
 * window is modeled as the Lunar Whale pattern: two conditional statics gated on
 * [Conditions.SourceAttackedThisTurn]. This approximates the printed card, which grants the
 * permission as a one-shot effect of the trigger: (a) if The Belligerent leaves the battlefield
 * after attacking (dies in combat, bounced), the real permission lasts until end of turn but the
 * statics end with the permanent; (b) the window opens at attack declaration rather than trigger
 * resolution. Fixing both needs an until-end-of-turn floating permission grant (add-feature
 * territory). [LookAtTopOfLibrary] grants the non-revealing peek, and
 * [PlayLandsAndCastFilteredFromTopOfLibrary] with an unrestricted [GameObjectFilter.Any] spell
 * filter grants "play lands and cast spells from the top of your library" (any card type).
 */
val TheBelligerent = card("The Belligerent") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Whenever The Belligerent attacks, create a Treasure token. Until end of turn, you " +
        "may look at the top card of your library any time, and you may play lands and cast spells " +
        "from the top of your library.\n" +
        "Crew 3"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateTreasure(1)
    }

    staticAbility {
        condition = Conditions.SourceAttackedThisTurn
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        condition = Conditions.SourceAttackedThisTurn
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Any)
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1454af8c-bce9-47d3-890f-283e2fea2cf2.jpg?1782694429"
    }
}
