package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * Scenario tests for Living Brain, Mechanical Marvel.
 *
 * Card reference:
 * - Living Brain, Mechanical Marvel ({4}): Legendary Artifact Creature — Robot Villain, 3/3
 *   "At the beginning of combat on your turn, target non-Equipment artifact you control becomes
 *    an artifact creature with base power and toughness 3/3 until end of turn. Untap it."
 */
class LivingBrainMechanicalMarvelScenarioTest : ScenarioTestBase() {

    init {
        context("Living Brain, Mechanical Marvel enters the battlefield") {

            test("cast for {4} resolves and enters battlefield as a 3/3 Legendary Artifact Creature — Robot Villain") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Living Brain, Mechanical Marvel")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Living Brain, Mechanical Marvel")
                withClue("Casting Living Brain, Mechanical Marvel should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Living Brain, Mechanical Marvel should be on the battlefield") {
                    game.isOnBattlefield("Living Brain, Mechanical Marvel") shouldBe true
                }

                val permanentId = game.findPermanent("Living Brain, Mechanical Marvel")
                withClue("Permanent ID should not be null") {
                    permanentId shouldNotBe null
                }

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[permanentId]
                withClue("Card info should not be null") {
                    cardInfo shouldNotBe null
                }

                withClue("Should have power 3") {
                    cardInfo!!.power shouldBe 3
                }
                withClue("Should have toughness 3") {
                    cardInfo!!.toughness shouldBe 3
                }
                withClue("Should be a Legendary Artifact Creature — Robot Villain") {
                    cardInfo!!.typeLine shouldBe "Legendary Artifact Creature — Robot Villain"
                }
            }
        }

        context("Living Brain, Mechanical Marvel beginning of combat trigger") {

            test("targeted tapped non-Equipment artifact becomes 3/3 artifact creature and is untapped until end of turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Living Brain, Mechanical Marvel")
                    .withCardOnBattlefield(1, "Stabilizer", true)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stabilizerId = game.findPermanent("Stabilizer")!!

                // Advance to beginning of combat — triggered ability fires and goes on the stack
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                // Select the tapped Stabilizer as target for the triggered ability
                game.selectTargets(listOf(stabilizerId))
                game.resolveStack()

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[stabilizerId]

                withClue("Stabilizer should now be an artifact creature") {
                    cardInfo!!.typeLine shouldContain "Creature"
                }
                withClue("Stabilizer should have base power 3") {
                    cardInfo!!.power shouldBe 3
                }
                withClue("Stabilizer should have base toughness 3") {
                    cardInfo!!.toughness shouldBe 3
                }
                withClue("Stabilizer should be untapped after the ability resolves") {
                    game.state.getEntity(stabilizerId)!!.has<TappedComponent>() shouldBe false
                }

                // Advance to next turn's precombat main — CLEANUP has already expired
                // the until-end-of-turn effect; stopping here avoids the next BEGIN_COMBAT
                // trigger firing again before we can check state
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val clientStateAfterEnd = game.getClientState(1)
                val cardInfoAfterEnd = clientStateAfterEnd.cards[stabilizerId]
                withClue("Stabilizer should revert to a non-creature artifact after end of turn") {
                    cardInfoAfterEnd!!.typeLine shouldNotContain "Creature"
                }
            }
        }
    }
}
