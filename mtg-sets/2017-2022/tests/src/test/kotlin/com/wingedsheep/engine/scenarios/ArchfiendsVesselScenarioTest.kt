package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Archfiend's Vessel — "When this creature enters, if it entered from your graveyard or you cast
 * it from your graveyard, exile it. If you do, create a 5/5 black Demon creature token with flying."
 *
 * Covers: reanimated from your own graveyard it becomes a Demon; cast from hand it stays a 1/1;
 * and reanimated out of an opponent's graveyard it stays too, because that's not *your* graveyard.
 */
class ArchfiendsVesselScenarioTest : ScenarioTestBase() {

    private fun board(graveyardOwner: Int) = scenario()
        .withPlayers()
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInHand(1, "Reanimate")
        .withCardInHand(1, "Archfiend's Vessel")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withCardInGraveyard(graveyardOwner, "Archfiend's Vessel")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("reanimated from your graveyard, it is exiled and becomes a 5/5 flying Demon") {
            val game = board(graveyardOwner = 1)
            game.castSpellTargetingGraveyardCard(1, "Reanimate", 1, "Archfiend's Vessel").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Archfiend's Vessel") shouldBe false
            game.isInExile(1, "Archfiend's Vessel") shouldBe true
            game.findPermanents("Demon Token") shouldHaveSize 1
        }

        test("cast from hand, it stays on the battlefield and makes no Demon") {
            val game = board(graveyardOwner = 1)
            game.castSpell(1, "Archfiend's Vessel").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Archfiend's Vessel") shouldBe true
            game.findPermanents("Demon Token").shouldBeEmpty()
        }

        test("reanimated from an opponent's graveyard, it stays and makes no Demon") {
            val game = board(graveyardOwner = 2)
            game.castSpellTargetingGraveyardCard(1, "Reanimate", 2, "Archfiend's Vessel").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Archfiend's Vessel") shouldBe true
            game.findPermanents("Demon Token").shouldBeEmpty()
        }
    }
}
