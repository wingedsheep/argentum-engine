package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Avacynian Missionaries // Lunarch Inquisitors (SOI #6).
 *
 * Front: "At the beginning of your end step, if this creature is equipped, transform it."
 * Back:  "When this creature transforms into Lunarch Inquisitors, you may exile another target
 *        creature until this creature leaves the battlefield."
 *
 * Covers the equipped intervening "if", the "until leaves" duration anchored on a permanent that
 * has just transformed (same object, so the exile lasts until the Inquisitors leave), and the
 * resolution-time "you may".
 */
class AvacynianMissionariesScenarioTest : ScenarioTestBase() {

    private fun board(equipped: Boolean): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Avacynian Missionaries", summoningSickness = false)
        .apply { if (equipped) withCardAttachedTo(1, "Bonesplitter", "Avacynian Missionaries") }
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        .build()

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Pass to the end step, let the transform resolve, and target the Bears with the back-face trigger. */
    private fun TestGame.transformAndTargetBears(): EntityId {
        val missionaries = findPermanent("Avacynian Missionaries")!!
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
        nameOf(missionaries) shouldBe "Lunarch Inquisitors"

        val bears = findPermanent("Grizzly Bears")!!
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("\"another\" target creature: the Inquisitors can't target themselves") {
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
        }
        selectTargets(listOf(bears)).error shouldBe null
        resolveStack()
        return missionaries
    }

    init {
        test("equipped at end step: transforms, exiles the target, and it returns when the Inquisitors leave") {
            val game = board(equipped = true)
            val inquisitors = game.transformAndTargetBears()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            withClue("the Equipment stays attached through the transform") {
                game.isOnBattlefield("Bonesplitter") shouldBe true
            }

            game.castSpell(1, "Murder", inquisitors).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Avacynian Missionaries") shouldBe true
            withClue("the exiled creature returns as soon as the Inquisitors leave the battlefield") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }

        test("the exile is optional") {
            val game = board(equipped = true)
            game.transformAndTargetBears()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInExile(2, "Grizzly Bears") shouldBe false
        }

        test("unequipped at end step: nothing triggers and it stays Avacynian Missionaries") {
            val game = board(equipped = false)
            val missionaries = game.findPermanent("Avacynian Missionaries")!!
            game.passUntilPhase(Phase.ENDING, Step.END)

            game.state.stack.size shouldBe 0
            game.nameOf(missionaries) shouldBe "Avacynian Missionaries"
        }
    }
}
