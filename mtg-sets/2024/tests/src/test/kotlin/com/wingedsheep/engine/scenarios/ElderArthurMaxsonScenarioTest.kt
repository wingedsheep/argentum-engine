package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ElderArthurMaxson
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Elder Arthur Maxson — training granted by a static to creature tokens you control (not to
 * nontoken creatures), and the sacrifice-another-creature indestructible ability.
 */
class ElderArthurMaxsonScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + ElderArthurMaxson)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    fun plusOnes(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    fun ready(d: GameTestDriver, name: String) =
        d.putCreatureOnBattlefield(d.player1, name).also(d::removeSummoningSickness)

    test("a creature token you control trains; a nontoken creature doesn't") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, ElderArthurMaxson.name)
        val token = ready(d, "Savannah Lions")
        d.addComponent(token, TokenComponent)
        val nontoken = ready(d, "Savannah Lions")
        val centaur = ready(d, "Centaur Courser")

        d.state.projectedState.hasKeyword(token, Keyword.TRAINING) shouldBe true
        d.state.projectedState.hasKeyword(nontoken, Keyword.TRAINING) shouldBe false

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(token, nontoken, centaur), d.player2).error shouldBe null
        resolve(d)
        plusOnes(d, token) shouldBe 1
        plusOnes(d, nontoken) shouldBe 0
    }

    test("an opponent's creature token doesn't get training") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, ElderArthurMaxson.name)
        val theirs = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.addComponent(theirs, TokenComponent)
        d.state.projectedState.hasKeyword(theirs, Keyword.TRAINING) shouldBe false
    }

    test("sacrificing another creature makes Elder Arthur Maxson indestructible until end of turn") {
        val d = setup()
        val elder = d.putCreatureOnBattlefield(d.player1, ElderArthurMaxson.name)
        val fodder = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.submit(
            ActivateAbility(
                d.player1, elder, ElderArthurMaxson.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        ).error shouldBe null
        (fodder in d.state.getBattlefield()) shouldBe false
        resolve(d)
        d.state.projectedState.hasKeyword(elder, Keyword.INDESTRUCTIBLE) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasKeyword(elder, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
