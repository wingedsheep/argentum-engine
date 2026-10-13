package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ButchDeLoriaTunnelSnake
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ButchDeLoriaTunnelSnakeScenarioTest : FunSpec({
    val rogue = card("Butch Test Rogue") { typeLine = "Creature — Human Rogue"; power = 1; toughness = 1 }
    val rogueSnake = card("Butch Test Rogue Snake") { typeLine = "Creature — Snake Rogue"; power = 1; toughness = 1 }
    val snake = card("Butch Test Snake") { typeLine = "Creature — Snake"; power = 1; toughness = 1 }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ButchDeLoriaTunnelSnake, rogue, rogueSnake, snake))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }

    test("attacking gets +1/+1 for each other Rogue and/or Snake you control, a Rogue Snake counted once") {
        val d = setup()
        val butch = d.putCreatureOnBattlefield(d.player1, ButchDeLoriaTunnelSnake.name).also(d::removeSummoningSickness)
        d.putCreatureOnBattlefield(d.player1, rogue.name)
        d.putCreatureOnBattlefield(d.player1, rogueSnake.name)
        d.putCreatureOnBattlefield(d.player1, snake.name)
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, rogue.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(butch), d.player2).error shouldBe null
        resolve(d)
        d.state.projectedState.getPower(butch) shouldBe 5
        d.state.projectedState.getToughness(butch) shouldBe 5
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getPower(butch) shouldBe 2
    }

    test("the activated ability puts a menace counter on another creature and makes it a Rogue for good") {
        val d = setup()
        val butch = d.putCreatureOnBattlefield(d.player1, ButchDeLoriaTunnelSnake.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val abilityId = ButchDeLoriaTunnelSnake.activatedAbilities.single().id
        d.giveMana(d.player1, Color.BLACK, 2)
        d.submit(ActivateAbility(d.player1, butch, abilityId, targets = listOf(ChosenTarget.Permanent(butch))))
            .error.shouldNotBeNull()
        d.submit(ActivateAbility(d.player1, butch, abilityId, targets = listOf(ChosenTarget.Permanent(bears))))
            .error shouldBe null
        resolve(d)
        d.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.MENACE) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
        d.state.projectedState.hasSubtype(bears, "Rogue") shouldBe true
        d.state.projectedState.hasSubtype(bears, "Bear") shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasSubtype(bears, "Rogue") shouldBe true
    }
})
