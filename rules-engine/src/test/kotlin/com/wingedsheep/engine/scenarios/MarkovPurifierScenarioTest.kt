package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Markov Purifier (VOW #312) — {1}{W}{B} Creature — Vampire Cleric, 2/3.
 *
 *   Lifelink
 *   At the beginning of your end step, if you gained life this turn, you may pay {2}. If you do,
 *   draw a card.
 *
 * Exercises the printed Lifelink keyword and the intervening-if end-step trigger: without life
 * gained this turn the trigger doesn't even fire; after gaining life this turn, it offers the
 * may-pay {2}, and paying draws a card.
 */
class MarkovPurifierScenarioTest : ScenarioTestBase() {

    init {
        // A simple life-gain spell to turn the "if you gained life this turn" condition on.
        cardRegistry.register(
            CardDefinition.instant(
                name = "Healing Salve Test",
                manaCost = ManaCost.parse("{W}"),
                oracleText = "You gain 3 life.",
                script = CardScript.spell(effect = GainLifeEffect(3, EffectTarget.Controller)),
            )
        )

        context("Markov Purifier") {

            test("has Lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Markov Purifier", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val purifier = game.findPermanent("Markov Purifier")!!
                withClue("Markov Purifier has Lifelink") {
                    game.state.projectedState.hasKeyword(purifier, Keyword.LIFELINK) shouldBe true
                }
            }

            test("without gaining life this turn, the end step trigger does not fire") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Markov Purifier", summoningSickness = false)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handSizeBefore = game.handSize(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("no life gained this turn -> no may-pay offer, no card drawn") {
                    game.handSize(1) shouldBe handSizeBefore
                }
            }

            test("after gaining life this turn, paying {2} at the end step draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Markov Purifier", summoningSickness = false)
                    .withCardInHand(1, "Healing Salve Test")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Healing Salve Test").error shouldBe null
                game.resolveStack()

                val handSizeBefore = game.handSize(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack() // end-step trigger resolves to the may-pay gate

                withClue("the may-pay {2} gate is offered") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null

                withClue("paying then asks which mana sources to use") {
                    (game.getPendingDecision() is SelectManaSourcesDecision) shouldBe true
                }
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("paying {2} draws a card") {
                    game.handSize(1) shouldBe handSizeBefore + 1
                }
            }
        }
    }
}
