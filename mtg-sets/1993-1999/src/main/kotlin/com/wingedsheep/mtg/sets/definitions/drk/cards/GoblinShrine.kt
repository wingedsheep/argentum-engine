package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Shrine
 * {1}{R}{R}
 * Enchantment — Aura
 * Enchant land
 * As long as enchanted land is a basic Mountain, Goblin creatures get +1/+0.
 * When this Aura leaves the battlefield, it deals 1 damage to each Goblin creature.
 *
 * Goblin Caves' aggressive twin, and the two share the same conditional-lord shape: the anthem is
 * live on the *enchanted land* still being a basic Mountain, not a fact checked once when the Aura
 * enters.
 *
 * The parting shot is a real leaves-the-battlefield trigger, so it fires whatever took the Shrine
 * off the battlefield — the land dying under it, disenchantment, or a bounce. It hits every Goblin,
 * including the ones it was buffing, which is the point: a Goblin the Shrine kept at 1 toughness
 * dies to its own patron's departure.
 */
val GoblinShrine = card("Goblin Shrine") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "As long as enchanted land is a basic Mountain, Goblin creatures get +1/+0.\n" +
        "When this Aura leaves the battlefield, it deals 1 damage to each Goblin creature."
    auraTarget = Targets.Land

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 0,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)),
            ),
            condition = Conditions.EnchantedPermanentMatches(
                GameObjectFilter.BasicLand.withSubtype(Subtype.MOUNTAIN)
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)),
            Effects.DealDamage(1, EffectTarget.Self),
        )
        description = "When this Aura leaves the battlefield, it deals 1 damage to each Goblin creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Ron Spencer"
        flavorText = "\"I knew it weren't no ordinary pile of—you know.\" —Norin the Wary"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd69a6dc-27f3-42aa-9e63-4417796e4ef5.jpg?1783947934"
    }
}
