package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Marshals' Pathcruiser (DFT #236).
 *
 * Marshals' Pathcruiser {3} — Artifact — Vehicle 6/5
 * When this Vehicle enters, search your library for a basic land card, reveal it, put it into your
 * hand, then shuffle.
 * Exhaust — {W}{U}{B}{R}{G}: This Vehicle becomes an artifact creature. Put two +1/+1 counters on it.
 * Crew 5
 *
 * The load-bearing claim is the **duration** of the exhaust animate: the card has no "until end of
 * turn" clause, so unlike Crew the Vehicle stays a creature across the turn boundary. Its printed
 * 6/5 is the base P/T, so the two counters make it an 8/7.
 */
class MarshalsPathcruiserScenarioTest : ScenarioTestBase() {

    private val exhaustAbilityId
        get() = cardRegistry.getCard("Marshals' Pathcruiser")!!.script.activatedAbilities[0].id

    init {
        context("Marshals' Pathcruiser") {

            test("the exhaust animate is permanent and survives the end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Marshals' Pathcruiser")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pathcruiser = game.findPermanent("Marshals' Pathcruiser")!!
                withClue("a Vehicle is not a creature until something animates it") {
                    game.state.projectedState.isCreature(pathcruiser) shouldBe false
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pathcruiser,
                        abilityId = exhaustAbilityId
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("6/5 base plus two +1/+1 counters, still an artifact") {
                    projected.isCreature(pathcruiser) shouldBe true
                    projected.hasType(pathcruiser, "ARTIFACT") shouldBe true
                    projected.getPower(pathcruiser) shouldBe 8
                    projected.getToughness(pathcruiser) shouldBe 7
                }
                game.state.getEntity(pathcruiser)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("no 'until end of turn' clause — cleanup must not undo it") {
                    game.state.projectedState.isCreature(pathcruiser) shouldBe true
                }
            }
        }
    }
}
