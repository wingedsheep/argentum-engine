package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.tla.cards.WarBalloon
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for War Balloon (TLA #159).
 *
 * {2}{R} 4/3 Artifact — Vehicle.
 * Flying
 * {1}: Put a fire counter on this Vehicle.
 * As long as this Vehicle has three or more fire counters on it, it's an artifact creature.
 * Crew 3
 */
class WarBalloonScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(WarBalloon)

        context("War Balloon") {

            test("{1} activated ability adds a fire counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "War Balloon")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balloon = game.findPermanent("War Balloon")!!
                val abilityId = cardRegistry.getCard("War Balloon")!!.script.activatedAbilities[0].id

                withClue("starts with no fire counters") {
                    val startingFire = game.state.getEntity(balloon)?.get<CountersComponent>()
                        ?.getCount(CounterType.FIRE) ?: 0
                    startingFire shouldBe 0
                }

                game.execute(ActivateAbility(game.player1Id, balloon, abilityId)).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("activating {1} puts one fire counter on it") {
                    game.state.getEntity(balloon)?.get<CountersComponent>()
                        ?.getCount(CounterType.FIRE) shouldBe 1
                }
            }

            test("not a creature below three fire counters, an artifact creature at three or more") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "War Balloon", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balloon = game.findPermanent("War Balloon")!!

                // Seed two fire counters: below the 3+ threshold, so it stays a noncreature artifact.
                game.state = game.state.updateEntity(balloon) {
                    it.with(CountersComponent(mapOf(CounterType.FIRE to 2)))
                }
                withClue("below threshold: not a creature") {
                    game.state.projectedState.isCreature(balloon) shouldBe false
                }
                withClue("below threshold: can't attack on its own (not a creature)") {
                    game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    game.declareAttackers(mapOf("War Balloon" to 2)).error shouldNotBe null
                    game.state.getEntity(balloon)
                        ?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() shouldBe false
                }
            }

            test("at three fire counters it becomes an artifact creature and can attack on its own") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "War Balloon", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balloon = game.findPermanent("War Balloon")!!

                game.state = game.state.updateEntity(balloon) {
                    it.with(CountersComponent(mapOf(CounterType.FIRE to 3)))
                }

                val projected = game.state.projectedState
                withClue("at 3+ fire counters it's an artifact creature with its printed 4/3") {
                    projected.isCreature(balloon) shouldBe true
                    projected.hasType(balloon, "ARTIFACT") shouldBe true
                    projected.getPower(balloon) shouldBe 4
                    projected.getToughness(balloon) shouldBe 3
                }

                withClue("at 3+ it can attack without being crewed") {
                    game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    game.declareAttackers(mapOf("War Balloon" to 2)).error shouldBe null
                    game.state.getEntity(balloon)
                        ?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() shouldBe true
                }
            }
        }
    }
}
