package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Compulsory Rest
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 * Enchanted creature has "{2}, Sacrifice this creature: You gain 2 life."
 *
 * The granted ability belongs to the enchanted creature, so its controller (not necessarily the
 * Aura's) may activate it, sacrifices the creature itself, and gains the life.
 */
val CompulsoryRest = card("Compulsory Rest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "Enchanted creature has \"{2}, Sacrifice this creature: You gain 2 life.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf),
                effect = Effects.GainLife(2),
                descriptionOverride = "{2}, Sacrifice this creature: You gain 2 life."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Eric Deschamps"
        flavorText = "The pain of death is nothing compared to the pain of failure."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11cb3a5b-beee-4f53-a6b5-e9b5603546fa.jpg"
    }
}
