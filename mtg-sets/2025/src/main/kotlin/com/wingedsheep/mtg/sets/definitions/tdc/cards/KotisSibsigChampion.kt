package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kotis, Sibsig Champion
 * {B}{G}{U}
 * Legendary Creature — Zombie Warrior
 * 3/3
 * Once during each of your turns, you may cast a creature spell from your graveyard by exiling
 * three other cards from your graveyard in addition to paying its other costs.
 * Whenever one or more creatures you control enter, if one or more of them entered from a
 * graveyard or was cast from a graveyard, put two +1/+1 counters on Kotis.
 *
 * The graveyard cast is a once-per-turn [MayCastFromGraveyard] grant with an exile-three-others
 * additional cost; the counters are a batch-enters trigger gated by the
 * `AnyEnteredOrWasCastFromGraveyard` intervening-"if".
 */
val KotisSibsigChampion = card("Kotis, Sibsig Champion") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Zombie Warrior"
    power = 3
    toughness = 3
    oracleText = "Once during each of your turns, you may cast a creature spell from your graveyard " +
        "by exiling three other cards from your graveyard in addition to paying its other costs.\n" +
        "Whenever one or more creatures you control enter, if one or more of them entered from a " +
        "graveyard or was cast from a graveyard, put two +1/+1 counters on Kotis."

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Creature,
            duringYourTurnOnly = true,
            oncePerTurn = true,
            additionalCost = Costs.additional.ExileOtherCards(3),
        )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.youControl()).enter()
        interveningIf = Conditions.AnyEnteredOrWasCastFromGraveyard
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Whenever one or more creatures you control enter, if one or more of them " +
            "entered from a graveyard or was cast from a graveyard, put two +1/+1 counters on Kotis."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9ea2e9c-c1c7-4870-877a-023118909f56.jpg?1783907189"
    }
}
