package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nezahal, Primal Tide
 * {5}{U}{U}
 * Legendary Creature — Elder Dinosaur
 * 7/7
 * This spell can't be countered.
 * You have no maximum hand size.
 * Whenever an opponent casts a noncreature spell, draw a card.
 * Discard three cards: Exile Nezahal. Return it to the battlefield tapped under its owner's
 * control at the beginning of the next end step.
 */
val NezahalPrimalTide = card("Nezahal, Primal Tide") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elder Dinosaur"
    power = 7
    toughness = 7
    oracleText = "This spell can't be countered.\n" +
        "You have no maximum hand size.\n" +
        "Whenever an opponent casts a noncreature spell, draw a card.\n" +
        "Discard three cards: Exile Nezahal. Return it to the battlefield tapped under its owner's " +
        "control at the beginning of the next end step."

    cantBeCountered = true

    staticAbility {
        ability = NoMaximumHandSize()
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Discard(count = 3)
        effect = Effects.Move(EffectTarget.Self, Zone.EXILE) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true)
            )
        description = "Discard three cards: Exile Nezahal. Return it to the battlefield tapped under its " +
            "owner's control at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48eba418-94ab-46a3-958c-4d5058fc2bcd.jpg?1783935322"
        ruling(
            "2018-01-19",
            "Nezahal's triggered ability resolves before the noncreature spell that caused it to trigger."
        )
        ruling(
            "2018-01-19",
            "After Nezahal returns to the battlefield, it will be a new object with no connection to the creature that was exiled. It won't be in combat or have any additional abilities it may have had when it was exiled. Any +1/+1 counters on it or Auras attached to it are removed, and any Equipment will no longer be attached."
        )
    }
}
