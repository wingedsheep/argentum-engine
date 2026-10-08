package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Monk Gyatso — "Whenever another creature you control becomes the target of a spell or ability,
 * you may airbend that creature."
 *
 * Pins the yes / no branches of the "may", that the airbent creature is castable from exile for
 * {2} by its owner (and the spell that targeted it loses its target), that Gyatso doesn't trigger
 * for itself ("another"), and that a creature that left the battlefield in response is not
 * airbent from its new zone (CR 400.7).
 */
class MonkGyatsoScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Monk Gyatso")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("yes: the targeted creature is airbent and its owner may recast it for {2}") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            withClue("Grizzly Bears is exiled") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
            withClue("Giant Growth lost its only target and went to the graveyard") {
                game.isInGraveyard(1, "Giant Growth") shouldBe true
            }
            withClue("its owner can cast it from exile with the two remaining Forests ({2})") {
                game.getLegalActions(1).any {
                    it.action is CastSpell && (it.action as CastSpell).cardId == bears && it.isAffordable
                } shouldBe true
            }
        }

        test("no: the creature stays and the spell resolves") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(false)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 5
        }

        test("targeting Gyatso itself doesn't trigger") {
            val game = setup()
            val monk = game.findPermanent("Monk Gyatso")!!
            game.castSpell(1, "Giant Growth", monk).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.state.projectedState.getPower(monk) shouldBe 6
        }

        test("a creature that left the battlefield in response is not airbent from its new zone") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            // Gyatso's trigger is on the stack above Giant Growth; the Bears leave before it resolves.
            game.state = game.zones.moveToZone(game.state, bears, Zone.HAND).state
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            withClue("the Bears card stays in hand rather than being exiled from there") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }
        }
    }
}
