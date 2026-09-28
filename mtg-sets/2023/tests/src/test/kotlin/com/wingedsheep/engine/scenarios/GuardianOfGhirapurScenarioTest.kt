package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Guardian of Ghirapur (MOM #16) — "When this creature enters, exile up to one other target
 * creature or artifact you control. Return it to the battlefield under its owner's control at the
 * beginning of the next end step."
 */
class GuardianOfGhirapurScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Guardian of Ghirapur")
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("blinks an artifact you control: exiled now, back at the beginning of the end step") {
            val game = board()
            game.castSpell(1, "Guardian of Ghirapur").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets.values.flatten().toSet()
            withClue("only other creatures/artifacts you control are legal") {
                legal.contains(game.findPermanent("Ornithopter")!!) shouldBe true
                legal.contains(game.findPermanent("Grizzly Bears")!!) shouldBe true
                legal.contains(game.findPermanent("Hill Giant")!!) shouldBe false
                legal.contains(game.findPermanent("Guardian of Ghirapur")!!) shouldBe false
            }

            game.selectTargets(listOf(game.findPermanent("Ornithopter")!!))
            game.resolveStack()

            game.isInExile(1, "Ornithopter") shouldBe true
            game.isOnBattlefield("Ornithopter") shouldBe false

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the delayed trigger returned the artifact") {
                game.isOnBattlefield("Ornithopter") shouldBe true
                game.isInExile(1, "Ornithopter") shouldBe false
            }
        }

        test("up to one: choosing no target exiles nothing") {
            val game = board()
            game.castSpell(1, "Guardian of Ghirapur").error shouldBe null
            game.resolveStack()
            if (game.getPendingDecision() is ChooseTargetsDecision) game.skipTargets()
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Guardian of Ghirapur") shouldBe true
        }
    }
}
