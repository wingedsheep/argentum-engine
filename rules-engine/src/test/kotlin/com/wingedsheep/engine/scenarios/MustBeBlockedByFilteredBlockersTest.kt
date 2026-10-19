package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustBeBlocked
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MustBeBlocked(allCreatures = true, blockers = <filter>)` — a Lure that binds only the potential
 * blockers matching the filter ("All Walls able to block this creature do so", Marble Priest).
 * Blockers outside the filter are free to block anything or nothing; matching blockers that can't
 * block it (tapped) aren't bound either.
 */
class MustBeBlockedByFilteredBlockersTest : FunSpec({

    val walls = GameObjectFilter.Creature.withSubtype("Wall")
    val priest = card("Test Wall Lure") {
        typeLine = "Creature — Cleric"
        power = 3
        toughness = 3
        staticAbility { ability = MustBeBlocked(allCreatures = true, blockers = walls) }
    }
    val wall = card("Test Wall") {
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
        keywords(Keyword.DEFENDER)
    }

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(priest, wall))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(d, p1, p2)
    }

    test("only blockers inside the filter are bound to block it") {
        val (d, p1, p2) = setup()
        val attacker = d.putCreatureOnBattlefield(p1, priest.name)
        d.removeSummoningSickness(attacker)
        val wallA = d.putCreatureOnBattlefield(p2, wall.name)
        val wallB = d.putCreatureOnBattlefield(p2, wall.name)
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(attacker), p2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)

        d.declareBlockers(p2, mapOf(wallA to listOf(attacker))).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(bears to listOf(attacker))).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(wallA to listOf(attacker), wallB to listOf(attacker))).outcome shouldBe Outcome.Done
    }

    test("a matching blocker that can't block isn't bound") {
        val (d, p1, p2) = setup()
        val attacker = d.putCreatureOnBattlefield(p1, priest.name)
        d.removeSummoningSickness(attacker)
        val tappedWall = d.putCreatureOnBattlefield(p2, wall.name)
        d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        d.tapPermanent(tappedWall)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(attacker), p2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)

        d.declareBlockers(p2, emptyMap()).outcome shouldBe Outcome.Done
    }

    test("the description reads the blocker filter") {
        MustBeBlocked(allCreatures = true, blockers = walls).description shouldBe
            "All Wall creatures able to block this creature do so"
    }
})
