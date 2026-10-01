package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine tests for exalted (CR 702.83) as a keyword-derived trigger — printed on a card, or carried
 * by exalted keyword counters (CR 122.1b; Emissary of Soulfire).
 *
 * | Rule | Covered by |
 * |---|---|
 * | 702.83a — a creature you control attacks alone → it gets +1/+1 until end of turn | "a lone attacker gets +1/+1 from another permanent's exalted" |
 * | 702.83a — the exalted creature itself attacking alone | "exalted pumps its own lone attack" |
 * | 702.83b — two declared attackers: nobody attacks alone | "no trigger when two creatures attack" |
 * | 702.83a — "you control": an opponent's exalted is silent | "an opponent's exalted does not trigger" |
 * | each instance triggers separately | "two exalted permanents trigger twice" |
 * | 122.1b — an exalted counter grants exalted; each counter is an instance | "each exalted counter is its own trigger" |
 * | printed + counter instances add | "printed exalted plus a counter is two instances" |
 * | lose all abilities strips printed exalted but not the counter's | "losing all abilities keeps counter exalted" |
 * | the pump lasts until end of turn | "the bonus ends at cleanup" |
 */
class ExaltedKeywordScenarioTest : FunSpec({

    val exaltedBear = card("Test Exalted Bear") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.EXALTED)
    }

    val strip = card("Test Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(exaltedBear, strip).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    fun GameTestDriver.addExaltedCounters(id: EntityId, n: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.EXALTED, n))
        })
    }

    fun GameTestDriver.creature(name: String, player: EntityId = activePlayer!!): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    /** Declares [attackers] and returns how many triggers went on the stack. */
    fun GameTestDriver.attackWith(attackers: List<EntityId>): Int {
        val me = activePlayer!!
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, attackers, getOpponent(me)).error shouldBe null
        return state.stack.size
    }

    fun GameTestDriver.resolveAll() {
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("a lone attacker gets +1/+1 from another permanent's exalted") {
        val driver = createDriver()
        driver.creature("Test Exalted Bear")
        val bears = driver.creature("Grizzly Bears")

        driver.attackWith(listOf(bears)) shouldBe 1
        driver.resolveAll()

        driver.power(bears) shouldBe 3
        driver.toughness(bears) shouldBe 3
    }

    test("exalted pumps its own lone attack") {
        val driver = createDriver()
        val exalted = driver.creature("Test Exalted Bear")

        driver.attackWith(listOf(exalted)) shouldBe 1
        driver.resolveAll()

        driver.power(exalted) shouldBe 3
    }

    test("no trigger when two creatures attack") {
        val driver = createDriver()
        val exalted = driver.creature("Test Exalted Bear")
        val bears = driver.creature("Grizzly Bears")

        driver.attackWith(listOf(exalted, bears)) shouldBe 0
        driver.power(exalted) shouldBe 2
        driver.power(bears) shouldBe 2
    }

    test("an opponent's exalted does not trigger") {
        val driver = createDriver()
        driver.creature("Test Exalted Bear", driver.getOpponent(driver.activePlayer!!))
        val bears = driver.creature("Grizzly Bears")

        driver.attackWith(listOf(bears)) shouldBe 0
        driver.power(bears) shouldBe 2
    }

    test("two exalted permanents trigger twice") {
        val driver = createDriver()
        driver.creature("Test Exalted Bear")
        driver.creature("Test Exalted Bear")
        val bears = driver.creature("Grizzly Bears")

        withClue("each instance is its own trigger") { driver.attackWith(listOf(bears)) shouldBe 2 }
        driver.resolveAll()

        driver.power(bears) shouldBe 4
        driver.toughness(bears) shouldBe 4
    }

    test("each exalted counter is its own trigger") {
        val driver = createDriver()
        val carrier = driver.creature("Grizzly Bears")
        driver.addExaltedCounters(carrier, 2)
        val attacker = driver.creature("Grizzly Bears")

        withClue("an exalted counter grants the keyword") {
            driver.state.projectedState.hasKeyword(carrier, Keyword.EXALTED) shouldBe true
        }
        driver.attackWith(listOf(attacker)) shouldBe 2
        driver.resolveAll()

        driver.power(attacker) shouldBe 4
    }

    test("printed exalted plus a counter is two instances") {
        val driver = createDriver()
        val exalted = driver.creature("Test Exalted Bear")
        driver.addExaltedCounters(exalted, 1)
        val bears = driver.creature("Grizzly Bears")

        driver.attackWith(listOf(bears)) shouldBe 2
        driver.resolveAll()

        driver.power(bears) shouldBe 4
    }

    test("losing all abilities keeps counter exalted") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val exalted = driver.creature("Test Exalted Bear")
        driver.addExaltedCounters(exalted, 1)
        val bears = driver.creature("Grizzly Bears")

        val spell = driver.putCardInHand(me, "Test Strip")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, spell, targets = listOf(exalted)).error shouldBe null
        driver.bothPass()

        withClue("the printed instance is gone; the counter's survives") {
            driver.attackWith(listOf(bears)) shouldBe 1
        }
        driver.resolveAll()
        driver.power(bears) shouldBe 3
    }

    test("the bonus ends at cleanup") {
        val driver = createDriver()
        driver.creature("Test Exalted Bear")
        val bears = driver.creature("Grizzly Bears")

        driver.attackWith(listOf(bears)) shouldBe 1
        driver.resolveAll()
        driver.power(bears) shouldBe 3

        driver.passPriorityUntil(Step.UPKEEP)
        driver.power(bears) shouldBe 2
    }
})
