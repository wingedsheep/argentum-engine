package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.AclazotzDeepestBetrayal
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aclazotz, Deepest Betrayal // Temple of the Dead (LCI #88).
 *
 *  1. **Attack — discard / draw-for-who-can't** — each opponent discards a card; for each opponent
 *     with an empty hand (who can't) the controller draws instead.
 *  2. **Bat on land discard** — an opponent discarding a land card makes a 1/1 black flying Bat.
 *  3. **Dies → return tapped + transformed** — the shared dies-trigger returns Temple of the Dead.
 *  4. **Transform-back gate** — Temple of the Dead's transform needs some player at one or fewer
 *     cards in hand.
 */
class AclazotzDeepestBetrayalScenarioTest : ScenarioTestBase() {

    init {
        fun attack(game: TestGame) {
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Aclazotz, Deepest Betrayal" to 2)).error shouldBe null
        }

        // Resolve the attack trigger, answering the opponent's discard selection (if any).
        fun resolveAttackTrigger(game: TestGame) {
            var guard = 0
            while (guard++ < 20) {
                val decision = game.getPendingDecision()
                if (decision is SelectCardsDecision) {
                    val pick = decision.options.take(decision.minSelections.coerceAtLeast(1))
                    game.submitDecision(CardsSelectedResponse(decision.id, pick))
                } else if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                } else break
            }
        }

        context("Aclazotz, Deepest Betrayal") {

            test("an opponent with cards discards one and the controller draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aclazotz, Deepest Betrayal", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(2, "Lightning Bolt") // two non-land cards
                    .withCardInLibrary(1, "Swamp") // a card to draw if (wrongly) drawn
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                attack(game)
                resolveAttackTrigger(game)

                withClue("the opponent discarded exactly one card") { game.handSize(2) shouldBe 1 }
                withClue("the controller drew nothing (opponent could discard)") { game.handSize(1) shouldBe 0 }
            }

            test("draws a card for an opponent who can't discard (empty hand)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aclazotz, Deepest Betrayal", summoningSickness = false)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                attack(game)
                resolveAttackTrigger(game)

                withClue("the empty-handed opponent couldn't discard, so the controller drew 1") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("an opponent discarding a land card makes a 1/1 flying Bat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aclazotz, Deepest Betrayal", summoningSickness = false)
                    .withCardInHand(2, "Forest") // the opponent's only card is a land
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                attack(game)
                resolveAttackTrigger(game)

                withClue("the discarded land triggered one Bat token") {
                    game.findPermanents("Bat Token").size shouldBe 1
                }
            }

            test("when it dies it returns tapped as Temple of the Dead") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aclazotz, Deepest Betrayal", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aclazotz = game.findPermanent("Aclazotz, Deepest Betrayal")!!

                repeat(2) {
                    game.castSpell(1, "Lightning Bolt", targetId = aclazotz).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }
                var guard = 0
                while (game.findPermanent("Temple of the Dead") == null && guard++ < 10) game.resolveStack()

                withClue("same entity returned as the back face, tapped") {
                    game.findPermanent("Temple of the Dead") shouldBe aclazotz
                    game.state.getEntity(aclazotz)!!.get<TappedComponent>() shouldNotBe null
                }
            }
        }

        context("Temple of the Dead — transform-back gate") {

            val transformAbilityId = AclazotzDeepestBetrayal.backFace!!
                .activatedAbilities.first { !it.isManaAbility }.id

            test("transforms back while a player has one or fewer cards in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of the Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3) // {2}{B} for the transform
                    // Player1's hand is empty (0 <= 1), so the gate is satisfied.
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of the Dead")!!
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the land flipped to its front face") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Aclazotz, Deepest Betrayal"
                }
            }

            test("cannot transform back while every player has two or more cards in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of the Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of the Dead")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId)
                )

                withClue("activation is illegal — no player at one or fewer cards") {
                    result.error shouldNotBe null
                }
                withClue("it stays a land") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Temple of the Dead"
                }
            }
        }
    }
}
