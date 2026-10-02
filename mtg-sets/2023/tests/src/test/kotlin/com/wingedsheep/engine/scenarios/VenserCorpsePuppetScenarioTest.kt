package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Venser, Corpse Puppet (ONE #219) — {U}{B} 1/3 Legendary Creature — Phyrexian Zombie Wizard.
 *
 *   Lifelink, toxic 1
 *   Whenever you proliferate, choose one —
 *   • If you don't control a creature named The Hollow Sentinel, create The Hollow Sentinel, a
 *     legendary 3/3 colorless Phyrexian Golem artifact creature token.
 *   • Target artifact creature you control gains flying and lifelink until end of turn.
 */
class VenserCorpsePuppetScenarioTest : ScenarioTestBase() {

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private val construct = card("Test Construct") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 2
        toughness = 2
    }

    private fun TestGame.proliferateAndChoose(mode: String) {
        castSpell(1, "Test Spreading").error shouldBe null
        var guard = 0
        while (guard++ < 20) {
            val decision = getPendingDecision()
            when {
                decision is ChooseOptionDecision -> {
                    val index = decision.options.indexOfFirst { it.contains(mode) }
                    check(index >= 0) { "Mode '$mode' not offered; options=${decision.options}" }
                    submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
                    return
                }
                decision != null -> selectCards(emptyList())
                state.stack.isNotEmpty() -> resolveStack()
                else -> error("no mode choice was offered")
            }
        }
    }

    private fun TestGame.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 20) {
            if (hasPendingDecision()) selectCards(emptyList()) else resolveStack()
        }
    }

    private fun board(vararg extra: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Venser, Corpse Puppet")
        .apply { extra.forEach { withCardOnBattlefield(1, it) } }
        .withCardInHand(1, "Test Spreading")
        .withCardInHand(1, "Test Spreading")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(spreading)
        cardRegistry.register(construct)

        test("Venser has lifelink") {
            val game = board()
            val venser = game.findPermanent("Venser, Corpse Puppet")!!
            game.state.projectedState.hasKeyword(venser, Keyword.LIFELINK) shouldBe true
        }

        test("first mode creates The Hollow Sentinel, a legendary 3/3 colorless artifact creature") {
            val game = board()
            game.proliferateAndChoose("Hollow Sentinel")
            game.drain()

            val sentinels = game.findPermanents("The Hollow Sentinel")
            sentinels shouldHaveSize 1
            val sentinel = sentinels.single()
            val card = game.state.getEntity(sentinel)!!.get<CardComponent>()!!
            card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)
            card.typeLine.isLegendary shouldBe true
            card.typeLine.subtypes.map { it.value }.toSet() shouldBe setOf("Phyrexian", "Golem")
            card.colors shouldBe emptySet()
            game.state.projectedState.getPower(sentinel) shouldBe 3
            game.state.projectedState.getToughness(sentinel) shouldBe 3
        }

        test("first mode creates nothing while you already control The Hollow Sentinel") {
            val game = board()
            game.proliferateAndChoose("Hollow Sentinel")
            game.drain()
            val first = game.findPermanents("The Hollow Sentinel").single()

            game.proliferateAndChoose("Hollow Sentinel")
            // A second token would trip the legend rule and pause for a choice, so drain without
            // answering decisions: none may appear.
            var guard = 0
            while (game.state.stack.isNotEmpty() && guard++ < 20) {
                game.hasPendingDecision() shouldBe false
                game.resolveStack()
            }
            withClue("the second resolution sees the existing Sentinel and does nothing") {
                game.hasPendingDecision() shouldBe false
                game.findPermanents("The Hollow Sentinel") shouldBe listOf(first)
            }
        }

        test("second mode grants flying and lifelink to target artifact creature you control") {
            val game = board("Test Construct")
            val construct = game.findPermanent("Test Construct")!!
            game.proliferateAndChoose("gains flying and lifelink")
            if (game.hasPendingDecision()) game.selectTargets(listOf(construct)).error shouldBe null
            game.drain()

            game.state.projectedState.hasKeyword(construct, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(construct, Keyword.LIFELINK) shouldBe true
        }
    }
}
