package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine tests for rampage N (CR 702.23) as a keyword-derived trigger — printed on a card with the
 * `rampage(n)` builder helper, or granted by [Effects.GrantRampage] (Rapid Fire).
 *
 * | Rule | Covered by |
 * |---|---|
 * | 702.23a — becomes blocked → +N/+N per blocker beyond the first | "printed rampage counts blockers beyond the first" |
 * | 702.23a — one trigger however many block; one blocker → +0/+0 | "blocked by one creature, rampage adds nothing" |
 * | 702.23a — nothing when unblocked | "an unblocked attacker gets nothing" |
 * | 702.23b — bonus counted when the trigger resolves | "a blocker removed in response lowers the bonus" |
 * | 702.23c — each instance triggers separately | "two printed instances trigger separately" |
 * | rampage is an ability — lose all abilities, no trigger | "a creature that lost its abilities has no rampage" |
 * | granted rampage triggers and reads as the keyword | "granted rampage triggers and is a rampage keyword" |
 * | repeated grants add up (projected `RAMPAGE_<n>` strings sum) | "two grants of rampage 1 are rampage 2" |
 */
class RampageKeywordScenarioTest : FunSpec({

    val rampageTwo = card("Test Rampage Two") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        rampage(2)
    }

    val doubleRampage = card("Test Double Rampage") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
        rampage(1)
        rampage(2)
    }

    val frenzy = card("Test Frenzy") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantRampage(1, t)
        }
    }

    val strip = card("Test Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    val kill = card("Test Kill") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(rampageTwo, doubleRampage, frenzy, strip, kill).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    /** Casts a one-target instant from [caster]'s hand and resolves it. */
    fun GameTestDriver.castAndResolve(name: String, target: EntityId, color: Color, caster: EntityId = activePlayer!!) {
        val spell = putCardInHand(caster, name)
        giveMana(caster, color, 1)
        castSpell(caster, spell, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.attacker(name: String): EntityId {
        val id = putCreatureOnBattlefield(activePlayer!!, name)
        removeSummoningSickness(id)
        return id
    }

    fun GameTestDriver.blockers(count: Int): List<EntityId> =
        List(count) { putCreatureOnBattlefield(getOpponent(activePlayer!!), "Grizzly Bears") }

    /** The active player attacks with [attacker]; the opponent blocks with [blockers]. Returns the stack size. */
    fun GameTestDriver.attackInto(attacker: EntityId, blockers: List<EntityId>): Int {
        val me = activePlayer!!
        val opponent = getOpponent(me)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(attacker), opponent).error shouldBe null
        bothPass()
        declareBlockers(opponent, blockers.associateWith { listOf(attacker) }).error shouldBe null
        return state.stack.size
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("printed rampage counts blockers beyond the first") {
        val driver = createDriver()
        val beast = driver.attacker("Test Rampage Two")
        driver.attackInto(beast, driver.blockers(3)) shouldBe 1
        driver.resolveStack()

        driver.power(beast) shouldBe 7
        driver.toughness(beast) shouldBe 7
    }

    test("blocked by one creature, rampage adds nothing") {
        val driver = createDriver()
        val beast = driver.attacker("Test Rampage Two")
        driver.attackInto(beast, driver.blockers(1)) shouldBe 1
        driver.resolveStack()

        driver.power(beast) shouldBe 3
        driver.toughness(beast) shouldBe 3
    }

    test("an unblocked attacker gets nothing") {
        val driver = createDriver()
        val beast = driver.attacker("Test Rampage Two")
        driver.attackInto(beast, emptyList()) shouldBe 0

        driver.power(beast) shouldBe 3
    }

    test("a blocker removed in response lowers the bonus") {
        val driver = createDriver()
        val beast = driver.attacker("Test Rampage Two")
        val blockers = driver.blockers(3)
        driver.attackInto(beast, blockers) shouldBe 1

        // With the rampage trigger on the stack, whoever holds priority kills a blocker.
        driver.castAndResolve("Test Kill", blockers.first(), Color.BLACK, caster = driver.state.priorityPlayerId!!)
        driver.resolveStack()

        driver.power(beast) shouldBe 5
        driver.toughness(beast) shouldBe 5
    }

    test("two printed instances trigger separately") {
        val driver = createDriver()
        val beast = driver.attacker("Test Double Rampage")
        driver.attackInto(beast, driver.blockers(2)) shouldBe 2
        driver.resolveStack()

        driver.power(beast) shouldBe 4
        driver.toughness(beast) shouldBe 4
    }

    test("a creature that lost its abilities has no rampage") {
        val driver = createDriver()
        val beast = driver.attacker("Test Rampage Two")
        driver.castAndResolve("Test Strip", beast, Color.BLUE)
        driver.state.projectedState.hasKeyword(beast, Keyword.RAMPAGE) shouldBe false

        driver.attackInto(beast, driver.blockers(3)) shouldBe 0
        driver.power(beast) shouldBe 3
    }

    test("granted rampage triggers and is a rampage keyword") {
        val driver = createDriver()
        val bears = driver.attacker("Grizzly Bears")
        driver.castAndResolve("Test Frenzy", bears, Color.RED)
        driver.state.projectedState.hasKeyword(bears, Keyword.RAMPAGE) shouldBe true

        driver.attackInto(bears, driver.blockers(3)) shouldBe 1
        driver.resolveStack()

        driver.power(bears) shouldBe 4
        driver.toughness(bears) shouldBe 4
    }

    test("two grants of rampage 1 are rampage 2") {
        val driver = createDriver()
        val bears = driver.attacker("Grizzly Bears")
        driver.castAndResolve("Test Frenzy", bears, Color.RED)
        driver.castAndResolve("Test Frenzy", bears, Color.RED)

        // One summed trigger, not two.
        driver.attackInto(bears, driver.blockers(2)) shouldBe 1
        driver.resolveStack()

        driver.power(bears) shouldBe 4
        driver.toughness(bears) shouldBe 4
    }

    test("granted rampage stacks with printed rampage") {
        val driver = createDriver()
        val beast = driver.attacker("Test Rampage Two")
        driver.castAndResolve("Test Frenzy", beast, Color.RED)

        driver.attackInto(beast, driver.blockers(2)) shouldBe 2
        driver.resolveStack()

        // +2/+2 printed and +1/+1 granted for the one blocker beyond the first.
        driver.power(beast) shouldBe 6
        driver.toughness(beast) shouldBe 6
    }
})
