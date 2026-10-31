package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.FloodOfTears
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flood of Tears (M20 #59) — "Return all nonland permanents to their owners' hands. If you return
 * four or more nontoken permanents you control this way, you may put a permanent card from your
 * hand onto the battlefield."
 *
 * Lands stay; every nonland permanent on both sides goes home. Only *your nontoken* permanents
 * count toward four — a token or an opponent's permanent doesn't — and the card put back may be
 * one that was just returned.
 */
class FloodOfTearsScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(FloodOfTears)

        context("Flood of Tears") {

            fun build(yours: List<String>, yourTokens: List<String> = emptyList()) = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Flood of Tears")
                .withLandsOnBattlefield(1, "Island", 6)
                .apply { yours.forEach { withCardOnBattlefield(1, it) } }
                .apply { yourTokens.forEach { withCardOnBattlefield(1, it, isToken = true) } }
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("four of your nontoken permanents returned lets you put one of them back") {
                val game = build(listOf("Grizzly Bears", "Hill Giant", "Air Elemental", "Ornithopter"))

                game.castSpell(1, "Flood of Tears").error shouldBe null
                game.resolveStack()

                withClue("every nonland permanent was returned, lands stayed") {
                    game.findPermanents("Grizzly Bears").size shouldBe 0
                    game.findPermanents("Hill Giant").size shouldBe 0
                    game.findPermanents("Air Elemental").size shouldBe 0
                    game.findPermanents("Island").size shouldBe 6
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isInHand(2, "Hill Giant") shouldBe true
                }

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val elemental = game.findCardsInHand(1, "Air Elemental").first()
                game.selectCards(listOf(elemental)).error shouldBe null
                game.resolveStack()

                withClue("a card returned this way may be the one put onto the battlefield") {
                    game.findPermanent("Air Elemental") shouldNotBe null
                    game.isInHand(1, "Air Elemental") shouldBe false
                }
                withClue("the opponent's creatures stay in their hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
            }

            test("tokens and opponents' permanents don't count toward four") {
                val game = build(
                    yours = listOf("Grizzly Bears", "Hill Giant", "Air Elemental"),
                    yourTokens = listOf("Ornithopter")
                )

                game.castSpell(1, "Flood of Tears").error shouldBe null
                game.resolveStack()

                withClue("three nontoken permanents of yours is not enough — no put is offered") {
                    game.hasPendingDecision() shouldBe false
                }
                game.findPermanents("Grizzly Bears").size shouldBe 0
                game.findPermanents("Ornithopter").size shouldBe 0
                game.isInHand(1, "Air Elemental") shouldBe true
                game.isInHand(2, "Hill Giant") shouldBe true
            }
        }
    }
}
