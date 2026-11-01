package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Storrev, Devkarin Lich (WAR #219) — {1}{B}{B}{G} 5/4 trample.
 *
 *   Whenever Storrev deals combat damage to a player or planeswalker, return to your hand target
 *   creature or planeswalker card in your graveyard that wasn't put there this combat.
 */
class StorrevDevkarinLichScenarioTest : ScenarioTestBase() {

    init {
        test("returns a card that was already in the graveyard, but not one that died this combat") {
            val game = scenario()
                .withPlayers("Storrev", "Defender")
                .withCardOnBattlefield(1, "Storrev, Devkarin Lich", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInGraveyard(1, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Storrev, Devkarin Lich" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null

            var guard = 0
            while (game.getPendingDecision() !is ChooseTargetsDecision && guard++ < 10) game.passPriority()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("Grizzly Bears died this combat, so it can't be chosen") {
                choice.legalTargets[0]!! shouldNotContain bears
            }
            choice.legalTargets[0]!! shouldContain giant

            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 15
        }
    }
}
