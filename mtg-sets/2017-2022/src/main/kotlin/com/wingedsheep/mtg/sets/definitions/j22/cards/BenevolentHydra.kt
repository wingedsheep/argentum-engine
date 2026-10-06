package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Benevolent Hydra
 * {X}{G}{G}
 * Creature — Hydra
 * 1/1
 *
 * This creature enters with X +1/+1 counters on it.
 * If one or more +1/+1 counters would be put on another creature you control, that many plus one
 * +1/+1 counters are put on it instead.
 * {T}, Remove a +1/+1 counter from this creature: Put a +1/+1 counter on another target creature
 * you control.
 *
 * The second line is Hardened Scales' [ModifyCounterPlacement] with the recipient narrowed to
 * "another" creature — `notSourceItself()` resolves against the Hydra as the replacement's source,
 * so the Hydra's own entry counters and any counters put on it are not modified.
 */
val BenevolentHydra = card("Benevolent Hydra") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 1
    toughness = 1
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "If one or more +1/+1 counters would be put on another creature you control, that many " +
        "plus one +1/+1 counters are put on it instead.\n" +
        "{T}, Remove a +1/+1 counter from this creature: Put a +1/+1 counter on another target " +
        "creature you control."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                recipient = Recipient.Object(
                    GameObjectFilter.Creature.youControl().notSourceItself()
                ),
            ),
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1),
        )
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Put a +1/+1 counter on another target creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c575ed35-6357-4b48-9d20-e4249577142b.jpg?1783919182"
        ruling(
            "2022-12-02",
            "\"Put on another creature you control\" includes creatures other than Benevolent Hydra " +
                "that enter the battlefield with +1/+1 counters on them. If another creature would " +
                "enter the battlefield with a number of +1/+1 counters on it while you control " +
                "Benevolent Hydra, it enters with that many counters plus one."
        )
        ruling(
            "2022-12-02",
            "Each additional Benevolent Hydra you control will increase the number of +1/+1 " +
                "counters placed on another creature by one."
        )
    }
}
