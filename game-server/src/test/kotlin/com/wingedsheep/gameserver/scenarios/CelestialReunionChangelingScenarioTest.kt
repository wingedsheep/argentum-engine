package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Behold-the-chosen-type on Celestial Reunion must include changelings, since a
 * changeling card is every creature type (Rule 702.73a). Regression for the missing
 * changeling branch in `CardPredicate.HasSubtypeFromVariable`.
 */
class CelestialReunionChangelingScenarioTest : ScenarioTestBase() {

    init {
        test("beholding 'Elf' includes shapeshifters with Changeling") {
            // Three creatures total so the behold-two SelectCardsDecision actually pauses
            // for input (ChooseExactly(2) auto-resolves when there are only 2 candidates).
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Celestial Reunion")
                .withCardOnBattlefield(1, "Gangly Stompling")     // Shapeshifter, Changeling
                .withCardOnBattlefield(1, "Chitinous Graspling")  // Shapeshifter, Changeling
                .withCardOnBattlefield(1, "Elvish Ranger")        // literal Elf
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val castResult = game.castXSpell(1, "Celestial Reunion", xValue = 0)
            withClue("Cast should succeed: ${castResult.error}") {
                castResult.error shouldBe null
            }

            game.resolveStack()

            // 1. May we behold? Yes.
            game.answerYesNo(true)

            // 2. Choose creature type Elf (neither changeling literally has "Elf").
            val typeDecision = game.state.pendingDecision
            typeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            val elfIndex = typeDecision.options.indexOf("Elf")
            withClue("'Elf' should be in creature type options") {
                (elfIndex >= 0) shouldBe true
            }
            game.submitDecision(OptionChosenResponse(typeDecision.id, elfIndex))

            // 3. Behold-two pool must include both changelings even though
            //    neither carries the literal "Elf" subtype.
            val beholdDecision = game.state.pendingDecision
            beholdDecision.shouldBeInstanceOf<SelectCardsDecision>()

            val ganglyId = game.findPermanent("Gangly Stompling")!!
            val graspId = game.findPermanent("Chitinous Graspling")!!

            withClue("Gangly Stompling should be beholdable as an Elf via Changeling") {
                (ganglyId in beholdDecision.options) shouldBe true
            }
            withClue("Chitinous Graspling should be beholdable as an Elf via Changeling") {
                (graspId in beholdDecision.options) shouldBe true
            }
        }
    }
}
