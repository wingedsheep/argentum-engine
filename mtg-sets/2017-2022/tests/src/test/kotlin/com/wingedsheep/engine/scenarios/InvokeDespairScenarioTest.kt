package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invoke Despair (NEO) — "Target opponent sacrifices a creature of their choice. If they can't, they
 * lose 2 life and you draw a card. Then repeat this process for an enchantment and a planeswalker."
 *
 * Three rounds against one opponent, each with its own "if they can't" fallback. The rounds share one
 * running "sacrificed this way" tally, so the key case is a creature taken in round one followed by an
 * empty enchantment/planeswalker board: those later rounds must still fire their fallback.
 */
class InvokeDespairScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Invoke Despair")
        .withLandsOnBattlefield(1, "Swamp", 5)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("opponent chooses which creature to sacrifice; a sacrificed creature doesn't mask the empty planeswalker round") {
            val game = base()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Glorious Anthem")
                .build()

            val spell = game.findCardsInHand(1, "Invoke Despair").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.execute(
                CastSpell(game.player1Id, spell, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("casting Invoke Despair should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent, not the caster, picks the creature") { decision.playerId shouldBe game.player2Id }
            decision.options shouldContainExactlyInAnyOrder listOf(bears, giant)
            game.selectCards(listOf(giant))

            game.getPendingDecision() shouldBe null
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            withClue("the lone enchantment is sacrificed without a choice") {
                game.isOnBattlefield("Glorious Anthem") shouldBe false
            }
            withClue("only the planeswalker round fell through: lose 2, draw 1") {
                game.getLifeTotal(2) shouldBe 18
                game.handSize(1) shouldBe 1
            }
        }

        test("an empty board fires every fallback: opponent loses 6 and you draw 3") {
            val game = base().build()
            val spell = game.findCardsInHand(1, "Invoke Despair").single()

            game.execute(CastSpell(game.player1Id, spell, targets = listOf(ChosenTarget.Player(game.player2Id))))
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.getLifeTotal(2) shouldBe 14
            game.handSize(1) shouldBe 3
        }

        test("a planeswalker alone is sacrificed after the creature and enchantment rounds fall through") {
            val game = base()
                .withCardOnBattlefield(2, "Ajani Goldmane")
                .build()
            val spell = game.findCardsInHand(1, "Invoke Despair").single()

            game.execute(CastSpell(game.player1Id, spell, targets = listOf(ChosenTarget.Player(game.player2Id))))
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.isOnBattlefield("Ajani Goldmane") shouldBe false
            game.getLifeTotal(2) shouldBe 16
            game.handSize(1) shouldBe 2
        }
    }
}
