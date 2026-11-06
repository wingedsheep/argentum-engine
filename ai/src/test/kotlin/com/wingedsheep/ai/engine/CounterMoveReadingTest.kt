package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Counters moved between permanents — counted (Tester of the Tangential), chosen per kind
 * (Goldberry, River-Daughter) or chosen per permanent for one kind (Aetherborn Marauder): the
 * counters carry the meaning, so the slots read FLEXIBLE, the move needs no intent tag, and the
 * card cashes in the +1/+1 counters the deck puts down.
 */
class CounterMoveReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("per-kind and per-permanent chosen counter moves read fully") {
        AiReadingAudit.read(card("Aetherborn Marauder")).readFully shouldBe true
        AiReadingAudit.read(card("Goldberry, River-Daughter")).readFully shouldBe true
    }

    test("a counter move rewards the +1/+1 counters axis") {
        SynergyAxes.of(card("Aetherborn Marauder")).rewards shouldContain SynergyAxis.PLUS_COUNTERS
        SynergyAxes.of(card("Goldberry, River-Daughter")).rewards shouldContain SynergyAxis.PLUS_COUNTERS
    }

    test("Aetherborn Marauder's prompt moves a ground creature's counters onto the evasive lifelinker") {
        val marauder = card("Aetherborn Marauder")
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(marauder))
            initMirrorMatch(Deck.of("Swamp" to 40), startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.replaceState(d.state.updateEntity(courser) {
            it.with((it.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
        })
        d.giveMana(d.player1, Color.BLACK, 4)
        d.castSpell(d.player1, d.putCardInHand(d.player1, marauder.name)).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null

        val prompt = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        prompt.maxValue shouldBe 2
        val answer = AIPlayer.create(d.cardRegistry, d.player1, AiProfile.LIVE)
            .respondToDecision(d.state, prompt).shouldBeInstanceOf<NumberChosenResponse>()
        answer.number shouldBeGreaterThan 0
    }
})
