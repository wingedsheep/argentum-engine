package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clinging Mists
 * {2}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn.
 * Fateful hour — If you have 5 or less life, tap all attacking creatures. Those creatures don't
 * untap during their controller's next untap step.
 */
val ClingingMists = card("Clinging Mists") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn.\n" +
        "Fateful hour — If you have 5 or less life, tap all attacking creatures. " +
        "Those creatures don't untap during their controller's next untap step."

    spell {
        val attacking = GroupFilter(GameObjectFilter.Creature.attacking())
        effect = Effects.PreventAllCombatDamage() then
            Effects.If(
                condition = Conditions.LifeAtMost(5),
                then = Patterns.Group.tapAll(attacking) then
                    Effects.ForEachInGroup(
                        filter = attacking,
                        effect = Effects.GrantKeyword(
                            AbilityFlag.DOESNT_UNTAP,
                            EffectTarget.IterationEntity,
                            Duration.UntilAfterAffectedControllersNextUntap(),
                        )
                    )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Anthony Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0152975-790a-40e4-993e-a970676a2d32.jpg?1783940810"
    }
}
