package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.BoggartSpriteChaser
import com.wingedsheep.mtg.sets.definitions.lrw.cards.Pestermite
import com.wingedsheep.mtg.sets.definitions.lrw.cards.TarPitcher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sacrifice and discard costs are paid with the candidate worth least to the AI, not the first
 * one the enumerator lists. 2026-10-12 AI-vs-AI logs (2p game 1, T18): Tar Pitcher ("{T},
 * Sacrifice a Goblin") sacrificed a flying 2/3 Boggart Sprite-Chaser to ping a recursive 1/1
 * when Tar Pitcher itself was a legal, cheaper Goblin to give up.
 */
class CheapestCostCandidateAiTest : FunSpec({
    val cards = TestCards.all + listOf(TarPitcher, BoggartSpriteChaser, Pestermite)

    fun driver() = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Tar Pitcher sacrifices itself rather than a flying Sprite-Chaser") {
        val driver = driver()
        val player = driver.player1
        val chaser = driver.putCreatureOnBattlefield(player, "Boggart Sprite-Chaser")
        driver.putCreatureOnBattlefield(player, "Pestermite")
        val pitcher = driver.putCreatureOnBattlefield(player, "Tar Pitcher")
        driver.removeSummoningSickness(pitcher)
        driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.state.projectedState.hasKeyword(chaser, "FLYING") shouldBe true

        val registry = CardRegistry().apply { register(cards) }
        val activation = GameSimulator(registry).getLegalActions(driver.state, player)
            .first { (it.action as? ActivateAbility)?.sourceId == pitcher }
        // The enumerator lists the Sprite-Chaser first, so "take the first" is the bug this pins.
        activation.additionalCostInfo!!.validSacrificeTargets shouldBe listOf(chaser, pitcher)

        val chosen = AIPlayer.create(registry, player)
            .chooseFrom(driver.state, listOf(activation)).action as ActivateAbility
        chosen.costPayment?.sacrificedPermanents shouldBe listOf(pitcher)
    }

    test("a discard cost pitches the surplus land, not the castable spell") {
        val driver = driver()
        val player = driver.player1
        repeat(7) { driver.putLandOnBattlefield(player, "Mountain") }
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val giant = driver.putCardInHand(player, "Hill Giant")
        val land = driver.putCardInHand(player, "Mountain")
        val legal = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpell",
            description = "test",
            additionalCostInfo = AdditionalCostData(
                "discard", "DiscardCard", validDiscardTargets = listOf(giant, land), discardCount = 1,
            ),
        )

        val chosen = AIPlayer.create(driver.cardRegistry, player)
            .chooseFrom(driver.state, listOf(legal)).action as CastSpell
        chosen.additionalCostPayment?.discardedCards shouldBe listOf(land)
    }
})
