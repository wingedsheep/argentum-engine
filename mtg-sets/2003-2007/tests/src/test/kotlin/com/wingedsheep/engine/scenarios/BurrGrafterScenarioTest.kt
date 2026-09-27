package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.BurrGrafter
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Burr Grafter (CHK #203) — "Sacrifice this creature: Target creature gets +2/+2 until end of turn.
 * Soulshift 3."
 *
 * Sacrificing itself to pay the cost is a death, so soulshift triggers off its own ability.
 */
class BurrGrafterScenarioTest : ScenarioTestBase() {

    private val pumpAbility = BurrGrafter.activatedAbilities.single().id

    init {
        context("Burr Grafter") {

            test("sacrifice pumps target creature and soulshift 3 returns a Spirit") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Burr Grafter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3 — the inclusive bound
                    .withCardInGraveyard(1, "Vine Kami")        // Spirit, MV 7 — too big
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grafter = game.findPermanent("Burr Grafter")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = grafter,
                        abilityId = pumpAbility,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                game.isInGraveyard(1, "Burr Grafter") shouldBe true

                val yesNo = game.getPendingDecision()
                yesNo.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val hunt = game.findCardsInGraveyard(1, "Kami of the Hunt").single()
                withClue("Kami of the Hunt (MV 3) is legal; Vine Kami (MV 7) is not; Burr Grafter (MV 4) is not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(hunt)
                }
                game.selectTargets(listOf(hunt))
                game.resolveStack()

                game.isInHand(1, "Kami of the Hunt") shouldBe true
                game.isInGraveyard(1, "Vine Kami") shouldBe true
                withClue("Grizzly Bears gets +2/+2") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }
        }
    }
}
