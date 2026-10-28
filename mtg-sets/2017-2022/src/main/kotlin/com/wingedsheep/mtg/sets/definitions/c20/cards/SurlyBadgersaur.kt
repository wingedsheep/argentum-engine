package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Surly Badgersaur
 * {3}{R}
 * Creature — Badger Dinosaur
 * 3/3
 * Whenever you discard a creature card, put a +1/+1 counter on this creature.
 * Whenever you discard a land card, create a Treasure token.
 * Whenever you discard a noncreature, nonland card, this creature fights up to one target creature
 * you don't control.
 *
 * Three typed discard triggers (Waste Not's shape, from your own side): each fires once per
 * matching discarded card.
 */
val SurlyBadgersaur = card("Surly Badgersaur") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Badger Dinosaur"
    oracleText = "Whenever you discard a creature card, put a +1/+1 counter on this creature.\n" +
        "Whenever you discard a land card, create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Whenever you discard a noncreature, nonland card, this creature fights up to one target " +
        "creature you don't control."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you discard a creature card, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Land)
        effect = Effects.CreateTreasure(1)
        description = "Whenever you discard a land card, create a Treasure token."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
        val foe = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.Fight(EffectTarget.Self, foe)
        description = "Whenever you discard a noncreature, nonland card, this creature fights up to one target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7ba507e2-fe5e-4947-aada-20184aa09700.jpg?1783931211"
        ruling("2020-04-17", "Surly Badgersaur's abilities are triggered abilities, not activated abilities. They don't allow you to discard a card whenever you want; rather, you need some other way of discarding a card, such as a cycling ability.")
        ruling("2020-04-17", "If you discard a card as a cost to cast a spell or activate an ability, Surly Badgersaur's triggered ability resolves before that spell or ability but after you've chosen targets for it. If you discard a card while a spell or ability is resolving, that spell or ability finishes resolving before Surly Badgersaur's triggered ability does.")
        ruling("2020-04-17", "If the target creature is an illegal target when Surly Badgersaur's last ability tries to resolve, the ability doesn't resolve. If it's a legal target but Surly Badgersaur is no longer on the battlefield when the ability resolves, the target creature won't deal or be dealt damage.")
    }
}
