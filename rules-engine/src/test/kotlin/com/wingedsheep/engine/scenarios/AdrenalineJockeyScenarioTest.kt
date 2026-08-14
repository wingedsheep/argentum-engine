package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario coverage for Adrenaline Jockey's exhaust-activation trigger. */
class AdrenalineJockeyScenarioTest : ScenarioTestBase() {
    private val ordinaryActivator = card("Ordinary Activator") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        }
    }

    init {
        cardRegistry.register(ordinaryActivator)

        test("activating an exhaust ability puts a counter on Adrenaline Jockey") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Adrenaline Jockey")
                .withCardOnBattlefield(1, "Prowcatcher Specialist", summoningSickness = false)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jockey = game.findPermanent("Adrenaline Jockey")!!
            val specialist = game.findPermanent("Prowcatcher Specialist")!!
            val exhaust = cardRegistry.getCard("Prowcatcher Specialist")!!
                .script.activatedAbilities.single { it.isExhaust }

            val activation = game.execute(ActivateAbility(game.player1Id, specialist, exhaust.id))
            withClue("the exhaust activation should be legal: ${activation.error}") {
                activation.error shouldBe null
            }
            game.resolveStack()

            val counters = game.state.getEntity(jockey)!!.get<CountersComponent>()!!.counters
            counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
        }

        test("activating an ordinary ability does not trigger Adrenaline Jockey") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Adrenaline Jockey")
                .withCardOnBattlefield(1, "Ordinary Activator", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jockey = game.findPermanent("Adrenaline Jockey")!!
            val activator = game.findPermanent("Ordinary Activator")!!
            val ability = cardRegistry.getCard("Ordinary Activator")!!.script.activatedAbilities.single()

            game.execute(ActivateAbility(game.player1Id, activator, ability.id)).error shouldBe null
            game.resolveStack()

            val counters = game.state.getEntity(jockey)!!.get<CountersComponent>()?.counters.orEmpty()
            counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe null
        }
    }
}
