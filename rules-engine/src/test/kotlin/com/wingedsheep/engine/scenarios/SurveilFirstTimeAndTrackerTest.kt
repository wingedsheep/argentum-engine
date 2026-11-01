package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SurveiledEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The surveil-only turn history (CR 701.25): `Triggers.you.surveils(firstTimeEachTurn = true)`
 * ("whenever you surveil for the first time each turn", Whispering Snitch) and
 * `Conditions.SurveiledThisTurn` / `TurnTracker.SURVEILED` ("as long as you've surveilled this
 * turn", Darkblade Agent). Both read the same per-player marker the surveil-event executor sets,
 * so a scry never counts, a surveil 0 (CR 701.25c: no surveil event) never counts, and the
 * marker is cleared at end of turn.
 */
class SurveilFirstTimeAndTrackerTest : FunSpec({

    fun spell(name: String, text: String, effect: com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = text
        spell { this.effect = effect }
    }
    val SurveilOne = spell("Surveil One FT", "Surveil 1.", Patterns.Library.surveil(1))
    val SurveilZero = spell("Surveil Zero FT", "Surveil 0.", Patterns.Library.surveil(0))
    val ScryOne = spell("Scry One FT", "Scry 1.", Patterns.Library.scry(1))

    // "Whenever you surveil for the first time each turn, put a +1/+1 counter on it."
    val FirstSurveilWatcher = card("First Surveil Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.surveils(firstTimeEachTurn = true)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // "As long as you've surveilled this turn, this creature gets +2/+0."
    val SurveilBruiser = card("Surveil Bruiser") {
        manaCost = "{0}"
        typeLine = "Creature — Rogue"
        power = 1; toughness = 1
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = ModifyStats(2, 0, GroupFilter.source()),
                condition = Conditions.SurveiledThisTurn,
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(SurveilOne, SurveilZero, ScryOne, FirstSurveilWatcher, SurveilBruiser)
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.power(id: EntityId): Int? = state.projectedState.getPower(id)

    // Cast a scry/surveil spell and keep everything on top.
    fun GameTestDriver.castLook(player: EntityId, cardName: String) {
        val cardId = putCardInHand(player, cardName)
        castSpell(player, cardId)
        bothPass()
        repeat(4) {
            when (val decision = pendingDecision) {
                is SelectCardsDecision -> submitDecision(player, CardsSelectedResponse(decision.id, emptyList()))
                is ReorderLibraryDecision -> submitDecision(player, OrderedResponse(decision.id, decision.cards))
                else -> {}
            }
        }
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    test("first-time trigger fires on the first surveil of the turn only") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = driver.putCreatureOnBattlefield(active, "First Surveil Watcher")

        val before = driver.events.size
        driver.castLook(active, "Surveil One FT")
        driver.plusOneCounters(watcher) shouldBe 1

        driver.castLook(active, "Surveil One FT")
        driver.plusOneCounters(watcher) shouldBe 1

        driver.events.drop(before).filterIsInstance<SurveiledEvent>().map { it.firstThisTurn } shouldBe
            listOf(true, false)
    }

    test("first-time trigger resets on a later turn") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = driver.putCreatureOnBattlefield(active, "First Surveil Watcher")

        driver.castLook(active, "Surveil One FT")
        driver.plusOneCounters(watcher) shouldBe 1

        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, active)
        driver.castLook(active, "Surveil One FT")
        driver.plusOneCounters(watcher) shouldBe 2
    }

    test("a scry neither fires the first-surveil trigger nor uses it up") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = driver.putCreatureOnBattlefield(active, "First Surveil Watcher")

        driver.castLook(active, "Scry One FT")
        driver.plusOneCounters(watcher) shouldBe 0

        driver.castLook(active, "Surveil One FT")
        driver.plusOneCounters(watcher) shouldBe 1
    }

    test("an opponent's surveil does not fire 'you surveil' nor use up your first time") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        val watcher = driver.putCreatureOnBattlefield(active, "First Surveil Watcher")

        driver.castLook(opponent, "Surveil One FT")
        driver.plusOneCounters(watcher) shouldBe 0
    }

    test("SurveiledThisTurn holds after a surveil and clears at end of turn") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bruiser = driver.putCreatureOnBattlefield(active, "Surveil Bruiser")
        driver.power(bruiser) shouldBe 1

        driver.castLook(active, "Surveil One FT")
        driver.power(bruiser) shouldBe 3

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.getOpponent(active))
        driver.power(bruiser) shouldBe 1
    }

    test("SurveiledThisTurn is not satisfied by a scry or a surveil 0 (CR 701.25c)") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bruiser = driver.putCreatureOnBattlefield(active, "Surveil Bruiser")

        driver.castLook(active, "Scry One FT")
        driver.power(bruiser) shouldBe 1

        driver.castLook(active, "Surveil Zero FT")
        driver.power(bruiser) shouldBe 1
    }
})
