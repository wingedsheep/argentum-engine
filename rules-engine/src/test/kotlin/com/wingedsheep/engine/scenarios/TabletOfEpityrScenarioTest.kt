package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tablet of Epityr (ATQ #67).
 *
 * {1} Artifact
 * "Whenever an artifact you control is put into a graveyard from the battlefield, you may pay {1}.
 *  If you do, you gain 1 life."
 */
class TabletOfEpityrScenarioTest : ScenarioTestBase() {

    // {0} sorcery that destroys a target permanent, used to send an artifact to the graveyard.
    private val slayPermanent = card("Slay Permanent") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target permanent."
        spell {
            val t = target("target permanent", Targets.Permanent)
            effect = Effects.Destroy(t)
        }
    }

    init {
        cardRegistry.register(slayPermanent)

        context("Tablet of Epityr") {

            test("paying {1} when an artifact you control dies gains 1 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tablet of Epityr")
                    // A second artifact you control that we destroy.
                    .withCardOnBattlefield(1, "Ashnod's Altar")
                    .withCardInHand(1, "Slay Permanent")
                    .withLandsOnBattlefield(1, "Mountain", 1) // pays the {1} for the may-pay
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val altar = game.findPermanent("Ashnod's Altar")!!
                game.castSpell(1, "Slay Permanent", altar).error shouldBe null
                game.resolveStack()

                withClue("Ashnod's Altar should be destroyed") {
                    game.isOnBattlefield("Ashnod's Altar") shouldBe false
                }

                // Tablet's trigger goes on the stack and resolves into the "you may pay {1}" prompt.
                withClue("Tablet of Epityr offers the may-pay {1} decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Paying {1} gains 1 life") {
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("declining to pay {1} gains no life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tablet of Epityr")
                    .withCardOnBattlefield(1, "Ashnod's Altar")
                    .withCardInHand(1, "Slay Permanent")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val altar = game.findPermanent("Ashnod's Altar")!!
                game.castSpell(1, "Slay Permanent", altar).error shouldBe null
                game.resolveStack()

                withClue("Tablet of Epityr offers the may-pay {1} decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Declining the payment gains no life") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
