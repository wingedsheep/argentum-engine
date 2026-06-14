package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.AragornCompanyLeader
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Aragorn, Company Leader — Gap 7 (keyword counters + "choose a kind of counter").
 *
 * (a) A Ring tempt with a Ring-bearer other than Aragorn lets you choose a counter kind and put it
 *     on Aragorn, granting the matching keyword via the projection's keyword-counter map.
 * (b) Putting a counter on Aragorn fires his second ability, putting one of each of the four named
 *     kinds (first strike, vigilance, deathtouch, lifelink) on up to one other target creature.
 *
 * The Ring tempt is driven by a local "Ring Tempter" sorcery (the Faramir test pattern); the
 * Ring-bearer is designated by answering the temptation's SelectCardsDecision with a creature other
 * than Aragorn.
 */
class AragornCompanyLeaderScenarioTest : FunSpec({

    val RingTempter = card("Ring Tempter") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "The Ring tempts you."
        spell { effect = Effects.TheRingTemptsYou() }
    }

    val Bear = CardDefinition.creature("Ring Bear", ManaCost.parse("{2}"), emptySet(), 2, 2)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RingTempter, Bear, AragornCompanyLeader))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.tempt(player: EntityId, bearerId: EntityId) {
        val cardId = putCardInHand(player, "Ring Tempter")
        castSpell(player, cardId)
        bothPass()
        val decision = pendingDecision
        if (decision is SelectCardsDecision) {
            submitDecision(player, CardsSelectedResponse(decision.id, listOf(bearerId)))
        }
    }

    fun GameTestDriver.countersOf(entity: EntityId, type: CounterType): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.counters?.get(type) ?: 0

    test("Ring tempt with another Ring-bearer lets you choose a counter kind for Aragorn") {
        val driver = createDriver()
        val active = driver.activePlayer!!

        val aragorn = driver.putCreatureOnBattlefield(active, "Aragorn, Company Leader")
        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear")

        driver.tempt(active, bear)

        // First trigger resolves → ChooseOptionDecision over the four counter kinds. Pick first strike (index 0).
        driver.bothPass()
        val choose = driver.pendingDecision
        (choose is ChooseOptionDecision) shouldBe true
        choose as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choose.id, 0))

        // The first strike counter triggers the second ability; decline its optional target.
        var guard = 0
        while (guard++ < 8) {
            when (val pd = driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(active, emptyList())
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        driver.countersOf(aragorn, CounterType.FIRST_STRIKE) shouldBe 1
        driver.state.projectedState.hasKeyword(aragorn, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("putting a counter on Aragorn puts one of each kind on another target creature") {
        val driver = createDriver()
        val active = driver.activePlayer!!

        val aragorn = driver.putCreatureOnBattlefield(active, "Aragorn, Company Leader")
        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear")

        driver.tempt(active, bear)

        // First trigger → choose a counter for Aragorn (deathtouch, index 2).
        driver.bothPass()
        val choose = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choose.id, 2))

        // Second ability triggers off that counter; target the Bear with all four kinds.
        var guard = 0
        while (guard++ < 8) {
            when (val pd = driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(active, listOf(bear))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        // Aragorn got his deathtouch counter.
        driver.countersOf(aragorn, CounterType.DEATHTOUCH) shouldBe 1
        // The Bear got one of each of the four named kinds.
        driver.countersOf(bear, CounterType.FIRST_STRIKE) shouldBe 1
        driver.countersOf(bear, CounterType.VIGILANCE) shouldBe 1
        driver.countersOf(bear, CounterType.DEATHTOUCH) shouldBe 1
        driver.countersOf(bear, CounterType.LIFELINK) shouldBe 1
        // …granting the matching keywords via the keyword-counter projection.
        driver.state.projectedState.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe true
        driver.state.projectedState.hasKeyword(bear, Keyword.DEATHTOUCH) shouldBe true
    }
})
