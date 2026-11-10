package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Immolating Glare — {1}{W} Instant. "Destroy target attacking creature."
 */
class ImmolatingGlareScenarioTest : ScenarioTestBase() {
    init {
        test("destroys an attacking creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Immolating Glare")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passPriority() // the attacker passes; the defender gets priority

            val cast = game.castSpell(1, "Immolating Glare", bears)
            withClue("an attacking creature is a legal target: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("a creature that isn't attacking can't be targeted") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Immolating Glare")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Immolating Glare", bears).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
