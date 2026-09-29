package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wildwood Escort (MOM #216): "When this creature enters, return target creature or battle card
 * from your graveyard to your hand. If this creature would die, exile it instead."
 */
class WildwoodEscortScenarioTest : ScenarioTestBase() {

    private fun castEscort(vararg graveyard: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Wildwood Escort")
        .withLandsOnBattlefield(1, "Forest", 5)
        .apply { graveyard.forEach { withCardInGraveyard(1, it) } }
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.castSpell(1, "Wildwood Escort").error shouldBe null
            game.resolveStack()
        }

    init {
        test("enters: returns a battle card from your graveyard to your hand") {
            val game = castEscort("Invasion of Shandalar", "Giant Growth")
            val battle = game.findCardsInGraveyard(1, "Invasion of Shandalar").single()
            game.selectTargets(listOf(battle)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Invasion of Shandalar") shouldBe true
            game.isInGraveyard(1, "Giant Growth") shouldBe true
        }

        test("enters: returns a creature card from your graveyard to your hand") {
            val game = castEscort("Grizzly Bears")
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("no creature or battle card in the graveyard: trigger has no legal target") {
            val game = castEscort("Giant Growth")
            withClue("the trigger is removed for lack of a target") {
                game.hasPendingDecision() shouldBe false
            }
            game.isOnBattlefield("Wildwood Escort") shouldBe true
            game.isInGraveyard(1, "Giant Growth") shouldBe true
        }

        test("if it would die, it is exiled instead") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wildwood Escort")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val escort = game.findPermanent("Wildwood Escort")!!
            game.castSpell(1, "Lightning Bolt", escort).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wildwood Escort") shouldBe false
            game.isInGraveyard(1, "Wildwood Escort") shouldBe false
            game.isInExile(1, "Wildwood Escort") shouldBe true
        }
    }
}
