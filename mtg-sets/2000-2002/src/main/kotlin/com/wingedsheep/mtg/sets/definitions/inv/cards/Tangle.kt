package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tangle
 * {1}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn.
 * Each attacking creature doesn't untap during its controller's next untap step.
 */
val Tangle = card("Tangle") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn.\n" +
        "Each attacking creature doesn't untap during its controller's next untap step."

    spell {
        effect = Effects.PreventAllCombatDamage() then
            Effects.ForEachInGroup(
                GroupFilter.AttackingCreatures,
                Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    EffectTarget.IterationEntity,
                    Duration.UntilAfterAffectedControllersNextUntap()
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b37e39c-8aa4-4938-a492-7dac5de98dfb.jpg?1562916551"
    }
}
