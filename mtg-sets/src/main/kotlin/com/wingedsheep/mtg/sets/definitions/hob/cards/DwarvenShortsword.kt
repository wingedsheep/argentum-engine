package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dwarven Shortsword
 * {3}{W}
 * Artifact — Equipment
 *
 * When this Equipment enters, create a 2/2 red Dwarf creature token, then attach this Equipment to it.
 * Equipped creature gets +1/+2.
 * Equip {2}
 */
val DwarvenShortsword = card("Dwarven Shortsword") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 2/2 red Dwarf creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +1/+2.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dwarf"),
            imageUri = "https://cards.scryfall.io/normal/front/9/f/9fcb3a3f-c0d4-43d4-8549-826a38bfa27d.jpg?1785497537",
        ).then(Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)))
    }

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Manuel Castañón"
        flavorText = "Dwarves lived long lives if not felled in battle, and they forged their blades to last even longer."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2341cf3-4d2c-4a4f-9aea-8834104a8910.jpg?1785496931"
    }
}
