package com.wingedsheep.engine.view

import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.ExilePatterns
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * A step-based delayed trigger ("return it at the beginning of the next end step") was
 * scheduled in the open, so both players see it on its controller's badges until it fires.
 */
class ScheduledTriggerBadgeTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        return driver
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    fun GameTestDriver.scheduled(step: Step) = DelayedTriggeredAbility(
        id = "scheduled", effect = Effects.DrawCards(1), fireAtStep = step,
        sourceId = player1, sourceName = "Scheduled Draw", controllerId = player1,
    )

    fun GameTestDriver.badgeText(trigger: DelayedTriggeredAbility, viewer: com.wingedsheep.sdk.model.EntityId): String? =
        transformer(this).transform(state.copy(delayedTriggers = listOf(trigger)), viewer).players
            .single { it.playerId == player1 }.activeEffects.singleOrNull { it.name == "Scheduled Draw" }?.description

    test("a trigger scheduled for the next end step shows on its controller's badges, for both players") {
        val driver = createDriver()
        val trigger = driver.scheduled(Step.END)

        for (viewer in listOf(driver.player1, driver.player2)) {
            driver.badgeText(trigger, viewer) shouldBe "At the beginning of the next end step: Draw a card."
        }
    }

    test("the badge says which step, whose turn and how often") {
        val driver = createDriver()
        val viewer = driver.player2

        driver.badgeText(driver.scheduled(Step.UPKEEP), viewer) shouldNotBe driver.badgeText(driver.scheduled(Step.END), viewer)
        driver.badgeText(driver.scheduled(Step.UPKEEP).copy(fireOnPlayerId = driver.player2), viewer) shouldBe
            "At the beginning of Player 2's next upkeep: Draw a card."
        driver.badgeText(driver.scheduled(Step.UPKEEP).copy(fireOnPlayerId = driver.player1), viewer) shouldBe
            "At the beginning of Player 1's next upkeep: Draw a card."
        driver.badgeText(
            driver.scheduled(Step.BEGIN_COMBAT).copy(repeatAtEachMatchingStep = true, expiry = DelayedTriggerExpiry.EndOfTurn),
            viewer,
        ) shouldBe "At the beginning of each combat this turn: Draw a card."
    }

    test("a trigger waiting for any main phase names the main phase, not the precombat one") {
        val driver = createDriver()
        driver.badgeText(
            driver.scheduled(Step.PRECOMBAT_MAIN).copy(stepScope = DelayedTriggerStepScope.ANY_MAIN_PHASE, fireOnPlayerId = driver.player1),
            driver.player2,
        ) shouldBe "At the beginning of Player 1's next main phase: Draw a card."
    }

    test("a one-shot trigger that expires this turn says so, and is gone after cleanup without firing") {
        val driver = createDriver()
        val viewer = driver.player2
        val oneShot = driver.scheduled(Step.BEGIN_COMBAT)
        driver.badgeText(oneShot, viewer) shouldBe "At the beginning of the next combat: Draw a card."
        driver.badgeText(oneShot.copy(expiry = DelayedTriggerExpiry.EndOfTurn), viewer) shouldBe
            "At the beginning of the next combat this turn: Draw a card."

        // Scheduled after this turn's combat, it never fires and cleanup removes it.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val armed = driver.state.copy(delayedTriggers = listOf(oneShot.copy(expiry = DelayedTriggerExpiry.EndOfTurn)))
        val handSize = armed.getHand(driver.player1).size
        driver.replaceState(armed)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.delayedTriggers.none { it.id == "scheduled" } shouldBe true
        driver.state.getHand(driver.player1).size shouldBe handSize
        transformer(driver).transform(driver.state, viewer).players.single { it.playerId == driver.player1 }
            .activeEffects.none { it.name == "Scheduled Draw" } shouldBe true
    }

    test("exiling two cards until the end step shows two returns, each naming its card") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(driver.player1, "Hill Giant")
        var state = driver.state
        for (creature in listOf(bears, giant)) {
            val result = driver.services.effectExecutorRegistry.execute(
                state,
                ExilePatterns.exileUntilEndStep(EffectTarget.ContextTarget(0)),
                EffectContext(
                    sourceId = driver.player1, controllerId = driver.player1,
                    targets = listOf(ChosenTarget.Permanent(creature)),
                ),
            )
            state = result.state
        }
        val texts = transformer(driver).transform(state, driver.player2).players
            .single { it.playerId == driver.player1 }.activeEffects
            .filter { it.effectId.startsWith("scheduled_trigger_") }.map { it.description!! }

        texts.size shouldBe 2
        texts.single { "Grizzly Bears" in it } shouldNotContain "specific entity"
        texts.single { "Hill Giant" in it } shouldNotContain "specific entity"
    }

    test("a scheduled return names the card it will return, and a face-down source stays face down") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val slide = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val state = driver.state
            .moveToZone(bears, ZoneKey(driver.player1, Zone.BATTLEFIELD), ZoneKey(driver.player1, Zone.EXILE))
            .updateEntity(slide) { it.with(FaceDownComponent) }
        val trigger = DelayedTriggeredAbility(
            id = "return", effect = MoveToZoneEffect(EffectTarget.SpecificEntity(bears), Zone.BATTLEFIELD),
            fireAtStep = Step.END, sourceId = slide, sourceName = "Secret Slide", controllerId = driver.player1,
        )
        val badge = transformer(driver).transform(state.copy(delayedTriggers = listOf(trigger)), driver.player2).players
            .single { it.playerId == driver.player1 }.activeEffects.single { it.effectId == "scheduled_trigger_return" }

        badge.description!! shouldContain "Grizzly Bears"
        badge.description!! shouldNotContain "specific entity"
        badge.name shouldNotBe "Secret Slide"
    }

    test("an effect naming two different cards keeps its placeholders rather than guess which is which") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(driver.player1, "Hill Giant")
        val trigger = DelayedTriggeredAbility(
            id = "both",
            effect = CompositeEffect(listOf(
                MoveToZoneEffect(EffectTarget.SpecificEntity(bears), Zone.HAND),
                MoveToZoneEffect(EffectTarget.SpecificEntity(giant), Zone.GRAVEYARD),
            )),
            fireAtStep = Step.END, sourceId = bears, sourceName = "Grizzly Bears", controllerId = driver.player1,
        )
        val badge = transformer(driver).transform(driver.state.copy(delayedTriggers = listOf(trigger)), driver.player2)
            .players.single { it.playerId == driver.player1 }.activeEffects.single { it.effectId == "scheduled_trigger_both" }

        badge.description!! shouldContain "specific entity"
    }
})

