package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Stormtide Leviathan — Magic 2011 #74 (canonical printing)
 * {5}{U}{U}{U}
 * Creature — Leviathan
 * 8/8
 *
 * Islandwalk
 * All lands are Islands in addition to their other types.
 * Creatures without flying or islandwalk can't attack.
 *
 * The type change is the Urborg-shaped Layer 4 static over every land on the battlefield; the
 * "{T}: Add {U}" each land gains is the Island type's intrinsic mana ability, not scripted. The
 * attack restriction covers every creature with neither flying nor islandwalk, whoever controls
 * it, read off projected keywords.
 */
val StormtideLeviathan = card("Stormtide Leviathan") {
    manaCost = "{5}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 8
    toughness = 8
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
        "All lands are Islands in addition to their other types.\n" +
        "Creatures without flying or islandwalk can't attack."

    keywords(Keyword.ISLANDWALK)

    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter.AllLands,
            addSubtypes = listOf("Island")
        )
    }

    staticAbility {
        ability = CantAttack(
            GroupFilter(
                GameObjectFilter.Creature
                    .withoutKeyword(Keyword.FLYING)
                    .withoutKeyword(Keyword.ISLANDWALK)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e7f3fb6-93ce-4bc9-8efd-11af5a46218f.jpg?1783941820"

        ruling("2014-07-18", "Stormtide Leviathan's second ability causes each land on the battlefield to have the land type Island. Each land thus has the ability \"{T}: Add {U}.\" Nothing else changes about those lands, including their names, other subtypes, other abilities, and whether they're legendary or basic.")
        ruling("2014-07-18", "If Stormtide Leviathan loses its abilities, all lands on the battlefield (including those that enter the battlefield later on) will still be Islands in addition to their other types and will still be able to tap to produce {U}. The way continuous effects work, Stormtide Leviathan's type-changing ability is applied before the effect that removes that ability is applied.")
        ruling("2014-07-18", "Stormtide Leviathan's third ability affects all creatures with neither flying nor islandwalk, regardless of who controls them. They can't attack any player or planeswalker.")
        ruling("2014-07-18", "Islandwalk cares about lands with the land type Island, not necessarily lands named Island.")
    }
}
