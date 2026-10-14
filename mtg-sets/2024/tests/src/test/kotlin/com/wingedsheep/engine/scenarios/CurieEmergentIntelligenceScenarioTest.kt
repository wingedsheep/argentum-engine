package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.CurieEmergentIntelligence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Curie, Emergent Intelligence — its combat-damage trigger draws cards equal to its *base* power,
 * and its activated ability exiles another nontoken artifact creature as a cost and becomes a copy
 * of it as it last existed on the battlefield, keeping the draw trigger as the copy exception.
 */
class CurieEmergentIntelligenceScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + CurieEmergentIntelligence)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    fun activate(d: GameTestDriver, curie: EntityId, exiled: EntityId) = run {
        d.giveColorlessMana(d.player1, 1)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.submit(
            ActivateAbility(
                d.player1, curie, CurieEmergentIntelligence.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
            )
        ).also { if (it.error == null) resolve(d) }
    }
    /** Attacks the unblocked opponent with [attacker] and returns how many cards player 1 drew. */
    fun attackAndCountDraws(d: GameTestDriver, attacker: EntityId): Int {
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val hand = d.getHandSize(d.player1)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)
        resolve(d)
        return d.getHandSize(d.player1) - hand
    }

    test("Curie draws cards equal to its base power when it deals combat damage to a player") {
        val d = setup()
        val curie = d.putCreatureOnBattlefield(d.player1, CurieEmergentIntelligence.name)
        attackAndCountDraws(d, curie) shouldBe 1
    }

    test("it becomes a copy of the exiled artifact creature and keeps the base-power draw trigger") {
        val d = setup()
        val curie = d.putCreatureOnBattlefield(d.player1, CurieEmergentIntelligence.name)
        val frogmite = d.putCreatureOnBattlefield(d.player1, "Frogmite")
        activate(d, curie, frogmite).error shouldBe null

        d.state.getExile(d.player1).contains(frogmite) shouldBe true
        d.state.getEntity(curie)?.get<CardComponent>()?.name shouldBe "Frogmite"
        d.state.projectedState.getPower(curie) shouldBe 2
        // A +1/+1 counter raises its power, not its base power: the draw still counts 2.
        d.addComponent(curie, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        attackAndCountDraws(d, curie) shouldBe 2
    }

    test("it can't exile a creature that isn't an artifact") {
        val d = setup()
        val curie = d.putCreatureOnBattlefield(d.player1, CurieEmergentIntelligence.name)
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        activate(d, curie, courser).error shouldNotBe null
        d.state.getBattlefield().contains(courser) shouldBe true
    }
})
