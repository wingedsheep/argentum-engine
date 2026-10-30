package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fog on the Barrow-Downs
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature is a Spirit and can't attack or block. (It loses all other creature types.)
 */
val FogOnTheBarrowDowns = card("Fog on the Barrow-Downs") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature is a Spirit and can't attack or block. (It loses all other creature types.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // "Enchanted creature is a Spirit. (It loses all other creature types.)"
    // Only replace creature subtypes — keep card types/colors/abilities intact.
    staticAbility {
        ability = TransformPermanent(
            setCreatureSubtypes = setOf("Spirit"),
            filter = GroupFilter.attachedCreature()
        )
    }

    // "...and can't attack or block."
    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Marco Gorlei"
        flavorText = "\"Cold be hand and heart and bone,\nand cold be sleep under stone:\nnever more to wake on stony bed,\nnever, till the Sun fails and the Moon is dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb0cda3a-20d7-425a-89fe-b0a3cd7ced03.jpg?1686967795"
    }
}
