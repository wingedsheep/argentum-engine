package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Veteran Survivor (DSK #40) — Rule 400.7 compliance.
 *
 * Rule 400.7: An object that moves from one zone to another becomes a new object
 * with no memory of, or relation to, its previous existence.
 *
 * When Veteran Survivor is bounced to hand and recast, the new battlefield
 * instance must have no linked exile. The previously exiled card remains in exile.
 */
class VeteranSurvivorZoneChangeScenarioTest : ScenarioTestBase() {

    init {
        context("Veteran Survivor — Rule 400.7 zone change") {

            test("linked exile is cleared when bounced to hand; exiled card stays exiled") {
                // Setup:
                //  - P1: Veteran Survivor on battlefield, 1 card linked in exile,
                //        1 untapped Plains (to recast VS), 2 library cards
                //  - P2: Calamitous Tide in hand ({4}{U}{U}), 2 Islands + 4 Plains untapped,
                //        2 Wind Drakes in library (drawn after Tide resolves)
                //  - It is P2's precombat main so they can cast a sorcery.

                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Veteran Survivor", tapped = false)
                    .withCardInExile(1, "Wind Drake")   // card linked to VS
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(2, "Calamitous Tide")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withCardInLibrary(2, "Wind Drake")
                    .withCardInLibrary(2, "Wind Drake")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withActivePlayer(2)
                    .build()

                // Manually attach LinkedExileComponent to Veteran Survivor to simulate
                // having used the Survival trigger to exile Wind Drake.
                val veteranSurvivorId = game.findPermanent("Veteran Survivor")!!
                val exiledCardId = game.state.getExile(game.player1Id).first()

                game.state = game.state.updateEntity(veteranSurvivorId) { container ->
                    container.with(LinkedExileComponent(listOf(exiledCardId)))
                }

                withClue("pre-condition: VS should have 1 linked exile") {
                    game.state.getEntity(veteranSurvivorId)!!
                        .get<LinkedExileComponent>()!!
                        .exiledIds.size shouldBe 1
                }

                // P2 casts Calamitous Tide targeting Veteran Survivor.
                // Tide takes up to 2 targets (optional); providing 1 is valid.
                game.castSpell(2, "Calamitous Tide", veteranSurvivorId)

                // P1 has no responses — pass. Then Tide resolves.
                game.passPriority()
                game.resolveStack()

                // Calamitous Tide resolves: VS bounces, then P2 draws 2 and must discard 1.
                // Handle the discard decision if still pending.
                if (game.hasPendingDecision()) {
                    val p2Hand = game.state.getHand(game.player2Id)
                    game.selectCards(listOf(p2Hand.first()))
                }

                withClue("Veteran Survivor should be in P1's hand after being bounced") {
                    game.isOnBattlefield("Veteran Survivor") shouldBe false
                    game.findCardsInHand(1, "Veteran Survivor").size shouldBe 1
                }

                withClue("Wind Drake should still be in P1's exile after VS left battlefield") {
                    game.state.getExile(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Wind Drake"
                    } shouldBe true
                }

                // Advance directly to P1's precombat main (set active player and phase).
                // Using direct state manipulation is simpler than full turn cycling for this test.
                game.state = game.state.copy(
                    phase = Phase.PRECOMBAT_MAIN,
                    step = Step.PRECOMBAT_MAIN,
                    activePlayerId = game.player1Id,
                    priorityPlayerId = game.player1Id
                )

                withClue("P1 should have Veteran Survivor in hand to recast") {
                    game.findCardsInHand(1, "Veteran Survivor").size shouldBe 1
                }

                // P1 recasts Veteran Survivor ({W} = tap the 1 Plains).
                game.castSpell(1, "Veteran Survivor")
                game.passPriority() // P2 passes
                game.resolveStack()

                // Rule 400.7: the new instance is a new object — no linked exile.
                val newVsId = game.findPermanent("Veteran Survivor")
                withClue("Veteran Survivor should be back on the battlefield") {
                    newVsId shouldNotBe null
                }

                withClue("New Veteran Survivor has no linked exile (Rule 400.7)") {
                    val linkedIds = game.state.getEntity(newVsId!!)
                        ?.get<LinkedExileComponent>()
                        ?.exiledIds
                        .orEmpty()
                    linkedIds.size shouldBe 0
                }

                withClue("Wind Drake remains in P1's exile — it was not returned by the zone change") {
                    game.state.getExile(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Wind Drake"
                    } shouldBe true
                }
            }
        }
    }
}
