package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dsk.cards.SayItsName
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/** Scenario tests for Say Its Name. */
class SayItsNameScenarioTest : ScenarioTestBase() {

    init {
        context("Say Its Name — mill three, then may return a creature/land card") {

            test("mills three and returns a chosen creature card from the graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Say Its Name")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Say Its Name").error shouldBe null
                game.resolveStack()

                // Mill 3 puts the top three library cards (Grizzly Bears, Lightning Bolt, Forest) into
                // the graveyard, then a Gather/Select(up to 1) over creature/land cards prompts.
                withClue("Say Its Name pauses for the optional creature/land return") {
                    game.hasPendingDecision() shouldBe true
                }
                val bearsInGy = game.findCardsInGraveyard(1, "Grizzly Bears")
                withClue("Milled Grizzly Bears is a legal return candidate") {
                    bearsInGy.isNotEmpty() shouldBe true
                }
                game.selectCards(listOf(bearsInGy.first())).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returned to hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("Lightning Bolt (instant) stays milled in the graveyard") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
            }

            test("graveyard ability exiles three Say Its Names as its cost and searches for Altanak") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Say Its Name")
                    .withCardInGraveyard(1, "Say Its Name")
                    .withCardInGraveyard(1, "Say Its Name")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val copies = game.findCardsInGraveyard(1, "Say Its Name")
                withClue("Three Say Its Name copies seeded in the graveyard") {
                    copies.size shouldBe 3
                }
                val sourceId = copies.first()
                val abilityId = SayItsName.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(exiledCards = copies)
                    )
                )
                withClue("Activating the graveyard ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                // The multi-zone search may surface a (possibly empty) optional selection; skip it.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                // Altanak, the Thrice-Called isn't implemented, so the search finds nothing — but the
                // cost is still paid: all three Say Its Name copies are exiled from the graveyard.
                withClue("All three Say Its Name copies were exiled to pay the cost") {
                    game.findCardsInGraveyard(1, "Say Its Name").size shouldBe 0
                    game.state.getExile(game.player1Id).size shouldBeGreaterThanOrEqual 3
                }
            }
        }
    }
}
