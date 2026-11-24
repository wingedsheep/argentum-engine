package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Oak Street Innkeeper
 * {2}{G}
 * Creature — Elf
 * 1/2
 * During turns other than yours, tapped creatures you control have hexproof.
 *
 * A hexproof lord whose group filter carries the tapped state predicate, gated on
 * [Conditions.IsNotYourTurn]; both are re-read every projection.
 */
val OakStreetInnkeeper = card("Oak Street Innkeeper") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 2
    oracleText = "During turns other than yours, tapped creatures you control have hexproof."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(
                Keyword.HEXPROOF,
                GroupFilter(GameObjectFilter.Creature.tapped().youControl()),
            ),
            condition = Conditions.IsNotYourTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Svetlin Velinov"
        flavorText = "The city is vast and perilous, but there are places where a weary traveler can feel very much at home."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08274d1b-52b7-46c1-8f93-7631d2e21def.jpg?1783940347"
    }
}
