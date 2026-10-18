package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dramatist's Puppet — Kamigawa: Neon Dynasty #244 (canonical printing)
 * {4} · Artifact Creature — Construct · 2/4
 *
 * When this creature enters, for each kind of counter on target permanent, put another counter of
 * that kind on it or remove one from it.
 *
 * The kinds are read off the target as the trigger resolves; each kind is a separate add-or-remove
 * choice whose prompt names the kind ({counter}).
 */
val DramatistsPuppet = card("Dramatist's Puppet") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, for each kind of counter on target permanent, put another " +
        "counter of that kind on it or remove one from it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.ForEachCounterKindOn(
            permanent,
            Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        "Put another {counter} counter on it",
                        Effects.AddCountersOfChosenKind(permanent, kindFrom = CounterKindSource.ITERATION)
                    ),
                    EffectChoice(
                        "Remove a {counter} counter from it",
                        Effects.RemoveCountersOfIteratedKind(permanent)
                    )
                )
            )
        )
        description = "When this creature enters, for each kind of counter on target permanent, put another " +
            "counter of that kind on it or remove one from it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "244"
        artist = "Livia Prima"
        flavorText = "\"Who cares if the story's true as long as people are entertained?\"\n" +
            "—Shin, Towashi street performer"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b490e065-b54a-4015-9768-d0e62f0e2e84.jpg?1783923826"
    }
}
