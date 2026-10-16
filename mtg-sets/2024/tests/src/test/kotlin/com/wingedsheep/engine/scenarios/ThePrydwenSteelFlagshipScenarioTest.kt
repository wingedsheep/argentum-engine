package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ThePrydwenSteelFlagship
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The Prydwen, Steel Flagship — another nontoken artifact entering mints a Human Knight whose own
 * conditional static reads the controller's artifact-entered-this-turn log: 4/4 the turn it is
 * made, 2/2 on a later turn with no artifact entry. The Prydwen entering doesn't trigger itself.
 */
class ThePrydwenSteelFlagshipScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + ThePrydwenSteelFlagship)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    fun knights(d: GameTestDriver): List<EntityId> = d.state.getBattlefield().filter {
        d.state.getEntity(it)?.get<CardComponent>()?.name == "Human Knight Token"
    }

    test("another nontoken artifact entering creates a Knight that is 4/4 this turn and 2/2 later") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player1, ThePrydwenSteelFlagship.name)
        val artifact = d.putCardInHand(d.player1, "Artifact Creature")
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, artifact).error shouldBe null
        resolve(d)

        val knight = knights(d).also { it shouldHaveSize 1 }.single()
        d.state.projectedState.getPower(knight) shouldBe 4
        d.state.projectedState.getToughness(knight) shouldBe 4

        // Opponent's turn: no artifact entered under player 1's control, so the Knight is 2/2.
        d.passPriorityUntil(Step.UPKEEP)
        d.state.activePlayerId shouldBe d.player2
        d.state.projectedState.getPower(knight) shouldBe 2
        d.state.projectedState.getToughness(knight) shouldBe 2
    }

    test("The Prydwen entering doesn't trigger its own ability") {
        val d = setup()
        val prydwen = d.putCardInHand(d.player1, ThePrydwenSteelFlagship.name)
        d.giveMana(d.player1, Color.WHITE, 2)
        d.giveColorlessMana(d.player1, 4)
        d.castSpell(d.player1, prydwen).error shouldBe null
        resolve(d)

        (d.findPermanent(d.player1, ThePrydwenSteelFlagship.name) != null) shouldBe true
        knights(d).shouldBeEmpty()
    }
})
