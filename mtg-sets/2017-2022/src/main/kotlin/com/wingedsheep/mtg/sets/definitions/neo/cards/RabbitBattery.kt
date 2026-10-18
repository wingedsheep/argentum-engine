package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Rabbit Battery — Kamigawa: Neon Dynasty #157 (canonical printing)
 * {R} · Artifact Creature — Equipment Rabbit · 1/1
 *
 * Haste
 * Equipped creature gets +1/+1 and has haste.
 * Reconfigure {R}
 */
val RabbitBattery = card("Rabbit Battery") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Equipment Rabbit"
    power = 1
    toughness = 1
    oracleText = "Haste\nEquipped creature gets +1/+1 and has haste.\n" +
        "Reconfigure {R} ({R}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
    }

    reconfigure("{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d33a5b7-797b-4079-8d62-edd124c0fb5a.jpg?1783923861"
    }
}
