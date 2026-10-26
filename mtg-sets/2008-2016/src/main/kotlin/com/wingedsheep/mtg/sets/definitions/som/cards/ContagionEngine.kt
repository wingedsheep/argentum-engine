package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Contagion Engine
 * {6}
 * Artifact
 * When this artifact enters, put a -1/-1 counter on each creature target player controls.
 * {4}, {T}: Proliferate twice.
 *
 * The enter trigger targets a player and puts a counter on each creature that player controls as
 * it resolves. "Proliferate twice" is two complete proliferate actions in sequence, so the second
 * choice can differ from the first and nobody can respond between them (2011-01-01 rulings).
 */
val ContagionEngine = card("Contagion Engine") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, put a -1/-1 counter on each creature target player controls.\n" +
        "{4}, {T}: Proliferate twice. (Choose any number of permanents and/or players, then give each another " +
        "counter of each kind already there. Then do it again.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
            Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Proliferate() then Effects.Proliferate()
        description = "{4}, {T}: Proliferate twice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dce72636-08e4-484e-ad81-4d1597a31ffb.jpg?1783941712"
        ruling("2011-01-01", "Players can't respond between the first and the second proliferate actions.")
        ruling(
            "2011-01-01",
            "As Contagion Engine's activated ability resolves, you'll complete an entire proliferate action, " +
                "then you'll complete a second proliferate action. You may choose different players and/or " +
                "permanents, or different counters on those permanents, when you proliferate the second time."
        )
    }
}
