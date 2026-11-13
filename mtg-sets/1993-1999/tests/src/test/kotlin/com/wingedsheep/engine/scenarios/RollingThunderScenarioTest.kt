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
 * Rolling Thunder (TMP #198, {X}{R}{R}, Sorcery).
 *
 *   Rolling Thunder deals X damage divided as you choose among any number of targets.
 *
 * The first "any number of targets" — players and permanents alike — whose cap is the announced X:
 * each target must be assigned at least 1, so X bounds the target count, and the division is
 * announced with the targets (CR 601.2d) and held to X.
 */
class RollingThunderScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Rolling Thunder")
        .withCardOnBattlefield(2, "Llanowar Elves")   // 1/1
        .withCardOnBattlefield(2, "Centaur Courser")  // 3/3
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Rolling Thunder") {

            test("the cast offer divides X damage, with the target count capped by X") {
                val game = board()
                val spell = game.findCardsInHand(1, "Rolling Thunder").single()
                val offer = game.getLegalActions(1).first { (it.action as? CastSpell)?.cardId == spell }
                offer.requiresDamageDistribution shouldBe true
                offer.xConstrainsTargetCount shouldBe true
                withClue("five Mountains pay {X}{R}{R} for X up to 3; the total is X itself") {
                    offer.totalDamageByX!![3] shouldBe 3
                }
            }

            test("X = 3 divided between a creature and a player") {
                val game = board()
                val spell = game.findCardsInHand(1, "Rolling Thunder").single()
                val elves = game.findPermanent("Llanowar Elves")!!

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Player(game.player2Id)),
                        xValue = 3,
                        damageDistribution = mapOf(elves to 1, game.player2Id to 2),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("1 kills the Elves, 2 goes to the opponent's face; the Courser is untouched") {
                    game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                    game.getLifeTotal(2) shouldBe 18
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }

            test("no more than X targets, and the division must total X") {
                val game = board()
                val spell = game.findCardsInHand(1, "Rolling Thunder").single()
                val elves = game.findPermanent("Llanowar Elves")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val opponent = game.player2Id

                withClue("three targets can't each take 1 of X = 2") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(
                                ChosenTarget.Permanent(elves),
                                ChosenTarget.Permanent(courser),
                                ChosenTarget.Player(opponent),
                            ),
                            xValue = 2,
                            damageDistribution = mapOf(elves to 1, courser to 1, opponent to 0),
                        )
                    ).error shouldNotBe null
                }
                withClue("3 damage divided is not X = 2") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Permanent(courser)),
                            xValue = 2,
                            damageDistribution = mapOf(elves to 1, courser to 2),
                        )
                    ).error shouldNotBe null
                }

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(courser)),
                        xValue = 3,
                        damageDistribution = mapOf(courser to 3),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
            }
        }
    }
}
