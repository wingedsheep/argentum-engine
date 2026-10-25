package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Meteor Swarm (AFR #155, {X}{R}{R}{R}, Sorcery).
 *
 *   Meteor Swarm deals 8 damage divided as you choose among X target creatures and/or planeswalkers.
 *
 * A fixed total over an exact, X-driven target count: X names the number of targets, the 8 damage is
 * divided among them as the spell is cast, and each must take at least 1 — so X can't exceed 8.
 */
class MeteorSwarmScenarioTest : ScenarioTestBase() {

    private fun board(opponentCreatures: Int = 3, mountains: Int = 5) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Meteor Swarm")
        .apply { repeat(opponentCreatures) { withCardOnBattlefield(2, "Centaur Courser") } } // 3/3s
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Meteor Swarm") {

            test("X = 2 divides all 8 damage between exactly two targets") {
                val game = board()
                val spell = game.findCardsInHand(1, "Meteor Swarm").single()
                val (a, b, c) = game.findAllPermanents("Centaur Courser")

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)),
                        xValue = 2,
                        damageDistribution = mapOf(a to 5, b to 3),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("both targets take lethal; the untargeted Courser is untouched") {
                    game.state.getBattlefield().contains(a) shouldBe false
                    game.state.getBattlefield().contains(b) shouldBe false
                    game.state.getBattlefield().contains(c) shouldBe true
                }
            }

            test("the number of targets must equal X and the division must total 8") {
                val game = board()
                val spell = game.findCardsInHand(1, "Meteor Swarm").single()
                val (a, b, _) = game.findAllPermanents("Centaur Courser")

                withClue("X = 2 with a single target") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(a)),
                            xValue = 2,
                            damageDistribution = mapOf(a to 8),
                        )
                    ).error shouldNotBe null
                }
                withClue("a division totalling 6, not 8") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)),
                            xValue = 2,
                            damageDistribution = mapOf(a to 3, b to 3),
                        )
                    ).error shouldNotBe null
                }
                game.findCardsInHand(1, "Meteor Swarm").single() shouldBe spell
            }

            test("X is offered up to the legal targets, and never above 8") {
                val fewTargets = board(opponentCreatures = 3, mountains = 15)
                val fewSpell = fewTargets.findCardsInHand(1, "Meteor Swarm").single()
                withClue("three legal targets cap X at 3") {
                    fewTargets.getLegalActions(1)
                        .first { (it.action as? CastSpell)?.cardId == fewSpell }
                        .maxAffordableX shouldBe 3
                }

                val manyTargets = board(opponentCreatures = 10, mountains = 15)
                val manySpell = manyTargets.findCardsInHand(1, "Meteor Swarm").single()
                withClue("ten targets and twelve spare mana still cap X at 8") {
                    manyTargets.getLegalActions(1)
                        .first { (it.action as? CastSpell)?.cardId == manySpell }
                        .maxAffordableX shouldBe 8
                }
            }
        }
    }
}
