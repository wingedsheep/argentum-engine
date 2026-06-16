package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.TomBombadil
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tom Bombadil — {W}{U}{B}{R}{G} Legendary Creature — God Bard 4/4.
 *
 * Static: "As long as there are four or more lore counters among Sagas you control, Tom Bombadil
 * has hexproof and indestructible." (new [com.wingedsheep.sdk.dsl.Conditions.CounterKindAmongYouControlAtLeast]
 * summing lore counters across Sagas you control).
 *
 * Trigger: "Whenever the final chapter ability of a Saga you control resolves, reveal cards from
 * the top of your library until you reveal a Saga card. Put that card onto the battlefield and the
 * rest on the bottom of your library in a random order. This ability triggers only once each turn."
 * (new [com.wingedsheep.sdk.scripting.EventPattern.SagaChapterResolvedEvent] +
 * [com.wingedsheep.sdk.dsl.Triggers.WheneverFinalChapterOfYourSagaResolves], oncePerTurn).
 */
class TomBombadilScenarioTest : FunSpec({

    val projector = StateProjector()

    // A one-chapter test Saga: chapter I (its final chapter) gains 1 life — no targets, so it
    // resolves cleanly. Casting it adds the first lore counter, which both triggers and (being the
    // final chapter) fires Tom's "final chapter resolves" ability.
    val OneChapterSaga = card("One-Chapter Saga") {
        manaCost = "{1}"
        typeLine = "Enchantment — Saga"
        oracleText = "I — You gain 1 life."
        sagaChapter(1) {
            effect = Effects.GainLife(1)
        }
    }

    // A three-chapter test Saga used as the library reveal target: entering with one lore counter
    // it is far from its final chapter (III), so it stays on the battlefield after being revealed.
    val ThreeChapterSaga = card("Three-Chapter Saga") {
        manaCost = "{2}"
        typeLine = "Enchantment — Saga"
        oracleText = "I, II, III — You gain 1 life."
        sagaChapter(1) { effect = Effects.GainLife(1) }
        sagaChapter(2) { effect = Effects.GainLife(1) }
        sagaChapter(3) { effect = Effects.GainLife(1) }
    }

    fun setLoreCounters(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.LORE, count))
                .with(SagaComponent(triggeredChapters = (1..count).toSet()))
        }
        driver.replaceState(newState)
    }

    test("4+ lore counters among Sagas you control grant Tom hexproof and indestructible") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TomBombadil, OneChapterSaga))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val tom = driver.putCreatureOnBattlefield(active, "Tom Bombadil")

        // No Sagas yet → no hexproof / indestructible.
        val projected0 = projector.project(driver.state)
        projected0.hasKeyword(tom, Keyword.HEXPROOF) shouldBe false
        projected0.hasKeyword(tom, Keyword.INDESTRUCTIBLE) shouldBe false

        // Two Sagas with 2 lore counters each = 4 total → both keywords granted.
        val sagaA = driver.putPermanentOnBattlefield(active, "One-Chapter Saga")
        val sagaB = driver.putPermanentOnBattlefield(active, "One-Chapter Saga")
        setLoreCounters(driver, sagaA, 2)
        setLoreCounters(driver, sagaB, 2)

        val projected4 = projector.project(driver.state)
        projected4.hasKeyword(tom, Keyword.HEXPROOF) shouldBe true
        projected4.hasKeyword(tom, Keyword.INDESTRUCTIBLE) shouldBe true

        // Drop below 4 (3 total) → keywords gone.
        val downState = driver.state.updateEntity(sagaB) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withRemoved(CounterType.LORE, 1))
        }
        driver.replaceState(downState)

        val projected3 = projector.project(driver.state)
        projected3.hasKeyword(tom, Keyword.HEXPROOF) shouldBe false
        projected3.hasKeyword(tom, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("final chapter resolving reveals a Saga onto the battlefield, once per turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TomBombadil, OneChapterSaga, ThreeChapterSaga))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        driver.putCreatureOnBattlefield(active, "Tom Bombadil")

        // Library top: a Forest (nonsaga) over a Three-Chapter Saga; reveal-until-Saga skips the
        // Forest (→ bottom) and finds the Saga, which survives on the battlefield (chapter III is
        // far off). Top-most is the last pushed.
        driver.putCardOnTopOfLibrary(active, "Three-Chapter Saga")
        driver.putCardOnTopOfLibrary(active, "Forest")

        // Cast a one-chapter Saga; its only (final) chapter resolves, firing Tom's trigger.
        val sagaSpell = driver.putCardInHand(active, "One-Chapter Saga")
        driver.giveColorlessMana(active, 1)
        driver.castSpell(active, sagaSpell)
        driver.bothPass()

        // Resolve the chapter ability and Tom's triggered ability (no decisions expected).
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) {
            driver.bothPass()
        }

        // The revealed Three-Chapter Saga is now on the battlefield (Tom's trigger put it there).
        driver.state.getBattlefield().any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Three-Chapter Saga"
        } shouldBe true

        // Tom is still on the battlefield.
        driver.state.getBattlefield().any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Tom Bombadil"
        } shouldBe true
    }

    test("the final-chapter trigger fires only once each turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TomBombadil, OneChapterSaga, ThreeChapterSaga))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        driver.putCreatureOnBattlefield(active, "Tom Bombadil")

        // Two Sagas sit on top of the library; only the first should be revealed/put out this turn.
        driver.putCardOnTopOfLibrary(active, "Three-Chapter Saga")
        driver.putCardOnTopOfLibrary(active, "Three-Chapter Saga")

        // Cast two one-chapter Sagas back-to-back; both final chapters resolve this turn, but Tom's
        // trigger is once-per-turn, so it fires only on the first.
        repeat(2) {
            val spell = driver.putCardInHand(active, "One-Chapter Saga")
            driver.giveColorlessMana(active, 1)
            driver.castSpell(active, spell)
            driver.bothPass()
            var guard = 0
            while (guard++ < 20 && driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            }
        }

        // Only one Three-Chapter Saga was put onto the battlefield (the trigger fired once).
        val revealed = driver.state.getBattlefield().count {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Three-Chapter Saga"
        }
        revealed shouldBe 1
    }
})
