package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Alesha, Who Laughs at Fate
 * {1}{B}{R}
 * Legendary Creature — Human Warrior
 * 2/2
 *
 * First strike
 * Whenever Alesha attacks, put a +1/+1 counter on it.
 * Raid — At the beginning of your end step, if you attacked this turn, return target creature
 * card with mana value less than or equal to Alesha's power from your graveyard to the
 * battlefield.
 *
 * Raid is the intervening-if end-step trigger ([Conditions.YouAttackedThisTurn], CR 603.4) —
 * the condition is checked both when the trigger would fire and again on resolution. The
 * mana-value cap is *live*: the target is a graveyard [TargetObject] filtered by
 * [CardPredicate.ManaValueAtMostDynamic] over [DynamicAmounts.sourcePower], so it re-reads
 * Alesha's current power (including the +1/+1 counters her attack trigger stacks up) at target
 * selection and again at resolution — matching the ruling that the ability does nothing if
 * Alesha's power drops below the target's mana value before it resolves.
 */
val AleshaWhoLaughsAtFate = card("Alesha, Who Laughs at Fate") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Whenever Alesha attacks, put a +1/+1 counter on it.\n" +
        "Raid — At the beginning of your end step, if you attacked this turn, return target " +
        "creature card with mana value less than or equal to Alesha's power from your graveyard " +
        "to the battlefield."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever Alesha attacks, put a +1/+1 counter on it."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouAttackedThisTurn
        val t = target(
            "target creature card with mana value less than or equal to Alesha's power in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.IsCreature,
                            CardPredicate.ManaValueAtMostDynamic(DynamicAmounts.sourcePower())
                        ),
                        controllerPredicate = ControllerPredicate.OwnedByYou
                    ),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.PutOntoBattlefield(t)
        description = "Raid — At the beginning of your end step, if you attacked this turn, return " +
            "target creature card with mana value less than or equal to Alesha's power from your " +
            "graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a93e3406-4e29-4bc0-ae52-cbd2ac1f99a4.jpg?1783909093"
        ruling(
            "2024-11-08",
            "If the mana cost of a creature card in your graveyard includes {X}, X is 0 for the " +
                "purpose of determining its mana value.",
        )
        ruling(
            "2024-11-08",
            "If another effect causes Alesha's power to be less than the mana value of the target " +
                "card as its last ability tries to resolve, it won't resolve and none of its effects " +
                "will happen.",
        )
        ruling(
            "2024-11-08",
            "Raid abilities care only that you attacked with a creature. It doesn't matter how many " +
                "creatures you attacked with or which player, planeswalker, or battle those creatures " +
                "attacked.",
        )
        ruling(
            "2024-11-08",
            "Some raid abilities trigger at the beginning of your end step. These abilities trigger " +
                "if you attacked with a creature that turn, even if the permanent with that raid " +
                "ability wasn't on the battlefield when you attacked.",
        )
    }
}
