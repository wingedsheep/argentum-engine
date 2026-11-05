package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Dagger of the Worthy
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +2/+0 and has afflict 1. (Whenever it becomes blocked, defending player
 * loses 1 life.)
 * Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)
 *
 * The afflict grant floats the projected `AFFLICT_1` keyword, from which the engine derives the
 * "becomes blocked, defending player loses 1 life" trigger (see [com.wingedsheep.sdk.scripting.Afflict]).
 */
val DaggerOfTheWorthy = card("Dagger of the Worthy") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0 and has afflict 1. (Whenever it becomes blocked, " +
        "defending player loses 1 life.)\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GrantKeyword("${Keyword.AFFLICT.name}_1")
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abe9d9d7-bc4e-4367-b27c-e8d6e430467e.jpg?1783936002"
    }
}
