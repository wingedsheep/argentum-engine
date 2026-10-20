package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.RapidFire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RapidFireScenarioTest : FunSpec({

    val rampager = card("Rapid Fire Test Rampager") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        rampage(1)
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RapidFire, rampager))
        initMirrorMatch(Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0

    fun GameTestDriver.castRapidFire(target: EntityId): String? {
        val me = activePlayer!!
        val spell = putCardInHand(me, "Rapid Fire")
        giveMana(me, Color.WHITE, 4)
        return castSpell(me, spell, listOf(target)).error
    }

    fun GameTestDriver.attacker(name: String): EntityId =
        putCreatureOnBattlefield(activePlayer!!, name).also(::removeSummoningSickness)

    fun GameTestDriver.attackIntoBears(attacker: EntityId, blockers: Int) {
        val me = activePlayer!!
        val opp = getOpponent(me)
        val bears = List(blockers) { putCreatureOnBattlefield(opp, "Grizzly Bears") }
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(attacker), opp).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(opp, bears.associateWith { listOf(attacker) }).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("precombat, it grants first strike and rampage 2") {
        val d = driver()
        val bears = d.attacker("Grizzly Bears")
        d.castRapidFire(bears) shouldBe null
        d.bothPass()

        d.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        d.state.projectedState.hasKeyword(bears, Keyword.RAMPAGE) shouldBe true

        d.attackIntoBears(bears, blockers = 3)
        d.power(bears) shouldBe 6
    }

    test("a creature that already has rampage gets first strike but no rampage 2") {
        val d = driver()
        val beast = d.attacker(rampager.name)
        d.castRapidFire(beast) shouldBe null
        d.bothPass()
        d.state.projectedState.hasKeyword(beast, Keyword.FIRST_STRIKE) shouldBe true

        // Only the printed rampage 1: +1/+1 for each of the two blockers beyond the first.
        d.attackIntoBears(beast, blockers = 3)
        d.power(beast) shouldBe 4
    }

    test("a second Rapid Fire sees the first one's rampage and adds none") {
        val d = driver()
        val bears = d.attacker("Grizzly Bears")
        d.castRapidFire(bears) shouldBe null
        d.bothPass()
        d.castRapidFire(bears) shouldBe null
        d.bothPass()

        d.attackIntoBears(bears, blockers = 2)
        d.power(bears) shouldBe 4
    }

    test("castable after attackers are declared, not once blockers are") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.attacker("Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bears), opp).error shouldBe null
        d.castRapidFire(bears) shouldBe null
        d.bothPass()
        d.state.projectedState.hasKeyword(bears, Keyword.RAMPAGE) shouldBe true

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, emptyMap()).error shouldBe null
        d.castRapidFire(bears) shouldNotBe null
    }

    test("not castable after the first combat") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.attacker("Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, emptyList(), d.getOpponent(me)).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.castRapidFire(bears) shouldNotBe null
    }
})
