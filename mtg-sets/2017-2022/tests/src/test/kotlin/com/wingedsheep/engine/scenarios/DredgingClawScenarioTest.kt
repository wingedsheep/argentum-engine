package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dredging Claw — "Whenever a creature enters from your graveyard, you may attach this Equipment
 * to it."
 *
 * Covers: a creature reanimated from your graveyard gets the Claw; a creature cast from hand
 * doesn't trigger it; and a creature you reanimate from an opponent's graveyard doesn't either,
 * because it entered from *their* graveyard.
 */
class DredgingClawScenarioTest : ScenarioTestBase() {

    private fun board(graveyardOwner: Int) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Dredging Claw")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInHand(1, "Reanimate")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withCardInGraveyard(graveyardOwner, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.clawHost(): EntityId? =
        state.getEntity(findPermanent("Dredging Claw")!!)!!.get<AttachedToComponent>()?.targetId

    init {
        test("a creature returned from your graveyard can take the Claw") {
            val game = board(graveyardOwner = 1)
            game.castSpellTargetingGraveyardCard(1, "Reanimate", 1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()

            game.clawHost() shouldBe game.findPermanent("Hill Giant")
        }

        test("declining leaves the Claw unattached") {
            val game = board(graveyardOwner = 1)
            game.castSpellTargetingGraveyardCard(1, "Reanimate", 1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.clawHost() shouldBe null
        }

        test("a creature cast from hand does not trigger it") {
            val game = board(graveyardOwner = 1)
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.clawHost() shouldBe null
        }

        test("a creature reanimated from an opponent's graveyard does not trigger it") {
            val game = board(graveyardOwner = 2)
            game.castSpellTargetingGraveyardCard(1, "Reanimate", 2, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.clawHost() shouldBe null
        }
    }
}
