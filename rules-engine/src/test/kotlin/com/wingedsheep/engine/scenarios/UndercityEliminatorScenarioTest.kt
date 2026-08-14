package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Undercity Eliminator (MKM) — "When this creature enters, you may sacrifice an artifact or
 * creature. When you do, exile target creature an opponent controls."
 *
 * "**When** you do" is a CR 603.12 reflexive trigger, and what these tests pin is the resulting
 * order: the enters trigger resolves into a bare yes/no with no target chosen, the sacrifice
 * happens, and only *then* does a second ability go on the stack and ask for its target.
 */
class UndercityEliminatorScenarioTest : ScenarioTestBase() {

    init {
        test("sacrificing a creature exiles a creature an opponent controls") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Undercity Eliminator")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Undercity Eliminator").error shouldBe null
            game.resolveStack()

            // CR 603.12: the enters trigger asks only "do you want to sacrifice?" — no target yet.
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            // Two legal fodder permanents (the Bears and the Eliminator itself) — pick the Bears.
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            // Now — and only now — the reflexive ability wants its target.
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
            game.selectTargets(listOf(courser)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Centaur Courser") shouldBe true
            game.isOnBattlefield("Undercity Eliminator") shouldBe true
        }

        test("declining the sacrifice exiles nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Undercity Eliminator")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Undercity Eliminator").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isInExile(2, "Centaur Courser") shouldBe false
        }

        test("the Eliminator is legal fodder for its own ability") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Undercity Eliminator")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Undercity Eliminator").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            // Sole legal choice — it eats itself with no prompt.
            game.resolveStack()

            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
            game.selectTargets(listOf(courser)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Undercity Eliminator") shouldBe true
            game.isInExile(2, "Centaur Courser") shouldBe true
        }
    }
}
