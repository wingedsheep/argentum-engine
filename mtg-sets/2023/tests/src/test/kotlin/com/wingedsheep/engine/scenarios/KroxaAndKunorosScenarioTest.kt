package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kroxa and Kunoros (MOM #245) — {3}{R}{W}{B} Legendary Creature — Elder Giant Dog 6/6.
 *
 *   Vigilance, menace, lifelink
 *   Whenever Kroxa and Kunoros enters or attacks, you may exile five cards from your graveyard.
 *   When you do, return target creature card from your graveyard to the battlefield.
 */
class KroxaAndKunorosScenarioTest : ScenarioTestBase() {

    private val fodder = listOf("Forest", "Mountain", "Plains", "Island", "Lightning Bolt")

    private fun attackScenario(graveyard: List<String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Kroxa and Kunoros", summoningSickness = false)
        .apply { graveyard.forEach { withCardInGraveyard(1, it) } }
        .withCardInGraveyard(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("has vigilance, menace and lifelink") {
            val game = attackScenario(emptyList())
            val kroxa = game.findPermanent("Kroxa and Kunoros")!!
            val projected = game.state.projectedState
            projected.hasKeyword(kroxa, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(kroxa, Keyword.MENACE) shouldBe true
            projected.hasKeyword(kroxa, Keyword.LIFELINK) shouldBe true
        }

        test("attacking and exiling five cards returns a creature card from your graveyard") {
            val game = attackScenario(fodder + "Grizzly Bears")
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kroxa and Kunoros" to 2)).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 10) game.passPriority()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val toExile = fodder.map { game.findCardsInGraveyard(1, it).single() }
            game.selectCards(toExile).error shouldBe null

            guard = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) game.passPriority()
            val td = game.state.pendingDecision as ChooseTargetsDecision
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val legal = td.legalTargets[0].orEmpty()
            withClue("only creature cards in your own graveyard are legal") {
                legal shouldContain bears
                legal shouldNotContain game.findCardsInGraveyard(2, "Hill Giant").single()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.getGraveyard(game.player1Id).size shouldBe 0
            fodder.forEach { game.isInGraveyard(1, it) shouldBe false }
        }

        test("declining leaves the graveyard alone") {
            val game = attackScenario(fodder + "Grizzly Bears")
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kroxa and Kunoros" to 2)).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 10) game.passPriority()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.state.getGraveyard(game.player1Id).size shouldBe 6
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("with fewer than five cards in your graveyard nothing is exiled or returned") {
            val game = attackScenario(listOf("Forest", "Mountain", "Plains", "Grizzly Bears"))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kroxa and Kunoros" to 2)).error shouldBe null

            var guard = 0
            while (game.state.stack.isNotEmpty() && game.state.pendingDecision == null && guard++ < 10) {
                game.passPriority()
            }
            withClue("no exile offer can be made with only four cards") {
                (game.state.pendingDecision is YesNoDecision) shouldBe false
            }
            game.state.getGraveyard(game.player1Id).size shouldBe 4
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("the enters trigger also offers the exile") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Kroxa and Kunoros")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .apply { (fodder + "Grizzly Bears").forEach { withCardInGraveyard(1, it) } }
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Kroxa and Kunoros").error shouldBe null
            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 10) game.passPriority()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.selectCards(fodder.map { game.findCardsInGraveyard(1, it).single() }).error shouldBe null

            guard = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) game.passPriority()
            game.selectTargets(listOf(game.findCardsInGraveyard(1, "Grizzly Bears").single())).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
