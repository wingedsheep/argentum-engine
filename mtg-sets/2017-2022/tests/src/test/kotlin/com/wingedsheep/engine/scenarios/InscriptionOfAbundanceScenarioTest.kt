package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Inscription of Abundance (ZNR #186) — {1}{G} Instant, Kicker {2}{G}.
 *
 *   Choose one. If this spell was kicked, choose any number instead.
 *   • Put two +1/+1 counters on target creature.
 *   • Target player gains X life, where X is the greatest power among creatures they control.
 *   • Target creature you control fights target creature you don't control.
 *
 * Pins the kicker-gated mode count and printed-order resolution: X is measured over the target
 * player's creatures after the counters mode, and the grown creature then wins the fight.
 */
class InscriptionOfAbundanceScenarioTest : ScenarioTestBase() {

    init {
        context("Inscription of Abundance") {

            test("kicked: counters, then life equal to the grown power, then the grown creature fights") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inscription of Abundance")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Inscription of Abundance").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(giant),
                        ),
                        chosenModes = listOf(0, 1, 2),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears)),
                            listOf(ChosenTarget.Player(game.player1Id)),
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                        ),
                        declaredCostSlot = ChoiceSlot.KICKED,
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears got two +1/+1 counters") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                }
                withClue("X is the greatest power after the counters mode resolved") {
                    game.getLifeTotal(1) shouldBe 24
                }
                withClue("the 4/4 Bears kills Hill Giant and survives its 3 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("unkicked: choosing two modes is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inscription of Abundance")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Inscription of Abundance").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Player(game.player1Id),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears)),
                            listOf(ChosenTarget.Player(game.player1Id)),
                        ),
                    ),
                ).error.shouldNotBeNull()

                game.isInHand(1, "Inscription of Abundance") shouldBe true
            }

            test("unkicked: life mode for the opponent reads their creatures, not yours") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inscription of Abundance")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Inscription of Abundance").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(game.player2Id))),
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 23
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
