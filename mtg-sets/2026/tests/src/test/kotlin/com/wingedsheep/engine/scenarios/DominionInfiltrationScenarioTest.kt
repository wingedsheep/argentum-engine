package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dominion Infiltration (TRK #147) — {X}{R}{R}{R} Sorcery:
 *   Choose X target creatures your opponents control. For each of them, create a token that's a
 *   copy of that creature, except it has changeling. Those tokens gain haste. Exile those tokens at
 *   the beginning of the next end step.
 *
 * Focus: X sets the target count; each token is yours, every creature type (changeling as a copy
 * exception), hasty (haste granted through the created-tokens collection inside each iteration),
 * and gone at the next end step.
 */
class DominionInfiltrationScenarioTest : ScenarioTestBase() {

    init {
        context("Dominion Infiltration") {

            fun board(): TestGame = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Dominion Infiltration")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun cast(game: TestGame, targets: List<EntityId>, x: Int) =
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Dominion Infiltration").single(),
                        targets = targets.map { ChosenTarget.Permanent(it) },
                        xValue = x,
                    )
                )

            test("X = 2 copies both opponent creatures as hasty changelings under your control") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                cast(game, listOf(bears, courser), 2).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                for ((name, original) in listOf("Grizzly Bears" to bears, "Centaur Courser" to courser)) {
                    val copies = game.findPermanents(name)
                    withClue("$name: the original plus one token") { copies.size shouldBe 2 }
                    val token = copies.single { it != original }
                    withClue("$name token is controlled by the caster") {
                        projected.getController(token) shouldBe game.player1Id
                    }
                    withClue("$name token has changeling and is every creature type") {
                        projected.hasKeyword(token, Keyword.CHANGELING) shouldBe true
                        projected.hasSubtype(token, "Elf") shouldBe true
                        projected.hasSubtype(token, "Vedalken") shouldBe true
                    }
                    withClue("$name token gained haste") {
                        projected.hasKeyword(token, Keyword.HASTE) shouldBe true
                    }
                    withClue("$name original is untouched") {
                        projected.hasKeyword(original, Keyword.CHANGELING) shouldBe false
                        projected.hasKeyword(original, Keyword.HASTE) shouldBe false
                    }
                }
            }

            test("the tokens are exiled at the beginning of the next end step") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                cast(game, listOf(bears, courser), 2).error shouldBe null
                game.resolveStack()
                game.findPermanents("Grizzly Bears").size shouldBe 2

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
                game.findPermanents("Centaur Courser") shouldBe listOf(courser)
            }

            test("a creature you control can't be targeted") {
                val game = board()
                val giant = game.findPermanent("Hill Giant")!!

                cast(game, listOf(giant), 1).error shouldNotBe null
            }
        }
    }
}
