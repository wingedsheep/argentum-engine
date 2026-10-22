package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Devious Cover-Up (GRN #35, reprinted MID #48) — {2}{U}{U} Instant.
 *
 *   Counter target spell. If that spell is countered this way, exile it instead of putting it into
 *   its owner's graveyard. You may shuffle up to four target cards from your graveyard into your
 *   library.
 *
 * Pins the multi-requirement target binding: the counter must hit the spell target (declared
 * first), and only the chosen graveyard cards go back into the library.
 */
class DeviousCoverUpScenarioTest : ScenarioTestBase() {

    init {
        context("Devious Cover-Up") {

            test("exiles the countered spell and shuffles only the chosen graveyard cards into the library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Devious Cover-Up")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Divination")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Hill Giant").error shouldBe null
                game.passPriority()

                val giant = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                }
                val coverUp = game.findCardsInHand(1, "Devious Cover-Up").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        coverUp,
                        listOf(
                            ChosenTarget.Spell(giant),
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(bolt, game.player1Id, Zone.GRAVEYARD),
                        )
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant is countered into exile") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe false
                }
                withClue("the two chosen cards are shuffled into the library") {
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInLibrary(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInLibrary(1, "Plains").size shouldBe 1
                }
                withClue("the unchosen card stays, and Devious Cover-Up itself goes to the graveyard") {
                    game.isInGraveyard(1, "Divination") shouldBe true
                    game.isInGraveyard(1, "Devious Cover-Up") shouldBe true
                }
            }
        }
    }
}
