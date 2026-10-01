package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ezuri, Stalker of Spheres (ONE #201) — {2}{G}{U} 3/3 Legendary Creature — Phyrexian Elf Warrior.
 *
 *   When Ezuri enters, you may pay {3}. If you do, proliferate twice.
 *   Whenever you proliferate, draw a card.
 */
class EzuriStalkerOfSpheresScenarioTest : ScenarioTestBase() {

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    /** Resolve everything, answering each proliferate prompt by choosing [pick]. Returns prompt count. */
    private fun drain(game: TestGame, pick: EntityId): Int {
        var prompts = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 30) {
            if (game.hasPendingDecision()) { game.selectCards(listOf(pick)); prompts++ } else game.resolveStack()
        }
        return prompts
    }

    init {
        cardRegistry.register(spreading)

        test("paying {3} on entry proliferates twice and draws a card for each") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ezuri, Stalker of Spheres")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Ezuri, Stalker of Spheres").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            val handBefore = game.handSize(1)
            drain(game, bears) shouldBe 2

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            game.handSize(1) shouldBe handBefore + 2
        }

        test("declining the payment does not proliferate or draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ezuri, Stalker of Spheres")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Ezuri, Stalker of Spheres").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            val handBefore = game.handSize(1)
            game.answerYesNo(false)
            game.resolveStack()

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.handSize(1) shouldBe handBefore
        }

        test("any proliferate you do draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ezuri, Stalker of Spheres")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val handBefore = game.handSize(1)
            drain(game, bears) shouldBe 1

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.handSize(1) shouldBe handBefore + 1
        }
    }
}
