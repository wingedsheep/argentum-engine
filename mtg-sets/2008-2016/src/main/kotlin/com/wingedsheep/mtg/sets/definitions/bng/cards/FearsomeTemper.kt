package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fearsome Temper — Born of the Gods #95 (canonical printing)
 * {2}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 and has "{2}{R}: Target creature can't block this creature this turn."
 *
 * The granted ability's source is the enchanted creature, so `attacker = EffectTarget.Self` names it —
 * a pairwise restriction (the target may still block other attackers), as on Kozilek's Pathfinder.
 */
val FearsomeTemper = card("Fearsome Temper") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 and has \"{2}{R}: Target creature can't block this creature this turn.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{2}{R}")
                val t = target(TargetFilter.Creature)
                effect = Effects.CantBlock(target = t, attacker = EffectTarget.Self)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Seb McKinnon"
        flavorText = "Purphoros is god of creation and destruction, which makes him the most creatively destructive god in the pantheon."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6e7884c-2d66-42d1-87d1-1341a6fa52d8.jpg?1783939546"
    }
}
