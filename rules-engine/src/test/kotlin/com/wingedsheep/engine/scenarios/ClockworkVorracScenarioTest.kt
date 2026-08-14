package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Clockwork Vorrac (MRD #156).
 *
 * {5} Artifact Creature — Boar Beast 0/0
 * "Trample
 *  This creature enters with four +1/+1 counters on it.
 *  Whenever this creature attacks or blocks, remove a +1/+1 counter from it at end of combat.
 *  {T}: Put a +1/+1 counter on this creature."
 *
 * What sets it apart from its set-mate Clockwork Beetle is the refill ability: a bare {T} with no
 * cap and no timing restriction, so the counters can be pushed *past* the four it enters with.
 */
class ClockworkVorracScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private val addCounterAbilityId by lazy {
        cardRegistry.requireCard("Clockwork Vorrac").activatedAbilities[0].id
    }

    init {
        fun plusOnePlusOne(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Clockwork Vorrac") {

            test("enters with four +1/+1 counters and is a 4/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Clockwork Vorrac")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clockwork Vorrac").error shouldBe null
                game.resolveStack()

                val vorrac = game.findPermanent("Clockwork Vorrac")!!
                withClue("Enters with four +1/+1 counters") {
                    plusOnePlusOne(game, vorrac) shouldBe 4
                }
                withClue("Base 0/0 plus four +1/+1 counters projects as a 4/4") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(vorrac) shouldBe 4
                    projected.getToughness(vorrac) shouldBe 4
                }
            }

            test("sheds a counter at end of combat after attacking") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Vorrac")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val vorrac = game.findPermanent("Clockwork Vorrac")!!
                game.state = game.state.updateEntity(vorrac) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Clockwork Vorrac" to 2)).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("One +1/+1 counter shed at end of combat, leaving a 3/3") {
                    plusOnePlusOne(game, vorrac) shouldBe 3
                    val projected = stateProjector.project(game.state)
                    projected.getPower(vorrac) shouldBe 3
                }
            }

            test("does not shed a counter if it neither attacked nor blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Vorrac")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val vorrac = game.findPermanent("Clockwork Vorrac")!!
                game.state = game.state.updateEntity(vorrac) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("No combat participation → the intervening-if fails and nothing is removed") {
                    plusOnePlusOne(game, vorrac) shouldBe 4
                }
            }

            test("{T} refills a counter, and is not capped at the four it entered with") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Vorrac")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vorrac = game.findPermanent("Clockwork Vorrac")!!
                game.state = game.state.updateEntity(vorrac) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = vorrac,
                        abilityId = addCounterAbilityId
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Unlike Clockwork Avian there is no four-counter ceiling") {
                    plusOnePlusOne(game, vorrac) shouldBe 5
                    val projected = stateProjector.project(game.state)
                    projected.getPower(vorrac) shouldBe 5
                    projected.getToughness(vorrac) shouldBe 5
                }
            }

            test("shedding its last counter kills it — a 0/0 dies to state-based actions") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Vorrac")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val vorrac = game.findPermanent("Clockwork Vorrac")!!
                game.state = game.state.updateEntity(vorrac) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Clockwork Vorrac" to 2)).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("The last counter is gone, leaving a 0/0 that dies immediately") {
                    game.isOnBattlefield("Clockwork Vorrac") shouldBe false
                    game.isInGraveyard(1, "Clockwork Vorrac") shouldBe true
                }
            }
        }
    }
}
