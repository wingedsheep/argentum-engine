package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.Rootrunner
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rootrunner (CHK #237) — "{G}{G}, Sacrifice this creature: Put target land on top of its owner's
 * library. Soulshift 3."
 *
 * Sacrificing Rootrunner to its own ability is a death, so the one activation both puts the land on
 * top of its owner's library and fires soulshift.
 */
class RootrunnerScenarioTest : ScenarioTestBase() {

    private val bounceAbility = Rootrunner.activatedAbilities.single().id

    init {
        context("Rootrunner") {

            test("sacrifice puts an opponent's land on top of their library and triggers soulshift 3") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Rootrunner")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3 — the inclusive bound
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rootrunner = game.findPermanent("Rootrunner")!!
                val plains = game.findPermanent("Plains")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rootrunner,
                        abilityId = bounceAbility,
                        targets = listOf(ChosenTarget.Permanent(plains))
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                withClue("Rootrunner was sacrificed as a cost") {
                    game.isInGraveyard(1, "Rootrunner") shouldBe true
                }

                // Soulshift triggers off the sacrifice.
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val kami = game.findCardsInGraveyard(1, "Kami of the Hunt").single()
                withClue("only the MV-3 Spirit is legal; Rootrunner itself (MV 4) is not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(kami)
                }
                game.selectTargets(listOf(kami))
                game.resolveStack()

                game.isInHand(1, "Kami of the Hunt") shouldBe true
                withClue("the Plains is on top of Bob's library") {
                    game.findPermanent("Plains") shouldBe null
                    game.state.getLibrary(game.player2Id).first() shouldBe plains
                    game.librarySize(2) shouldBe 2
                }
            }
        }
    }
}
