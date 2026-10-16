package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Urabrask, Heretic Praetor (SNC #129) — {3}{R}{R} Legendary Creature — Phyrexian Praetor 4/4.
 *
 *   Haste
 *   At the beginning of your upkeep, exile the top card of your library. You may play it this turn.
 *   At the beginning of each opponent's upkeep, the next time they would draw a card this turn,
 *   instead they exile the top card of their library. They may play it this turn.
 *
 * Covers: the opponent's draw-step draw is replaced by exiling their own top card, which *they*
 * may cast that turn (and Urabrask's controller draws nothing extra); and the controller's own
 * upkeep impulse, which leaves the regular draw untouched.
 */
class UrabraskHereticPraetorScenarioTest : ScenarioTestBase() {

    private fun cardNames(game: TestGame, ids: List<EntityId>) =
        ids.map { game.state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        context("Urabrask, Heretic Praetor") {

            test("an opponent's next draw becomes an exile they may play this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Urabrask, Heretic Praetor")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val p1Hand = game.handSize(1)
                val p2Hand = game.handSize(2)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id

                withClue("the draw-step draw was replaced: Grizzly Bears went to exile, not hand") {
                    game.handSize(2) shouldBe p2Hand
                    cardNames(game, game.state.getExile(game.player2Id)) shouldContain "Grizzly Bears"
                }
                withClue("it's their library, not Urabrask's controller's") {
                    game.state.getExile(game.player1Id) shouldBe emptyList()
                    game.handSize(1) shouldBe p1Hand
                }

                game.castSpellFromExile(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("the opponent may play the exiled card this turn") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
            }

            test("your upkeep exiles your top card to play this turn; your draw still happens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Urabrask, Heretic Praetor")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val p1Hand = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id

                withClue("Grizzly Bears was exiled at upkeep, and the draw step drew a Mountain") {
                    cardNames(game, game.state.getExile(game.player1Id)) shouldContain "Grizzly Bears"
                    game.handSize(1) shouldBe p1Hand + 1
                }

                game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }
    }
}
