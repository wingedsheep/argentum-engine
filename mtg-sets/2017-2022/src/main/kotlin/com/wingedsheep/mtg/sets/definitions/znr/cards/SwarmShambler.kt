package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swarm Shambler — Zendikar Rising #207
 * {G} · Creature — Fungus Beast · 0/0
 *
 * This creature enters with a +1/+1 counter on it.
 * Whenever a creature you control with a +1/+1 counter on it becomes the target of a spell an
 * opponent controls, create a 1/1 green Insect creature token.
 * {1}, {T}: Put a +1/+1 counter on this creature.
 *
 * The counter filter is read when the targeting happens, so a counter put on the creature after
 * it was targeted doesn't retroactively trigger the ability (ruling). One spell targeting the
 * creature several times emits one becomes-target event, so it triggers once.
 */
val SwarmShambler = card("Swarm Shambler") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus Beast"
    power = 0
    toughness = 0
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "Whenever a creature you control with a +1/+1 counter on it becomes the target of a spell an " +
        "opponent controls, create a 1/1 green Insect creature token.\n" +
        "{1}, {T}: Put a +1/+1 counter on this creature."

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()
        ).becomesTarget(byOpponent = true, spellsOnly = true)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect")
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a7e4f99-ece4-473e-b712-40e4c53558e8.jpg?1783929330"
        ruling("2020-09-25", "If a spell targets a creature you control with a +1/+1 counter on it more than once, Swarm Shambler’s ability triggers only once.")
        ruling("2020-09-25", "You create just one Insect token, no matter how many +1/+1 counters the target creature has on it.")
        ruling("2020-09-25", "An ability that triggers when a creature becomes the target of a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-09-25", "If a creature without a +1/+1 counter on it becomes the target of a spell an opponent controls, putting a +1/+1 counter on it after that won’t cause Swarm Shambler’s middle ability to trigger.")
    }
}
