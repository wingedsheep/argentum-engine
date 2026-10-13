package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jhoira, Ageless Innovator
 * {U}{R}
 * Legendary Creature — Human Artificer
 * 2/3
 * {T}: Put two ingenuity counters on Jhoira, then you may put an artifact card with mana value X or
 * less from your hand onto the battlefield, where X is the number of ingenuity counters on Jhoira.
 *
 * Counters first, then the gather → filter (mana value ≤ `countersOnSelf(INGENUITY)`) →
 * choose-up-to-one → move-to-battlefield pipeline (Lady Octopus's shape, ending in a put rather than
 * a cast). X is read after the counters land, so a fresh Jhoira's first activation allows mana
 * value 2 or less.
 *
 * Per the ruling, if Jhoira has left the battlefield before the ability resolves, no counters are
 * placed (the `Self` reference names nothing, and the pipeline carries on past that step) and X is
 * the count she last had on the battlefield — `EffectTarget.Self` is a last-known-information
 * reference, so the counter read falls back to her battlefield-exit snapshot.
 */
val JhoiraAgelessInnovator = card("Jhoira, Ageless Innovator") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "{T}: Put two ingenuity counters on Jhoira, then you may put an artifact card with " +
        "mana value X or less from your hand onto the battlefield, where X is the number of " +
        "ingenuity counters on Jhoira."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            run(Effects.AddCounters(CounterType.INGENUITY, 2, EffectTarget.Self))
            val jhoiraArtifacts = gather(
                CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Artifact)
            )
            val jhoiraEligible = filter(
                jhoiraArtifacts,
                GameObjectFilter.Any.manaValueAtMostDynamic(
                    DynamicAmounts.countersOnSelf(CounterType.INGENUITY),
                )
            )
            val jhoiraChosen = chooseUpTo(
                1,
                from = jhoiraEligible,
                selectedLabel = "Put onto the battlefield"
            )
            move(jhoiraChosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        description = "Put two ingenuity counters on Jhoira, then you may put an artifact card with " +
            "mana value X or less from your hand onto the battlefield, where X is the number of " +
            "ingenuity counters on Jhoira."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd911900-1f5f-4420-96b8-1e4fe67e59f6.jpg?1783921284"
        ruling(
            "2022-09-09",
            "If Jhoira, Ageless Innovator isn't on the battlefield as the activated ability resolves, " +
                "you won't be able to put any ingenuity counters on it. However, you can still put an " +
                "artifact card from your hand onto the battlefield. Use the number of ingenuity " +
                "counters that were on Jhoira before it left the battlefield to determine the value of X."
        )
    }
}
