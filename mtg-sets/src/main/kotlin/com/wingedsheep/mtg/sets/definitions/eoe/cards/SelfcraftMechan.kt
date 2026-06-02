package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Selfcraft Mechan
 * {3}{U}
 * Artifact Creature — Robot Artificer
 * 3/4
 * When this creature enters, you may sacrifice an artifact. When you do,
 * put a +1/+1 counter on target creature and draw a card.
 */
val SelfcraftMechan = card("Selfcraft Mechan") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Artificer"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you may sacrifice an artifact. When you do, put a +1/+1 counter on target creature and draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = SacrificeEffect(GameObjectFilter.Artifact),
            optional = true,
            reflexiveEffect = Effects.Composite(
                listOf(
                    Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                    Effects.DrawCards(1)
                )
            ),
            reflexiveTargetRequirements = listOf(Targets.Creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Milivoj Ćeran"
        flavorText = "Sometimes the best parts are already in use."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b2de056-5c27-44d6-871d-909411bd52dd.jpg?1752946852"
        ruling("2025-07-25", "You don't choose a target for Selfcraft Mechan's ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you sacrifice an artifact this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal. Then if the target creature is an illegal target as the reflexive trigger tries to resolve, it won't resolve and you won't draw a card.")
    }
}
