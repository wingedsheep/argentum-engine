package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Waterbender's Restoration — {U}{U} instant, "As an additional cost to cast this spell, waterbend
 * {X}. Exile X target creatures you control. Return those cards to the battlefield under their
 * owner's control at the beginning of the next end step."
 *
 * Pins the cost-linked target count (X from the waterbend {X} cost bounds the targets), that the
 * waterbend taps may come from the very creatures being targeted, and the delayed return at the
 * next end step.
 */
class WaterbendersRestorationScenarioTest : ScenarioTestBase() {

    init {
        test("waterbend X by tapping the targets themselves, exile them, and return them at the next end step") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Waterbender's Restoration")
                .withLandsOnBattlefield(1, "Island", 2)       // the {U}{U}
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val seeker = game.findPermanent("Glory Seeker")!!

            val action = game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.isAffordable && it.hasTapForGeneric
            }
            withClue("Waterbender's Restoration is offered as an X-carrying waterbend cast") {
                action shouldNotBe null
                action!!.hasXCost shouldBe true
            }

            val cast = (action!!.action as CastSpell).copy(
                xValue = 2,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(seeker)),
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = setOf(bears, seeker)),
            )
            val result = game.execute(cast)
            withClue("casting for waterbend {X=2}, tapping both targets, succeeds: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("both creatures are exiled") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Glory Seeker") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("both cards return to the battlefield at the beginning of the next end step") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Glory Seeker") shouldBe true
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }
        }

        test("X caps the number of targets") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Waterbender's Restoration")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            val action = game.getLegalActions(1).first {
                it.actionType == "CastSpell" && it.action is CastSpell && it.hasXCost
            }

            val cast = (action.action as CastSpell).copy(
                xValue = 1,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(seeker)),
            )
            withClue("two targets with X = 1 is rejected") {
                game.execute(cast).error shouldNotBe null
            }
        }
    }
}
