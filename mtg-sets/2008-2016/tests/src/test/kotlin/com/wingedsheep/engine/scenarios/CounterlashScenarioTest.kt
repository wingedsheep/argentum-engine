package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Counterlash (DKA #33) — "Counter target spell. You may cast a spell that shares a card type with
 * it from your hand without paying its mana cost."
 *
 * The opponent casts a creature on their own turn; Counterlash counters it and its caster may cast
 * a creature from hand for free at instant speed. A non-matching card can't be chosen, and
 * declining casts nothing.
 */
class CounterlashScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Counterlash")
            .withCardInHand(1, "Hill Giant")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Island", 6)
            .withCardInHand(2, "Grizzly Bears")
            .withLandsOnBattlefield(2, "Forest", 2)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.counterTheBears() {
            castSpell(2, "Grizzly Bears").error shouldBe null
            passPriority()
            castSpellTargetingStackSpell(1, "Counterlash", "Grizzly Bears").error shouldBe null
            resolveStack()
        }

        context("Counterlash") {

            test("counters the spell and casts a creature from hand for free at instant speed") {
                val game = board()
                game.counterTheBears()

                (game.state.pendingDecision is SelectCardsDecision) shouldBe true
                withClue("an instant doesn't share a card type with a creature spell") {
                    game.selectCards(listOf(game.findCardsInHand(1, "Lightning Bolt").single()))
                        .outcome shouldNotBe Outcome.Done
                }
                game.selectCards(listOf(game.findCardsInHand(1, "Hill Giant").single())).error shouldBe null
                game.resolveStack()

                withClue("the bears were countered") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("the giant resolved under Alice's control") {
                    val giant = game.findPermanent("Hill Giant")!!
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                }
                game.isInHand(1, "Lightning Bolt") shouldBe true
            }

            test("declining still counters the spell and casts nothing") {
                val game = board()
                game.counterTheBears()

                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
                game.findPermanent("Hill Giant") shouldBe null
            }
        }
    }
}
