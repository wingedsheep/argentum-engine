package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wicked Wolf
 * {2}{G}{G}
 * Creature — Wolf
 * 3/3
 * When this creature enters, it fights up to one target creature you don't control.
 * Sacrifice a Food: Put a +1/+1 counter on this creature. It gains indestructible until end of
 * turn. Tap it.
 *
 * The enters trigger is [com.wingedsheep.mtg.sets.definitions.woe.cards.AgathasChampion]'s fight
 * without the bargain gate: `optional = true` is the "up to one". The Food ability has no tap
 * symbol in its cost, so it can be activated while the Wolf is already tapped (and in response to
 * its own enters trigger), per the 2019-10-04 rulings; "Tap it" is part of the effect.
 */
val WickedWolf = card("Wicked Wolf") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, it fights up to one target creature you don't control.\n" +
        "Sacrifice a Food: Put a +1/+1 counter on this creature. It gains indestructible until end " +
        "of turn. Tap it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val foe = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.Fight(EffectTarget.Self, foe)
        description = "When this creature enters, it fights up to one target creature you don't control."
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
        description = "Put a +1/+1 counter on this creature. It gains indestructible until end of turn. Tap it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09476eac-55d2-4955-8951-ae4ce117c98b.jpg?1783932601"

        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
        ruling(
            "2019-10-04",
            "If the target creature is an illegal target when Wicked Wolf's first ability tries to " +
                "resolve, the ability doesn't resolve. If Wicked Wolf is no longer on the battlefield, " +
                "the target creature won't deal or be dealt damage."
        )
        ruling(
            "2019-10-04",
            "You can activate Wicked Wolf's last ability any number of times while its first ability " +
                "is on the stack."
        )
        ruling("2019-10-04", "You can activate Wicked Wolf's last ability even if it's already tapped.")
    }
}
