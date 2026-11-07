package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MustBeBlocked(allCreatures = true, blockers = <filter>)` — a Lure that binds only the potential
 * blockers matching the filter ("All Walls able to block this creature do so", Marble Priest).
 * Blockers outside the filter are free to block anything or nothing; matching blockers that can't
 * block it (tapped) aren't bound either.
 *
 * `MustBeBlocked(blockers = <filter>)` (no `allCreatures`) — "must be blocked by an Eldrazi if able"
 * (Slayer's Cleaver): only a matching blocker obeys the requirement, one is enough, and the
 * declaration must obey as many requirements as possible (CR 509.1c) — counted per requirement, so
 * one Eldrazi blocking a creature under two Cleavers obeys both.
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

    val eldrazi = GameObjectFilter.Creature.withSubtype("Eldrazi")
    val eldraziBait = card("Test Eldrazi Bait") {
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
        staticAbility { ability = MustBeBlocked(blockers = eldrazi) }
    }
    val eldraziBanner = card("Test Eldrazi Banner") {
        typeLine = "Artifact"
        staticAbility { ability = MustBeBlocked(filter = GroupFilter.attachedCreature(), blockers = eldrazi) }
    }
    val plainBait = card("Test Plain Bait") {
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
        staticAbility { ability = MustBeBlocked() }
    }
    val drone = card("Test Eldrazi Drone") {
        typeLine = "Creature — Eldrazi Drone"
        power = 2
        toughness = 2
    }

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(priest, wall, eldraziBait, eldraziBanner, plainBait, drone))
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

    fun attackWith(d: GameTestDriver, p1: EntityId, p2: EntityId, attackers: List<EntityId>) {
        attackers.forEach { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, attackers, p2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
    }

    test("if able: only a matching blocker obeys it, and one is enough") {
        val (d, p1, p2) = setup()
        val bait = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val droneA = d.putCreatureOnBattlefield(p2, drone.name)
        val droneB = d.putCreatureOnBattlefield(p2, drone.name)
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        attackWith(d, p1, p2, listOf(bait))

        d.declareBlockers(p2, emptyMap()).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(bears to listOf(bait))).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(droneA to listOf(bait))).outcome shouldBe Outcome.Done
    }

    test("if able: other creatures stay free once one matching blocker blocks") {
        val (d, p1, p2) = setup()
        val bait = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val droneA = d.putCreatureOnBattlefield(p2, drone.name)
        val droneB = d.putCreatureOnBattlefield(p2, drone.name)
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        attackWith(d, p1, p2, listOf(bait))

        d.declareBlockers(
            p2, mapOf(droneA to listOf(bait), droneB to listOf(bait), bears to listOf(bait))
        ).outcome shouldBe Outcome.Done
    }

    test("if able: with no matching creature able to block, any declaration is fine") {
        val (d, p1, p2) = setup()
        val bait = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val tappedDrone = d.putCreatureOnBattlefield(p2, drone.name)
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        d.tapPermanent(tappedDrone)
        attackWith(d, p1, p2, listOf(bait))

        d.declareBlockers(p2, mapOf(bears to listOf(bait))).outcome shouldBe Outcome.Done
    }

    test("if able: no blocker is forced when no matching creature can block") {
        val (d, p1, p2) = setup()
        val bait = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        attackWith(d, p1, p2, listOf(bait))

        d.declareBlockers(p2, emptyMap()).outcome shouldBe Outcome.Done
    }

    test("if able: two requirements on one attacker are both obeyed by a single matching blocker") {
        val (d, p1, p2) = setup()
        val bait = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val banner = d.putPermanentOnBattlefield(p1, eldraziBanner.name)
        d.replaceState(
            d.state.updateEntity(banner) { it.with(AttachedToComponent(bait)) }
                .updateEntity(bait) { it.with(AttachmentsComponent(listOf(banner))) }
        )
        val droneA = d.putCreatureOnBattlefield(p2, drone.name)
        attackWith(d, p1, p2, listOf(bait))

        d.declareBlockers(p2, emptyMap()).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(droneA to listOf(bait))).outcome shouldBe Outcome.Done
    }

    test("if able: the declaration obeys the most requirements possible (CR 509.1c)") {
        val (d, p1, p2) = setup()
        val gated = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val open = d.putCreatureOnBattlefield(p1, plainBait.name)
        val droneA = d.putCreatureOnBattlefield(p2, drone.name)
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        attackWith(d, p1, p2, listOf(gated, open))

        // Drone on the open bait and Bears on the gated one obeys one requirement; two are possible.
        d.declareBlockers(p2, mapOf(droneA to listOf(open), bears to listOf(gated))).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(droneA to listOf(gated))).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(droneA to listOf(gated), bears to listOf(open))).outcome shouldBe Outcome.Done
    }

    test("if able: one matching blocker between two gated attackers obeys one requirement, which is the most possible") {
        val (d, p1, p2) = setup()
        val baitA = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val baitB = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val droneA = d.putCreatureOnBattlefield(p2, drone.name)
        attackWith(d, p1, p2, listOf(baitA, baitB))

        d.declareBlockers(p2, emptyMap()).outcome shouldNotBe Outcome.Done
        d.declareBlockers(p2, mapOf(droneA to listOf(baitB))).outcome shouldBe Outcome.Done
    }

    test("if able: the suggested declaration assigns only the matching blocker the requirement needs") {
        val (d, p1, p2) = setup()
        val bait = d.putCreatureOnBattlefield(p1, eldraziBait.name)
        val droneA = d.putCreatureOnBattlefield(p2, drone.name)
        d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        attackWith(d, p1, p2, listOf(bait))

        val suggestion = d.legalActions(p2).single { it.mandatoryBlockerAssignments != null }.mandatoryBlockerAssignments
        suggestion shouldBe mapOf(droneA to listOf(bait))
        d.declareBlockers(p2, suggestion!!).outcome shouldBe Outcome.Done
    }

    test("if able: the description reads the blocker filter") {
        MustBeBlocked(blockers = eldrazi).description shouldBe
            "This creature must be blocked by an Eldrazi creature if able"
        MustBeBlocked(filter = GroupFilter.attachedCreature(), blockers = eldrazi).description shouldBe
            "${GroupFilter.attachedCreature().description} must be blocked by an Eldrazi creature if able"
    }

    test("the description reads the blocker filter") {
        MustBeBlocked(allCreatures = true, blockers = walls).description shouldBe
            "All Wall creatures able to block this creature do so"
    }
})
