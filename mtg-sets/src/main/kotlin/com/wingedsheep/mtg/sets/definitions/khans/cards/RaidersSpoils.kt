package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raiders' Spoils
 * {3}{B}
 * Enchantment
 * Creatures you control get +1/+0.
 * Whenever a Warrior you control deals combat damage to a player, you may pay 1 life.
 * If you do, draw a card.
 */
val RaidersSpoils = card("Raiders' Spoils") {
    manaCost = "{3}{B}"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+0.\nWhenever a Warrior you control deals combat damage to a player, you may pay 1 life. If you do, draw a card."

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    staticAbility {
        ability = GrantTriggeredAbilityToCreatureGroup(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = MayEffect(
                    effect = CompositeEffect(listOf(
                        Effects.LoseLife(1, EffectTarget.Controller),
                        Effects.DrawCards(1)
                    ))
                )
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Warrior").youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Wayne Reynolds"
        flavorText = "To conquer is to eat. —Edicts of Ilagra"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd3e423e-cb62-43e1-9d0c-e702f823c8e1.jpg?1562793691"
    }
}
