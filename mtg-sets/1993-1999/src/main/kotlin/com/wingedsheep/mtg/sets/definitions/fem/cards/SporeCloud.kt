package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spore Cloud
 * {1}{G}{G}
 * Instant
 * Tap all blocking creatures. Prevent all combat damage that would be dealt this turn. Each
 * attacking creature and each blocking creature doesn't untap during its controller's next untap
 * step.
 *
 * A Fog with two riders. The freeze covers attackers *and* blockers, and each is bounded by
 * [Duration.UntilAfterAffectedControllersNextUntap] so it expires against that creature's own
 * controller's untap step — which matters, because the two sides untap on different turns.
 */
val SporeCloud = card("Spore Cloud") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Tap all blocking creatures. Prevent all combat damage that would be dealt this " +
        "turn. Each attacking creature and each blocking creature doesn't untap during its " +
        "controller's next untap step."

    spell {
        effect = Effects.Composite(
            Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Creature.blocking())),
            Effects.PreventAllCombatDamage(),
            Effects.ForEachInGroup(
                filter = GroupFilter(
                    GameObjectFilter.Creature.attacking() or GameObjectFilter.Creature.blocking()
                ),
                effect = GrantKeywordEffect(
                    AbilityFlag.DOESNT_UNTAP.name,
                    EffectTarget.Self,
                    Duration.UntilAfterAffectedControllersNextUntap,
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72a"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1691a9f4-4ea7-440f-9bdc-4214ab3c90f0.jpg?1783947886"
    }
}
