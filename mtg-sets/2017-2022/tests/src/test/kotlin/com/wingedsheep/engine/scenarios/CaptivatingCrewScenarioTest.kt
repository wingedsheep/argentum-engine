package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.CaptivatingCrew
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CaptivatingCrewScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + CaptivatingCrew)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activate(crew: EntityId, creature: EntityId) = ActivateAbility(
        playerId = player1,
        sourceId = crew,
        abilityId = CaptivatingCrew.activatedAbilities.single().id,
        targets = listOf(ChosenTarget.Permanent(creature)),
        paymentStrategy = PaymentStrategy.FromPool,
    )

    fun GameTestDriver.fund() {
        giveMana(player1, Color.RED, 1)
        giveColorlessMana(player1, 3)
    }

    test("steals and untaps a creature with haste until cleanup") {
        val d = driver()
        val crew = d.putCreatureOnBattlefield(d.player1, "Captivating Crew")
        val creature = d.putCreatureOnBattlefield(d.player2, "Black Creature")
        d.tapPermanent(creature)
        d.fund()
        // No tap cost: even a newly entered Crew can activate.
        d.submitSuccess(d.activate(crew, creature))
        d.bothPass()
        val projected = StateProjector().project(d.state)
        projected.getController(creature) shouldBe d.player1
        projected.hasKeyword(creature, Keyword.HASTE) shouldBe true
        d.isTapped(creature) shouldBe false

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        val afterCleanup = StateProjector().project(d.state)
        afterCleanup.getController(creature) shouldBe d.player2
        afterCleanup.hasKeyword(creature, Keyword.HASTE) shouldBe false
    }

    test("requires sorcery timing and an opponent's creature but accepts an untapped target") {
        val d = driver()
        val crew = d.putCreatureOnBattlefield(d.player1, "Captivating Crew")
        val creature = d.putCreatureOnBattlefield(d.player2, "Black Creature")
        d.fund()
        d.submitExpectFailure(d.activate(crew, crew))
        d.submitSuccess(d.activate(crew, creature))
        d.fund()
        // The target is still opposing while the first activation is on the stack.
        d.submitExpectFailure(d.activate(crew, creature))
        d.bothPass()
        StateProjector().project(d.state).getController(creature) shouldBe d.player1

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.passPriority(d.player2)
        d.fund()
        d.submitExpectFailure(d.activate(crew, creature))
    }
})
