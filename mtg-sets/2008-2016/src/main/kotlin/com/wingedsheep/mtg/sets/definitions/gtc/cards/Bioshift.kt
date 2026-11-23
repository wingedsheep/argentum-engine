package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bioshift
 * {G/U}
 * Instant
 * Move any number of +1/+1 counters from target creature onto another target creature with the
 * same controller.
 *
 * Mutiny's target shape: one requirement of two (`count = 2`, `sameController = true`), so the
 * pair is distinct and shares a controller when chosen; the first is the source, the second the
 * destination. The resolution check only re-tests `differentControllers` as a set, so the
 * "controlled by different players at that time, no counters will move" ruling is gated here:
 * the destination must still be controlled by the source's controller (`TargetController` reads
 * the first chosen target's controller). The number is chosen on resolution (Scrounging Bandar's
 * `ChooseNumberThen` shape), capped at the source's +1/+1 counters.
 */
val Bioshift = card("Bioshift") {
    manaCost = "{G/U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Move any number of +1/+1 counters from target creature onto another target " +
        "creature with the same controller."

    spell {
        val (source, destination) = targets(TargetFilter.Creature, count = 2, sameController = true)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                destination,
                GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
            ),
            then = Effects.ChooseNumberThen(
                maxValue = DynamicAmounts.countersOn(source, CounterType.PLUS_ONE_PLUS_ONE),
                prompt = "Move how many +1/+1 counters?",
                then = Effects.MoveCounters(
                    counterType = CounterType.PLUS_ONE_PLUS_ONE,
                    amount = DynamicAmounts.xValue(),
                    source = source,
                    destination = destination
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "214"
        artist = "Scott Chou"
        flavorText = "\"It's all right if you change your mind. There's always a chance we can " +
            "rearrange parts later.\"\n—Zija, Simic mutationist"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e18f7a9-2af6-467a-8f62-5f7da83a3c92.jpg?1783940096"
        ruling("2013-01-24", "To move a counter from one creature to another, the counter is removed from the first creature and placed on the second. Any abilities that care about a counter being removed or placed on a creature will apply.")
        ruling("2013-01-24", "You decide how many counters to move when Bioshift resolves.")
        ruling("2013-01-24", "If one of the two creatures is an illegal target when Bioshift tries to resolve, or if the creatures are controlled by different players at that time, no counters will move.")
    }
}
