package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fearless Swashbuckler (DFT #204) — {1}{U}{R} Creature — Fish Pirate 3/3.
 *
 * "Haste
 *  Vehicles you control have haste.
 *  Whenever you attack, if a Pirate and a Vehicle attacked this combat, draw three cards, then
 *  discard two cards."
 *
 * The intervening "if" is modelled as two independent existence checks over `attackedThisCombat()`
 * rather than one conjunctive filter, so the tests separate the two failure modes: attacking with
 * the Pirate alone must *not* trigger, and attacking with a Pirate plus a crewed Vehicle must. The
 * haste grant is what makes the second case reachable on the Vehicle's first turn.
 */
class FearlessSwashbucklerScenarioTest : ScenarioTestBase() {

    init {
        context("Fearless Swashbuckler") {

            test("Vehicles you control have haste") {
                val game = swashbucklerGame()
                val ferry = game.findPermanent("Skybox Ferry")!!

                withClue("the grant reaches a Vehicle even while it is not a creature") {
                    game.state.projectedState.hasKeyword(ferry, Keyword.HASTE) shouldBe true
                }
            }

            test("attacking with the Pirate alone does not trigger — no Vehicle attacked") {
                val game = swashbucklerGame()
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Fearless Swashbuckler" to 2))
                withClue("the attack itself must land, or this proves nothing: ${attack.error}") {
                    attack.error shouldBe null
                    game.state.getEntity(game.findPermanent("Fearless Swashbuckler")!!)!!
                        .has<AttackingComponent>() shouldBe true
                }
                game.resolveStack()

                withClue("the intervening 'if' must fail with no Vehicle among the attackers") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("a Pirate and a crewed Vehicle attacking draws three and discards two") {
                val game = swashbucklerGame()
                val handBefore = game.handSize(1)

                // Crew the Ferry with the Swashbuckler's teammate so both can attack; the
                // Vehicle's haste comes from the Swashbuckler itself.
                val crew = game.execute(
                    CrewVehicle(
                        game.player1Id,
                        game.findPermanent("Skybox Ferry")!!,
                        listOf(game.findPermanent("Hill Giant")!!)
                    )
                )
                withClue("crew should succeed: ${crew.error}") { crew.error shouldBe null }
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Fearless Swashbuckler" to 2, "Skybox Ferry" to 2)
                )
                game.resolveStack()

                if (game.hasPendingDecision()) {
                    // The discard is a choose-2-from-hand selection.
                    game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2))
                    game.resolveStack()
                }

                withClue("draw 3 then discard 2 is a net +1") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }

    /**
     * The Swashbuckler, an ability-free Vehicle to be the attacking Vehicle, and a Hill Giant with
     * enough power to crew it. Turn 3 with the players' libraries stocked so the draw-three can't
     * deck anyone, and starting in the precombat main phase so the crew happens before combat.
     */
    private fun swashbucklerGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Fearless Swashbuckler", summoningSickness = false)
            .withCardOnBattlefield(1, "Skybox Ferry")
            .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        repeat(15) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        return builder
            .withActivePlayer(1)
            .withTurnNumber(3)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
