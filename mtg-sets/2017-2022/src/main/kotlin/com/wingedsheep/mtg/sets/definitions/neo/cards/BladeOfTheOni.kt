package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantColor
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic

/**
 * Blade of the Oni — Kamigawa: Neon Dynasty #89 (canonical printing)
 * {1}{B} · Artifact Creature — Equipment Demon · 3/1
 *
 * Menace
 * Equipped creature has base power and toughness 5/5, has menace, and is a black Demon in
 * addition to its other colors and types.
 * Reconfigure {2}{B}{B}
 */
val BladeOfTheOni = card("Blade of the Oni") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Equipment Demon"
    power = 3
    toughness = 1
    oracleText = "Menace\n" +
        "Equipped creature has base power and toughness 5/5, has menace, and is a black Demon in " +
        "addition to its other colors and types.\n" +
        "Reconfigure {2}{B}{B} ({2}{B}{B}: Attach to target creature you control; or unattach from a " +
        "creature. Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.MENACE)

    staticAbility {
        ability = SetBasePowerToughnessStatic(5, 5, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantColor(Color.BLACK, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantSubtype(Subtype.DEMON.value, Filters.EquippedCreature)
    }

    reconfigure("{2}{B}{B}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "89"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6bc3f71-b4ce-4c05-be3c-7a0faad476c3.jpg?1783923890"
    }
}
