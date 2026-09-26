package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.ScuttlingDeath
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scuttling Death (CHK #142) — "Sacrifice this creature: Target creature gets -1/-1 until end of turn.
 * Soulshift 4."
 *
 * Sacrificing it as its own cost is a death, so the one activation both shrinks the target and fires
 * soulshift.
 */
class ScuttlingDeathScenarioTest : ScenarioTestBase() {

    private val sacAbility = ScuttlingDeath.activatedAbilities.single().id

    init {
        context("Scuttling Death") {

            test("sacrificing it gives target creature -1/-1 and triggers soulshift 4") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Scuttling Death")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(1, "Ghost Ship")       // Spirit, MV 4 — the inclusive bound
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val death = game.findPermanent("Scuttling Death")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = death,
                        abilityId = sacAbility,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                game.isInGraveyard(1, "Scuttling Death") shouldBe true

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val ghostShip = game.findCardsInGraveyard(1, "Ghost Ship").single()
                withClue("Ghost Ship (MV 4) is legal; Scuttling Death itself (MV 5) is not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(ghostShip)
                }
                game.selectTargets(listOf(ghostShip))
                game.resolveStack()

                game.isInHand(1, "Ghost Ship") shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 1
                game.state.projectedState.getToughness(bears) shouldBe 1
            }

            test("the -1/-1 kills a 1-toughness creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Scuttling Death")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val death = game.findPermanent("Scuttling Death")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = death,
                        abilityId = sacAbility,
                        targets = listOf(ChosenTarget.Permanent(elves))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            }
        }
    }
}
