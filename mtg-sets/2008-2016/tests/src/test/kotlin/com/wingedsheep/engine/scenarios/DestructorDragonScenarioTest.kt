package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Destructor Dragon (Fate Reforged #127) — {4}{G}{G} Creature — Dragon 4/4.
 *
 * "Flying. When this creature dies, destroy target noncreature permanent."
 */
class DestructorDragonScenarioTest : ScenarioTestBase() {
    init {
        test("dying destroys target noncreature permanent; creatures aren't legal targets") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Destructor Dragon")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Forest")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Doom Blade", game.findPermanent("Destructor Dragon")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Destructor Dragon") shouldBe true

            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val forest = game.findPermanent("Forest")!!
            val legal = decision.legalTargets[0].orEmpty()
            legal shouldContain forest
            legal shouldNotContain game.findPermanent("Grizzly Bears")!!

            game.selectTargets(listOf(forest)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Forest") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
