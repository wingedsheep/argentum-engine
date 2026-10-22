package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Bloodthirsty Adversary (Innistrad: Midnight Hunt #129).
 *
 * When this creature enters, you may pay {2}{R} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then exile up to that many target
 * instant and/or sorcery cards with mana value 3 or less from your graveyard and copy them. You
 * may cast any number of the copies without paying their mana costs.
 *
 * Proves the reflexive half's graveyard targets are capped by the repetition count, filtered to
 * mana value 3 or less, exiled, copied, and that the copies are cast for free (or not at all).
 */
class BloodthirstyAdversaryScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Cast the Adversary off two Mountains, leaving [spare] Mountains untapped, and say yes. */
    private fun castAdversary(spare: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Bloodthirsty Adversary")
            .withLandsOnBattlefield(1, "Mountain", spare + 2)
            .withCardInGraveyard(1, "Shock")
            .withCardInGraveyard(1, "Lightning Bolt")
            .withCardInGraveyard(1, "Lava Axe")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Bloodthirsty Adversary").error shouldBe null
        game.resolveStack()
        game.answerYesNo(true)
        return game
    }

    /**
     * Drive the reflexive trigger to completion: cast every offered copy whose name is in
     * [castNames] (aiming each at the opponent), decline the rest.
     */
    private fun TestGame.resolveCasting(castNames: Set<String>) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            when (val decision = state.pendingDecision) {
                is SelectCardsDecision -> {
                    val pick = decision.options.firstOrNull { nameOf(it) in castNames }
                    selectCards(listOfNotNull(pick))
                }
                is ChooseTargetsDecision -> selectTargets(listOf(player2Id))
                null -> passPriority()
                else -> error("unexpected decision $decision")
            }
            guard++
        }
    }

    init {
        test("paying twice gives two counters and casts free copies of two cheap spells") {
            val game = castAdversary(spare = 6)
            val count = game.getPendingDecision() as? ChooseNumberDecision
                ?: error("expected the repeat-count question; got ${game.getPendingDecision()}")
            count.maxValue shouldBe 2
            game.chooseNumber(2)

            val shock = game.findCardsInGraveyard(1, "Shock").single()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val axe = game.findCardsInGraveyard(1, "Lava Axe").single()
            withClue("Lava Axe has mana value 5 — not a legal target") {
                game.selectTargets(listOf(axe)).error shouldNotBe null
            }
            withClue("three targets exceed the two payments") {
                game.selectTargets(listOf(shock, bolt, axe)).error shouldNotBe null
            }
            game.selectTargets(listOf(shock, bolt)).error shouldBe null
            game.resolveCasting(setOf("Shock", "Lightning Bolt"))

            val adversary = game.findPermanent("Bloodthirsty Adversary")!!
            game.plusCounters(adversary) shouldBe 2
            withClue("free Shock (2) + free Lightning Bolt (3)") {
                game.getLifeTotal(2) shouldBe 15
            }
            withClue("the originals stay in exile; uncast/resolved copies don't linger") {
                game.isInExile(1, "Shock") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
                game.state.getExile(game.player1Id).size shouldBe 2
            }
            game.isInGraveyard(1, "Lava Axe") shouldBe true
        }

        test("declining to cast the copy still exiles the original") {
            val game = castAdversary(spare = 3)
            game.chooseNumber(1)

            val shock = game.findCardsInGraveyard(1, "Shock").single()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            withClue("two targets exceed the single payment") {
                game.selectTargets(listOf(shock, bolt)).error shouldNotBe null
            }
            game.selectTargets(listOf(shock)).error shouldBe null
            game.resolveCasting(emptySet())

            val adversary = game.findPermanent("Bloodthirsty Adversary")!!
            game.plusCounters(adversary) shouldBe 1
            game.getLifeTotal(2) shouldBe 20
            game.isInExile(1, "Shock") shouldBe true
            game.state.getExile(game.player1Id).size shouldBe 1
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
        }
    }
}
