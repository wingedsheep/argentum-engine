package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Seed of Hope (MOM #204) — "Mill two cards. You may put a permanent card from among the milled
 * cards into your hand. You gain 2 life."
 */
class SeedOfHopeScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Seed of Hope")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(2, "Swamp")
        .withLifeTotal(1, 20)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Seed of Hope") {

            test("mills two, returns the chosen permanent card, and gains 2 life") {
                val game = setup()

                game.castSpell(1, "Seed of Hope").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses") { decision.playerId shouldBe game.player1Id }
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                withClue("Lightning Bolt is not a permanent card") {
                    decision.options.contains(bears) shouldBe true
                    decision.options.size shouldBe 1
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returned to hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
                withClue("Lightning Bolt stays milled") { game.isInGraveyard(1, "Lightning Bolt") shouldBe true }
                withClue("gained 2 life") { game.getLifeTotal(1) shouldBe 22 }
            }

            test("declining still mills both cards and gains 2 life") {
                val game = setup()

                game.castSpell(1, "Seed of Hope").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("both milled cards stay in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("gained 2 life") { game.getLifeTotal(1) shouldBe 22 }
            }
        }
    }
}
