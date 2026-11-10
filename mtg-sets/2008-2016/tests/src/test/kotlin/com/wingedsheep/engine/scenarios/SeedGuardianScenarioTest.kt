package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Seed Guardian (OGW #143) — {2}{G}{G} Creature — Elemental, 3/4.
 *
 *   Reach
 *   When this creature dies, create an X/X green Elemental creature token, where X is the number
 *   of creature cards in your graveyard.
 *
 * X is counted on resolution and includes Seed Guardian itself; the token's size is then fixed.
 */
class SeedGuardianScenarioTest : ScenarioTestBase() {

    init {
        test("dying makes an Elemental sized by creature cards in the graveyard, itself included, fixed thereafter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Seed Guardian")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")       // creature card — counts
                .withCardInGraveyard(1, "Lightning Bolt")   // not a creature card
                .withCardInHand(1, "Murder")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val guardian = game.findPermanent("Seed Guardian")!!
            game.castSpell(1, "Murder", guardian).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Seed Guardian") shouldBe true
            val tokens = game.findAllPermanents("Elemental Token")
            withClue("Hill Giant + Seed Guardian itself make a 2/2 Elemental") {
                tokens shouldHaveSize 1
                game.state.projectedState.getPower(tokens.single()) shouldBe 2
                game.state.projectedState.getToughness(tokens.single()) shouldBe 2
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("another creature card hitting the graveyard doesn't grow the Elemental") {
                game.state.projectedState.getPower(tokens.single()) shouldBe 2
                game.state.projectedState.getToughness(tokens.single()) shouldBe 2
            }
        }
    }
}
