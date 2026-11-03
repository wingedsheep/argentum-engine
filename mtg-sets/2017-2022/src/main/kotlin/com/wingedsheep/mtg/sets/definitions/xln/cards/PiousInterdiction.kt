package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pious Interdiction
 * {3}{W}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, you gain 2 life.
 * Enchanted creature can't attack or block.
 */
val PiousInterdiction = card("Pious Interdiction") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, you gain 2 life.\n" +
        "Enchanted creature can't attack or block."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(2)
    }

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Lake Hurwitz"
        flavorText = "\"Ours is the true and righteous path. You will bow before our might.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/670b9645-ce34-41eb-a527-4af3e13c3a54.jpg?1783935795"
    }
}
