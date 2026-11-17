package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Elite Scaleguard (Fate Reforged #12) — {4}{W} Creature — Human Soldier, 2/3.
 *
 *   When this creature enters, bolster 2.
 *   Whenever a creature you control with a +1/+1 counter on it attacks, tap target creature
 *   defending player controls.
 *
 * Covers the bolster ETB, the counter-filtered attack trigger firing for *another* attacker and
 * binding its target to that attacker's defending player, and the trigger staying silent for an
 * attacker without a +1/+1 counter.
 */
class EliteScaleguardScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun seedCounters(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Elite Scaleguard") {

            test("ETB bolsters 2 onto the creature with the least toughness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Elite Scaleguard")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Elite Scaleguard")
                withClue("Casting Elite Scaleguard should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                val scaleguard = game.findPermanent("Elite Scaleguard")!!
                withClue("Grizzly Bears (toughness 2) gets the two +1/+1 counters") {
                    plusOneCounters(game, bears) shouldBe 2
                }
                withClue("Elite Scaleguard (toughness 3) gets none") {
                    plusOneCounters(game, scaleguard) shouldBe 0
                }
            }

            test("another creature you control with a +1/+1 counter attacks: tap a defending player's creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elite Scaleguard", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                seedCounters(game, bears, 1)

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("the attack trigger must announce a target creature the defending player controls") {
                    val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                    decision.legalTargets[0] shouldBe listOf(giant)
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is tapped before blockers are declared") {
                    isTapped(game, giant) shouldBe true
                }
            }

            test("attackers without a +1/+1 counter don't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elite Scaleguard", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Elite Scaleguard" to 2)).error shouldBe null

                withClue("no counter-bearing attacker, so no trigger and no target prompt") {
                    game.state.pendingDecision shouldBe null
                    game.state.stack.isEmpty() shouldBe true
                }
                withClue("Hill Giant stays untapped") {
                    isTapped(game, giant) shouldBe false
                }
            }
        }
    }
}
