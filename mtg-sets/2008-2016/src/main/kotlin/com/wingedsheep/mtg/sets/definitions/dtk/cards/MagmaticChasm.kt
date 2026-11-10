package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Magmatic Chasm
 * {1}{R}
 * Sorcery
 * Creatures without flying can't block this turn.
 *
 * The restriction is a dynamic group restriction (it doesn't change characteristics), so creatures
 * without flying that enter later this turn can't block either.
 */
val MagmaticChasm = card("Magmatic Chasm") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Creatures without flying can't block this turn."

    spell {
        effect = Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Volkan Baǵa"
        flavorText = "\"Our shamans assert their mastery over the land, thwarting the lesser clans' attempts to stand in the way of our dragonlord.\"\n—Allek, Atarka hunter"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e62a56c4-a3d9-43ab-83a8-6333578bcbc8.jpg?1783938587"
        ruling(
            "2016-04-08",
            "Because Magmatic Chasm's effect doesn't change the characteristics of any permanents, the set of creatures affected by Magmatic Chasm is constantly updated. Creatures without flying that enter the battlefield later in the turn won't be able to block."
        )
    }
}
