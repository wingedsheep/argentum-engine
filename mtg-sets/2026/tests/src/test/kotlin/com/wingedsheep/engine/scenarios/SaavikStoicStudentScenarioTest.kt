package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Saavik, Stoic Student ({4}{W} Legendary Creature — Vulcan Advisor, 2/2):
 * "Flash
 * Federation — When Saavik enters, she deals X damage to target attacking or blocking creature an
 * opponent controls, where X is the number of creature types among non-Borg creatures you control."
 */
class SaavikStoicStudentScenarioTest : ScenarioTestBase() {

    init {
        test("flashed in during combat, she deals X damage to an attacking opponent creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Saavik, Stoic Student")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Assembly Specialist")
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val giant = game.findPermanent("Hill Giant")!!

            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passPriority()
            game.castSpell(1, "Saavik, Stoic Student").error shouldBe null
            game.resolveStack()

            // The enters trigger targets a creature, so it prompts even with one legal target.
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("Vulcan + Advisor + Bear = 3 damage (the Borg adds nothing) kills the 3/3 Hill Giant") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }
    }
}
