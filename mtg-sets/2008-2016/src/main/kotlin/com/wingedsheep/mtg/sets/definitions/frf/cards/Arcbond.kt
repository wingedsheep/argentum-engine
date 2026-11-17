package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arcbond — Fate Reforged #91
 * {2}{R} · Instant
 * Choose target creature. Whenever that creature is dealt damage this turn, it deals that much
 * damage to each other creature and each player.
 *
 * An entity-scoped delayed trigger: `Triggers.self.isDealtDamage()` with `watchedTarget` set to the
 * chosen creature, expiring at end of turn. The watched creature is the triggering entity, so it is
 * the `damageSource` ("it deals"), "each other creature" excludes it, and "that much" is the trigger's
 * damage amount (simultaneous damage folds into one trigger). A creature killed by the damage still
 * deals it, by last-known information.
 */
val Arcbond = card("Arcbond") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose target creature. Whenever that creature is dealt damage this turn, it deals " +
        "that much damage to each other creature and each player."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.self.isDealtDamage(),
            watchedTarget = creature,
            effect = Effects.ForEachInGroup(
                GroupFilter.AllCreatures.otherThanTriggeringEntity(),
                Effects.DealDamage(
                    amount = DynamicAmounts.triggerDamageAmount(),
                    target = EffectTarget.IterationEntity,
                    damageSource = EffectTarget.TriggeringEntity
                )
            ) then Effects.ForEachPlayer(
                Player.Each,
                Effects.DealDamage(
                    amount = DynamicAmounts.triggerDamageAmount(),
                    target = EffectTarget.Controller,
                    damageSource = EffectTarget.TriggeringEntity
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Slawomir Maniak"
        flavorText = "\"If you must die today, make your death worthy of legend.\"\n—Alesha, Who Smiles at Death"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bc397d1-50a8-46cd-98b2-7104f2241420.jpg?1783938692"
        ruling("2014-11-24", "The creature is the source of the new damage.")
        ruling("2014-11-24", "The delayed triggered ability Arcbond creates will trigger even if the target " +
            "creature is dealt lethal damage. For example, if a 3/3 creature is targeted by Arcbond and later " +
            "in the turn it blocks a 7/7 creature, the 3/3 creature will deal 7 damage each other creature " +
            "and each player.")
        ruling("2014-11-24", "Damage dealt by the creature because of the ability Arcbond creates isn't " +
            "combat damage, even if it was combat damage that caused the ability to trigger.")
    }
}
