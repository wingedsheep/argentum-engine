package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Clash of Wills (ORI #49) — {X}{U} Instant.
 *
 *   Counter target spell unless its controller pays {X}.
 *
 * Player 1 casts Lightning Bolt at Player 2; Player 2 answers with Clash of Wills for X = 2.
 */
class ClashOfWillsScenarioTest : ScenarioTestBase() {

    /** Bolt is on the stack aimed at Player 2, and Clash of Wills (X = 2) has been cast on it. */
    private fun boltThenClash(mountains: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withCardInHand(2, "Clash of Wills")
            .withLandsOnBattlefield(2, "Island", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val bolt = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
        withClue("Bolt should be cast: ${bolt.error}") { bolt.error shouldBe null }
        game.passPriority().error shouldBe null

        val boltId = game.state.stack.single()
        val clash = game.state.getHand(game.player2Id).single {
            game.state.getEntity(it)?.get<CardComponent>()?.name == "Clash of Wills"
        }
        val cast = game.execute(
            CastSpell(game.player2Id, clash, listOf(ChosenTarget.Spell(boltId)), xValue = 2)
        )
        withClue("Clash of Wills should be cast: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()
        return game
    }

    init {
        context("Clash of Wills") {

            test("counters the spell when its controller can't pay {X}") {
                val game = boltThenClash(mountains = 1)

                withClue("no payment prompt for a player with no untapped mana") {
                    game.state.pendingDecision shouldBe null
                }
                game.state.stack shouldBe emptyList()
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(2, "Clash of Wills") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("counters the spell when its controller declines to pay {X}") {
                val game = boltThenClash(mountains = 3)

                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player1Id
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("the spell resolves when its controller pays {X}") {
                val game = boltThenClash(mountains = 3)

                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player1Id
                game.answerYesNo(true).error shouldBe null
                withClue("{X} is paid from the caster's untapped lands") {
                    game.state.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
                    game.submitManaSourcesAutoPay().error shouldBe null
                }
                game.resolveStack()

                withClue("Lightning Bolt resolved for 3") { game.getLifeTotal(2) shouldBe 17 }
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(2, "Clash of Wills") shouldBe true
            }
        }
    }
}
