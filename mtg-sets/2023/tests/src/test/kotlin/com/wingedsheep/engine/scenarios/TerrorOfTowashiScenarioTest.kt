package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
 * Terror of Towashi (MOM #331) — {2}{B}{B} Creature — Phyrexian Ogre 4/3.
 *
 *   Deathtouch
 *   Whenever this creature attacks, you may pay {3}{B}. When you do, return target creature card
 *   from your graveyard to the battlefield. It's a Phyrexian in addition to its other types.
 */
class TerrorOfTowashiScenarioTest : ScenarioTestBase() {

    private fun baseScenario() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Terror of Towashi", summoningSickness = false)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardInGraveyard(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("has deathtouch") {
            val game = baseScenario()
            val terror = game.findPermanent("Terror of Towashi")!!
            game.state.projectedState.hasKeyword(terror, Keyword.DEATHTOUCH) shouldBe true
        }

        test("paying {3}{B} on attack returns a creature card from your graveyard as a Phyrexian") {
            val game = baseScenario()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Terror of Towashi" to 2)).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 10) game.passPriority()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.state.pendingDecision is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()

            guard = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) game.passPriority()
            val td = game.state.pendingDecision as ChooseTargetsDecision
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val legal = td.legalTargets[0].orEmpty()
            withClue("only creature cards in your own graveyard are legal") {
                legal shouldContain bears
                legal shouldNotContain game.findCardsInGraveyard(1, "Lightning Bolt").single()
                legal shouldNotContain game.findCardsInGraveyard(2, "Hill Giant").single()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            val onField = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasSubtype(onField, "Phyrexian") shouldBe true
            game.state.projectedState.hasSubtype(onField, "Bear") shouldBe true
        }

        test("declining the payment leaves the graveyard alone") {
            val game = baseScenario()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Terror of Towashi" to 2)).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 10) game.passPriority()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.state.getBattlefield().count {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Swamp" &&
                    game.state.getEntity(it)?.get<TappedComponent>() != null
            } shouldBe 0
        }
    }
}
