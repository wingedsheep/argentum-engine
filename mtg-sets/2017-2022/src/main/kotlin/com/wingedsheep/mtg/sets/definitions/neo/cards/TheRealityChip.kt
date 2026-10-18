package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * The Reality Chip — Kamigawa: Neon Dynasty #74 (canonical printing)
 * {1}{U} · Legendary Artifact Creature — Equipment Jellyfish · 0/4
 *
 * You may look at the top card of your library any time.
 * As long as The Reality Chip is attached to a creature, you may play lands and cast spells from
 * the top of your library.
 * Reconfigure {2}{U}
 *
 * The peek is unconditional. The play permission is [PlayLandsAndCastFilteredFromTopOfLibrary]
 * with an unrestricted spell filter, gated by a conditional static on the source being attached
 * to a creature (the Glowcap Lantern gate). Land plays from the top still use the normal
 * land-drop allowance.
 */
val TheRealityChip = card("The Reality Chip") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact Creature — Equipment Jellyfish"
    power = 0
    toughness = 4
    oracleText = "You may look at the top card of your library any time.\n" +
        "As long as The Reality Chip is attached to a creature, you may play lands and cast spells " +
        "from the top of your library.\n" +
        "Reconfigure {2}{U} ({2}{U}: Attach to target creature you control; or unattach from a " +
        "creature. Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.attachedTo(GameObjectFilter.Creature))
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Any)
    }

    reconfigure("{2}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d859de3a-0be1-4e66-b438-1c3d4ee756cd.jpg?1783923896"
    }
}
