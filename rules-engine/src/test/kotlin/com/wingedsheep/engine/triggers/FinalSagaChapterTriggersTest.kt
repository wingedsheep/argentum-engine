package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for `Triggers.you.finalSagaChapterTriggers()` (G147) — "whenever the final
 * chapter ability of a Saga you control triggers" (CR 714.2e; Historian's Boon). The engine stamps
 * [com.wingedsheep.sdk.scripting.AbilityTriggerCause.FINAL_SAGA_CHAPTER] on the final chapter
 * ability as it goes on the stack, whatever brought the lore counters there.
 */
class FinalSagaChapterTriggersTest : FunSpec({

    // "Whenever the final chapter ability of a Saga you control triggers, you gain 1 life."
    val observer = card("Final Chapter Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.finalSagaChapterTriggers()
            effect = Effects.GainLife(1)
        }
    }

    val threeChapterSaga = card("Three Chapter Test Saga") {
        manaCost = "{1}"
        typeLine = "Enchantment — Saga"
        sagaChapter(1) { effect = Effects.DrawCards(1) }
        sagaChapter(2) { effect = Effects.DrawCards(1) }
        sagaChapter(3) { effect = Effects.DrawCards(1) }
    }

    val oneChapterSaga = card("One Chapter Test Saga") {
        manaCost = "{1}"
        typeLine = "Enchantment — Saga"
        sagaChapter(1) { effect = Effects.DrawCards(1) }
    }

    // Puts two lore counters at once on any Saga — chapters II and III trigger together.
    val doubleLore = card("Double Lore") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val saga = target(TargetFilter(GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA)))
            effect = Effects.AddCounters(CounterType.LORE, 2, saga)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(observer, threeChapterSaga, oneChapterSaga, doubleLore))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while (guard++ < 30 && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.sagaWithOneLore(controller: EntityId): EntityId {
        val saga = putPermanentOnBattlefield(controller, "Three Chapter Test Saga")
        replaceState(state.updateEntity(saga) { container ->
            container.with(CountersComponent().withAdded(CounterType.LORE, 1))
                .with(SagaComponent(triggeredChapters = setOf(1)))
        })
        return saga
    }

    fun GameTestDriver.castDoubleLore(caster: EntityId, saga: EntityId) {
        val spell = putCardInHand(caster, "Double Lore")
        giveColorlessMana(caster, 1)
        castSpell(caster, spell, listOf(saga))
        drain()
    }

    test("two lore counters at once trigger chapters II and III, and only III is the final chapter") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Final Chapter Observer")
        val saga = driver.sagaWithOneLore(me)
        val life = driver.getLifeTotal(me)
        val hand = driver.getHandSize(me)

        driver.castDoubleLore(me, saga)

        withClue("both chapters resolved") { driver.getHandSize(me) shouldBe hand + 2 }
        withClue("the observer fired once, for chapter III") { driver.getLifeTotal(me) shouldBe life + 1 }
    }

    test("a one-chapter Saga's entry chapter is its final chapter") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Final Chapter Observer")
        val life = driver.getLifeTotal(me)

        val spell = driver.putCardInHand(me, "One Chapter Test Saga")
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, spell)
        driver.drain()

        driver.getLifeTotal(me) shouldBe life + 1
    }

    test("an opponent's Saga reaching its final chapter doesn't trigger it") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Final Chapter Observer")
        val saga = driver.sagaWithOneLore(opponent)
        val life = driver.getLifeTotal(me)
        val opponentHand = driver.getHandSize(opponent)

        driver.castDoubleLore(me, saga)

        withClue("the opponent's Saga resolved its chapters") {
            driver.getHandSize(opponent) shouldBe opponentHand + 2
        }
        driver.getLifeTotal(me) shouldBe life
    }
})
