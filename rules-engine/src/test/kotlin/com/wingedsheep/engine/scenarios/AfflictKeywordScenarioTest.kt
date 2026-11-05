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
 * Engine tests for afflict N (CR 702.130) as a keyword-derived trigger, printed on a card with the
 * `afflict(n)` builder helper.
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | 702.130a — becomes blocked → defending player loses N | "afflict triggers once however many creatures block" |
 * | 509.3c — one trigger per combat for several blockers | "afflict triggers once however many creatures block" |
 * | 702.130a — nothing when unblocked | "an unblocked attacker doesn't afflict" |
 * | 702.130b — each instance triggers separately | "two printed instances trigger separately" |
 * | afflict is an ability — lose all abilities, no trigger | "a creature that lost its abilities has no afflict" |
 * | defending player still loses life if the creature leaves before resolution | "the defending player loses life even if the afflict creature dies in response" |
 * | ruling — resolves before combat damage; 0 life loses the game | "afflict can kill before combat damage" |
 */
class AfflictKeywordScenarioTest : FunSpec({

    val afflictTwo = card("Test Afflict Two") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        afflict(2)
    }

    val doubleAfflict = card("Test Double Afflict") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
        afflict(1)
        afflict(2)
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
        listOf(afflictTwo, doubleAfflict, strip, kill).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

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

    test("afflict triggers once however many creatures block") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        val zombie = driver.attacker("Test Afflict Two")
        driver.attackInto(zombie, driver.blockers(3)) shouldBe 1
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getLifeTotal(driver.activePlayer!!) shouldBe 20
    }

    test("an unblocked attacker doesn't afflict") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        val zombie = driver.attacker("Test Afflict Two")
        driver.attackInto(zombie, emptyList()) shouldBe 0

        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("two printed instances trigger separately") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        val zombie = driver.attacker("Test Double Afflict")
        driver.attackInto(zombie, driver.blockers(1)) shouldBe 2
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("a creature that lost its abilities has no afflict") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        val zombie = driver.attacker("Test Afflict Two")
        driver.castAndResolve("Test Strip", zombie, Color.BLUE)
        driver.state.projectedState.hasKeyword(zombie, Keyword.AFFLICT) shouldBe false

        driver.attackInto(zombie, driver.blockers(1)) shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("the defending player loses life even if the afflict creature dies in response") {
        val driver = createDriver()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        val zombie = driver.attacker("Test Afflict Two")
        driver.attackInto(zombie, driver.blockers(1)) shouldBe 1

        driver.castAndResolve("Test Kill", zombie, Color.BLACK, caster = driver.state.priorityPlayerId!!)
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("afflict can kill before combat damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.setLifeTotal(opponent, 2)
        val zombie = driver.attacker("Test Afflict Two")
        driver.attackInto(zombie, driver.blockers(1)) shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 0
        driver.state.gameOver shouldBe true
        driver.state.winnerId shouldBe me
    }
})
