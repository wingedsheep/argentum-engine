package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Inscription of Insight (ZNR #61) — {3}{U} Sorcery, Kicker {2}{U}{U}.
 *
 *   Choose one. If this spell was kicked, choose any number instead.
 *   • Return up to two target creatures to their owners' hands.
 *   • Scry 2, then draw two cards.
 *   • Target player creates an X/X blue Illusion creature token, where X is the number of cards in
 *     their hand.
 *
 * Pins the cast-time kicker-gated mode count (`WasKicked` read off the cast's own declaration) and
 * that X is counted as the token mode is performed — after the bounce mode, in printed order.
 */
class InscriptionOfInsightScenarioTest : ScenarioTestBase() {

    init {
        context("Inscription of Insight") {

            test("kicked: bounce then token counts the bounced cards in the target player's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inscription of Insight")
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Inscription of Insight").first()
                val handBefore = game.handSize(2)

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(wurm),
                            ChosenTarget.Player(game.player2Id),
                        ),
                        chosenModes = listOf(0, 2),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(wurm)),
                            listOf(ChosenTarget.Player(game.player2Id)),
                        ),
                        declaredCostSlot = ChoiceSlot.KICKED,
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isInHand(2, "Craw Wurm") shouldBe true

                val illusions = game.findPermanents("Illusion Token")
                illusions shouldHaveSize 1
                val token = illusions.single()
                val expected = handBefore + 2
                withClue("X is the target player's hand size after the bounce mode resolved") {
                    game.state.projectedState.getPower(token) shouldBe expected
                    game.state.projectedState.getToughness(token) shouldBe expected
                }
                withClue("the target player creates the token") {
                    game.state.getEntity(token)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }
            }

            test("unkicked: choosing two modes is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inscription of Insight")
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Inscription of Insight").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Player(game.player1Id),
                        ),
                        chosenModes = listOf(0, 2),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears)),
                            listOf(ChosenTarget.Player(game.player1Id)),
                        ),
                    ),
                ).error.shouldNotBeNull()

                game.isInHand(1, "Inscription of Insight") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("unkicked: a single token mode for yourself counts your hand on resolution") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inscription of Insight")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Inscription of Insight").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        chosenModes = listOf(2),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(game.player1Id))),
                    ),
                ).error shouldBe null
                game.resolveStack()

                val token = game.findPermanents("Illusion Token").single()
                withClue("the spell itself has left the hand, so X counts the two cards still there") {
                    game.state.projectedState.getPower(token) shouldBe 2
                    game.state.projectedState.getToughness(token) shouldBe 2
                }
            }
        }
    }
}
