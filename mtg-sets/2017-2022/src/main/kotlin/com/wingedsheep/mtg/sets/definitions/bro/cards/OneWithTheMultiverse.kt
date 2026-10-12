package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * One with the Multiverse — The Brothers' War #59.
 *
 * The top card is looked at privately ([LookAtTopOfLibrary]), not revealed, so the play permission
 * is the non-revealing [PlayLandsAndCastFilteredFromTopOfLibrary] over every spell. The free cast is
 * a once-per-turn [MayCastWithoutPayingManaCost] scoped to hand and library casts, offered as its
 * own "(Free)" action on both paths and spent on this permanent once used.
 */
val OneWithTheMultiverse = card("One with the Multiverse") {
    manaCost = "{6}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may play lands and cast spells from the top of your library.\n" +
        "Once during each of your turns, you may cast a spell from your hand or the top of your " +
        "library without paying its mana cost."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Any)
    }

    staticAbility {
        ability = MayCastWithoutPayingManaCost(
            controllerOnly = true,
            oncePerTurn = true,
            fromZones = setOf(Zone.HAND, Zone.LIBRARY),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7868529f-8abb-4b1d-9c04-e628f81e08d2.jpg?1783920107"
    }
}
