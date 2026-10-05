package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The "for the first time each turn" window on the becomes-target trigger follows its controller
 * axis:
 *
 *  - with `byYou` (Valiant — "a spell or ability **you control** for the first time each turn") the
 *    window is the trigger controller's, so an opponent targeting the creature first doesn't close it;
 *  - with no controller axis (Angelic Cub — "a spell or ability for the first time each turn") the
 *    window is everyone's, so *any* earlier targeting closes it.
 *
 * The emission half drives real activations and reads both flags off the emitted [BecomesTargetEvent];
 * the matching half feeds synthesized events to [TriggerDetector] against both observer shapes.
 */
class BecomesTargetFirstTimeWindowTest : FunSpec({

    /** "{T}: Tap target creature." — a targeted ability either player can activate. */
    val tapper = card("Target Creature Tapper") {
        manaCost = "{0}"
        typeLine = "Creature — Human Wizard"
        power = 0
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            val victim = target(TargetFilter.Creature)
            effect = Effects.Tap(victim)
        }
    }

    val valiantObserver = card("Valiant Window Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Mouse"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.becomesTarget(byYou = true, firstTimeEachTurn = true)
            effect = Effects.GainLife(1)
        }
    }

    val anyoneObserver = card("Anyone Window Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Cat Angel"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.becomesTarget(firstTimeEachTurn = true)
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(tapper, valiantObserver, anyoneObserver))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.readyTapper(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Target Creature Tapper").also { removeSummoningSickness(it) }

    fun GameTestDriver.tap(player: EntityId, source: EntityId, target: EntityId) = submit(
        ActivateAbility(
            playerId = player,
            sourceId = source,
            abilityId = tapper.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(target))
        )
    )

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    context("emission") {
        test("an opponent's earlier targeting closes the any-controller window, not yours") {
            val driver = createDriver()
            val me = driver.activePlayer!!
            val opponent = driver.getOpponent(me)
            val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            val mine = driver.readyTapper(me)
            val theirs = driver.readyTapper(opponent)

            driver.passPriority(me)
            val first = driver.tap(opponent, theirs, bears).events.filterIsInstance<BecomesTargetEvent>().single()
            withClue("first targeting of the turn, by the opponent") {
                first.firstTimeByThisController shouldBe true
                first.firstTimeThisTurn shouldBe true
            }
            driver.resolveStack()
            // Priority can land on the resolved ability's controller; hand it back to the active player.
            driver.state.priorityPlayerId?.takeIf { it != me }?.let { driver.passPriority(it) }

            val second = driver.tap(me, mine, bears).events.filterIsInstance<BecomesTargetEvent>().single()
            withClue("your first targeting, but not the turn's first") {
                second.firstTimeByThisController shouldBe true
                second.firstTimeThisTurn shouldBe false
            }
        }
    }

    context("matching") {
        fun event(target: EntityId, controller: EntityId, byController: Boolean, byAnyone: Boolean) =
            BecomesTargetEvent(
                targetEntityId = target,
                targetName = "",
                sourceEntityId = EntityId.generate(),
                controllerId = controller,
                firstTimeByThisController = byController,
                firstTimeThisTurn = byAnyone,
            )

        fun firings(driver: GameTestDriver, event: BecomesTargetEvent, observer: EntityId) =
            TriggerDetector(
                driver.cardRegistry,
                predicateEvaluator = PredicateEvaluator(cardRegistry = null),
                conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions
            ).detectTriggers(driver.state, listOf(event))
                .filter { it.ability.trigger is EventPattern.BecomesTargetEvent && it.sourceId == observer }

        test("valiant reads the per-controller window") {
            val driver = createDriver()
            val me = driver.player1
            val valiant = driver.putCreatureOnBattlefield(me, "Valiant Window Observer")

            withClue("your first targeting after an opponent's still fires") {
                firings(driver, event(valiant, me, byController = true, byAnyone = false), valiant) shouldHaveSize 1
            }
            withClue("your second targeting doesn't") {
                firings(driver, event(valiant, me, byController = false, byAnyone = false), valiant) shouldHaveSize 0
            }
        }

        test("the unrestricted wording reads the any-controller window") {
            val driver = createDriver()
            val me = driver.player1
            val cub = driver.putCreatureOnBattlefield(me, "Anyone Window Observer")

            withClue("the turn's first targeting fires, whoever controls it") {
                firings(driver, event(cub, driver.player2, byController = true, byAnyone = true), cub) shouldHaveSize 1
            }
            withClue("your first targeting after an opponent's doesn't") {
                firings(driver, event(cub, me, byController = true, byAnyone = false), cub) shouldHaveSize 0
            }
        }
    }
})
