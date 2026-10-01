package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Green Sun's Twilight ({X}{G}, Sorcery):
 * "Reveal the top X plus one cards of your library. Choose a creature card and/or a land card from
 *  among them. Put those cards into your hand and the rest on the bottom of your library in a random
 *  order. If X is 5 or more, instead put the chosen cards onto the battlefield or into your hand and
 *  the rest on the bottom of your library in a random order."
 */
class GreenSunsTwilightScenarioTest : ScenarioTestBase() {

    private fun TestGame.cast(xValue: Int) {
        val cardId = state.getHand(player1Id).find {
            state.getEntity(it)?.get<CardComponent>()?.name == "Green Sun's Twilight"
        } ?: error("Green Sun's Twilight not in hand")
        execute(CastSpell(player1Id, cardId, emptyList(), xValue)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.pick(name: String) {
        getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        selectCards(listOf(findCardsInLibrary(1, name).single())).error shouldBe null
    }

    private fun library(builder: ScenarioBuilder, vararg names: String): ScenarioBuilder {
        names.forEach { builder.withCardInLibrary(1, it) }
        return builder
    }

    init {
        test("X=2 reveals three cards; chosen creature and land go to hand, the rest to the library bottom") {
            val game = library(
                scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Green Sun's Twilight")
                    .withLandsOnBattlefield(1, "Forest", 3),
                "Grizzly Bears", "Mountain", "Hill Giant", "Forest"
            )
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()

            game.cast(2)
            game.pick("Grizzly Bears")
            game.pick("Mountain")

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Mountain") shouldBe true
            withClue("the unchosen card goes under the unrevealed Forest") {
                game.state.getLibrary(game.player1Id).last() shouldBe giant
            }
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("X=5 — choosing the battlefield puts both chosen cards onto the battlefield") {
            val game = library(
                scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Green Sun's Twilight")
                    .withLandsOnBattlefield(1, "Forest", 6),
                "Grizzly Bears", "Mountain", "Hill Giant", "Plains", "Craw Wurm", "Island"
            )
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast(5)
            game.pick("Craw Wurm")
            game.pick("Mountain")
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.isOnBattlefield("Mountain") shouldBe true
            game.isInHand(1, "Craw Wurm") shouldBe false
            game.state.getLibrary(game.player1Id).size shouldBe 4
        }

        test("X=5 — declining the battlefield puts both chosen cards into hand") {
            val game = library(
                scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Green Sun's Twilight")
                    .withLandsOnBattlefield(1, "Forest", 6),
                "Grizzly Bears", "Mountain", "Hill Giant", "Plains", "Craw Wurm", "Island"
            )
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast(5)
            game.pick("Hill Giant")
            game.pick("Island")
            game.answerYesNo(false).error shouldBe null

            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInHand(1, "Island") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false
        }
    }
}
