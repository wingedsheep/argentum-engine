package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Golbez, Crystal Collector — Final Fantasy #225
 * {U}{B} · Legendary Creature — Human Wizard · 1/4
 *
 * Whenever an artifact you control enters, surveil 1.
 * At the beginning of your end step, if you control four or more artifacts, return target
 * creature card from your graveyard to your hand. Then if you control eight or more artifacts,
 * each opponent loses life equal to that card's power.
 *
 * First ability: an ANY-bound enters trigger filtered to "artifact you control" → [Effects.Surveil].
 *
 * The end-step ability is gated by an intervening-if (`triggerCondition`, checked both on trigger
 * and on resolution — CR 603.4) requiring four or more artifacts. It returns a captured target
 * creature card to hand (Ruin-Lurker Bat's `triggerCondition` shape + Rakdos Joins Up's captured
 * target handle), then a [ConditionalEffect] runs only when you control eight or more artifacts:
 * each opponent loses life equal to the returned card's power, read via
 * [DynamicAmounts.targetPower] off the same bound target.
 */
val GolbezCrystalCollector = card("Golbez, Crystal Collector") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 4
    oracleText = "Whenever an artifact you control enters, surveil 1.\n" +
        "At the beginning of your end step, if you control four or more artifacts, return target creature " +
        "card from your graveyard to your hand. Then if you control eight or more artifacts, each opponent " +
        "loses life equal to that card's power."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Surveil(1)
        description = "Whenever an artifact you control enters, surveil 1."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouControlAtLeast(4, GameObjectFilter.Artifact.youControl())
        val creatureCard = target(
            "target creature card from your graveyard",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard),
        )
        effect = Effects.Move(creatureCard, Zone.HAND)
            .then(
                ConditionalEffect(
                    condition = Conditions.YouControlAtLeast(8, GameObjectFilter.Artifact.youControl()),
                    effect = Effects.LoseLife(
                        DynamicAmounts.targetPower(0),
                        EffectTarget.PlayerRef(Player.EachOpponent),
                    ),
                ),
            )
        description = "At the beginning of your end step, if you control four or more artifacts, return " +
            "target creature card from your graveyard to your hand. Then if you control eight or more " +
            "artifacts, each opponent loses life equal to that card's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Bachzim"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/849f5716-7211-4e93-a220-f88d49f937f4.jpg?1748706611"
    }
}
