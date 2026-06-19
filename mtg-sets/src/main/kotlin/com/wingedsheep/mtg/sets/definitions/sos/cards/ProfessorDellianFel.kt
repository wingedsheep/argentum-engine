package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Professor Dellian Fel
 * {2}{B}{G}
 * Legendary Planeswalker — Dellian
 * Starting Loyalty: 5
 *
 * +2: You gain 3 life.
 * 0: You draw a card and lose 1 life.
 * −3: Destroy target creature.
 * −6: You get an emblem with "Whenever you gain life, target opponent loses that much life."
 *
 * The ultimate emblem grants a global triggered ability that fires on the controller's own
 * life-gain events; its targeted life-loss reads the life-gained amount from the trigger
 * context (TRIGGER_LIFE_GAINED) and drains it from the chosen opponent.
 */
val ProfessorDellianFel = card("Professor Dellian Fel") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Dellian"
    startingLoyalty = 5
    oracleText = "+2: You gain 3 life.\n" +
        "0: You draw a card and lose 1 life.\n" +
        "−3: Destroy target creature.\n" +
        "−6: You get an emblem with \"Whenever you gain life, target opponent loses that much life.\""

    // +2: You gain 3 life.
    loyaltyAbility(+2) {
        effect = Effects.GainLife(3)
    }

    // 0: You draw a card and lose 1 life.
    loyaltyAbility(0) {
        effect = Effects.DrawCards(1).then(
            Effects.LoseLife(1, EffectTarget.Controller)
        )
    }

    // −3: Destroy target creature.
    loyaltyAbility(-3) {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Destroy(creature)
    }

    // −6: Emblem with "Whenever you gain life, target opponent loses that much life."
    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.YouGainLife.event,
                binding = Triggers.YouGainLife.binding,
                targetRequirement = Targets.Opponent,
                effect = Effects.LoseLife(
                    amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_LIFE_GAINED),
                    target = EffectTarget.ContextTarget(0)
                )
            ),
            descriptionOverride = "Whenever you gain life, target opponent loses that much life."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "214"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6ff3b4d8-1271-4c5d-8834-7662244f173d.jpg?1775938486"
    }
}
