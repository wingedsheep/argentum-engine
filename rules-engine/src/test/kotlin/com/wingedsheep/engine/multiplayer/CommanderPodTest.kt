package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.sba.permanent.CommanderZoneChoiceCheck
import com.wingedsheep.engine.mechanics.sba.player.CommanderDamageLossCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Commander in a multiplayer pod (issue #1456).
 *
 * The premise of shipping Commander pods was that no commander code path assumes two players:
 * the command zone is per player, commander damage is tallied per *(commander, defending player)*
 * pair rather than per opponent pair, and the CR 903.9a zone choice loops `turnOrder`. That was
 * true but untested — `Format.Commander` had only ever been exercised at two seats, so a pod
 * relied on the *absence* of a 2-player assumption rather than on evidence.
 *
 * This pins the pod-shaped behaviour: seating and life at four seats, a commander's damage landing
 * on the defender it actually hit, tallies that neither pool across defenders nor across
 * commanders, a 21-damage loss that eliminates exactly one seat, and a zone choice that asks the
 * right owner when three commanders are in graveyards at once.
 */
class CommanderPodTest : FunSpec({

    /** A 7/7 legendary — big enough that three hits cross the 21-damage threshold. */
    val bigCommander = CardDefinition.creature(
        name = "Test Pod Commander",
        manaCost = ManaCost.parse("{4}{G}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 7,
        toughness = 7,
        supertypes = setOf(Supertype.LEGENDARY),
    )

    val bear = CardDefinition.creature(
        name = "Test Pod Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
    )

    /** A four-seat Commander pod, every seat running the same 99 bears behind the same commander. */
    fun podOfFour(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(listOf(bigCommander, bear))
        val players = driver.initMultiplayer(
            decks = List(4) { Deck(cards = List(99) { bear.name }) },
            format = Format.Commander(),
            commanders = List(4) { bigCommander.name },
            skipMulligans = true,
            startingPlayer = 0,
        )
        return driver to players
    }

    /**
     * Move [player]'s actual commander entity out of its command zone and onto the battlefield,
     * ready to attack. Deliberately *not* `putCreatureOnBattlefield`, which mints a fresh entity —
     * commander damage only accumulates from the entity carrying [CommanderComponent], so a test
     * that used a copy would pass while proving nothing.
     */
    fun GameTestDriver.commanderOntoBattlefield(player: EntityId): EntityId {
        val commanderId = state.getZone(ZoneKey(player, Zone.COMMAND)).single()
        replaceState(
            state
                .removeFromZone(ZoneKey(player, Zone.COMMAND), commanderId)
                .addToZone(ZoneKey(player, Zone.BATTLEFIELD), commanderId)
                .updateEntity(commanderId) { it.with(ControllerComponent(player)) }
        )
        return commanderId
    }

    // =========================================================================
    // Seating
    // =========================================================================

    test("a four-seat pod gives every player 40 life and their own commander in their own command zone") {
        val (driver, players) = podOfFour()
        players.size shouldBe 4

        for (playerId in players) {
            driver.getLifeTotal(playerId) shouldBe 40

            val commandZone = driver.state.getZone(ZoneKey(playerId, Zone.COMMAND))
            commandZone.size shouldBe 1
            val commanderId = commandZone.single()

            val commander = driver.state.getEntity(commanderId)!!
            commander.get<CardComponent>()!!.name shouldBe bigCommander.name
            // Own commander, not a shared one: the component names this seat as owner, and the
            // seat's registry lists exactly this entity.
            commander.get<CommanderComponent>()!!.ownerId shouldBe playerId
            driver.state.getEntity(playerId)!!.get<CommanderRegistryComponent>()!!
                .commanderIds shouldContainExactly listOf(commanderId)
        }

        // Four distinct commander entities, one per seat — no aliasing between command zones.
        players.map { driver.state.getZone(ZoneKey(it, Zone.COMMAND)).single() }
            .distinct().size shouldBe 4
    }

    // =========================================================================
    // Damage attribution through real combat
    // =========================================================================

    test("a commander's combat damage is tallied against the player it actually attacked, not the pod") {
        val (driver, players) = podOfFour()
        val (attacker, defender, bystanderA, bystanderB) =
            listOf(players[0], players[1], players[2], players[3])

        val commanderId = driver.commanderOntoBattlefield(attacker)
        driver.removeSummoningSickness(commanderId)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(commanderId), defender)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        // 7 damage: on the defender's life total and on the (commander, defender) tally.
        driver.getLifeTotal(defender) shouldBe 33
        driver.state.commanderDamageOf(commanderId, defender) shouldBe 7

        // The other two seats are untouched — a pod is not a single defending "side".
        for (bystander in listOf(bystanderA, bystanderB)) {
            driver.getLifeTotal(bystander) shouldBe 40
            driver.state.commanderDamageOf(commanderId, bystander) shouldBe 0
        }
    }

    // =========================================================================
    // The 21-damage threshold, per defender
    // =========================================================================

    /** The pod's state after the SBA has run once, so the loss checks read the same shape. */
    fun GameState.afterCommanderDamageCheck(): GameState = CommanderDamageLossCheck().check(this).newState

    test("one commander's damage does not pool across the defenders it hit") {
        val (driver, players) = podOfFour()
        val commanderId = driver.state.getZone(ZoneKey(players[0], Zone.COMMAND)).single()

        // 20 to each of three opponents — 60 total from one commander, nobody at the threshold.
        var state = driver.state
        for (defender in players.drop(1)) state = state.recordCommanderDamage(commanderId, defender, 20)

        val checked = state.afterCommanderDamageCheck()
        for (defender in players.drop(1)) {
            checked.getEntity(defender)!!.get<PlayerLostComponent>().shouldBeNull()
        }
    }

    test("21 damage from one commander eliminates exactly that defender, and the pod plays on") {
        val (driver, players) = podOfFour()
        val commanderId = driver.state.getZone(ZoneKey(players[0], Zone.COMMAND)).single()
        val doomed = players[1]

        val checked = driver.state
            .recordCommanderDamage(commanderId, doomed, 21)
            .recordCommanderDamage(commanderId, players[2], 20)
            .afterCommanderDamageCheck()

        val lost = checked.getEntity(doomed)!!.get<PlayerLostComponent>()
        lost shouldNotBe null
        lost!!.reason shouldBe LossReason.COMMANDER_DAMAGE

        for (survivor in listOf(players[0], players[2], players[3])) {
            checked.getEntity(survivor)!!.get<PlayerLostComponent>().shouldBeNull()
        }
    }

    test("damage from three different commanders never aggregates on one defender (CR 903.10a)") {
        val (driver, players) = podOfFour()
        val victim = players[0]
        val attackers = players.drop(1)

        // 20 each from three commanders — 60 damage at one seat, no commander-damage loss. The
        // victim would be dead on life total; that is a different loss condition and not this SBA's.
        var state = driver.state
        for (attacker in attackers) {
            val commanderId = state.getZone(ZoneKey(attacker, Zone.COMMAND)).single()
            state = state.recordCommanderDamage(commanderId, victim, 20)
        }

        state.afterCommanderDamageCheck()
            .getEntity(victim)!!.get<PlayerLostComponent>().shouldBeNull()

        // …but any one of them reaching 21 alone is lethal.
        val firstCommander = state.getZone(ZoneKey(attackers.first(), Zone.COMMAND)).single()
        state.recordCommanderDamage(firstCommander, victim, 1)
            .afterCommanderDamageCheck()
            .getEntity(victim)!!.get<PlayerLostComponent>()!!.reason shouldBe LossReason.COMMANDER_DAMAGE
    }

    test("the threshold is read from the format, so a pod preset's value applies at four seats") {
        val (driver, players) = podOfFour()
        val commanderId = driver.state.getZone(ZoneKey(players[0], Zone.COMMAND)).single()

        // A 16-damage Brawl-shaped threshold kills at 16 where the default 21 would not.
        val brawlish = driver.state
            .copy(format = Format.Commander(commanderDamageThreshold = 16, startingLife = 25))
            .recordCommanderDamage(commanderId, players[1], 16)
        brawlish.afterCommanderDamageCheck()
            .getEntity(players[1])!!.get<PlayerLostComponent>() shouldNotBe null

        driver.state.recordCommanderDamage(commanderId, players[1], 16)
            .afterCommanderDamageCheck()
            .getEntity(players[1])!!.get<PlayerLostComponent>().shouldBeNull()
    }

    // =========================================================================
    // CR 903.9a zone choice with more than two players
    // =========================================================================

    test("the zone choice asks one owner at a time, in turn order, with three commanders in graveyards") {
        val (driver, players) = podOfFour()
        val check = CommanderZoneChoiceCheck(DecisionHandler())

        // Seats 1, 2 and 3 all have their commander in their own graveyard at the same time.
        var state = driver.state
        val commanders = mutableMapOf<EntityId, EntityId>()
        for (playerId in players.drop(1)) {
            val commanderId = state.getZone(ZoneKey(playerId, Zone.COMMAND)).single()
            commanders[playerId] = commanderId
            state = state
                .removeFromZone(ZoneKey(playerId, Zone.COMMAND), commanderId)
                .addToZone(ZoneKey(playerId, Zone.GRAVEYARD), commanderId)
        }

        // One question per SBA pass, and it goes to the earliest seat in turn order that has one
        // pending — seat 0's commander is still in its command zone, so seat 1 is asked first.
        val first = check.check(state)
        first.isPaused shouldBe true
        val firstDecision = first.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        firstDecision.playerId shouldBe players[1]

        // Answering (either way) marks that commander as asked; the next pass moves to seat 2.
        val afterFirst = first.newState.updateEntity(commanders[players[1]]!!) {
            it.with(com.wingedsheep.engine.state.components.identity.CommanderZoneChoiceAskedComponent)
        }
        val second = check.check(afterFirst)
        second.isPaused shouldBe true
        second.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe players[2]
    }

    test("no seat is asked about a commander that is still in its command zone") {
        val (driver, _) = podOfFour()
        val result = CommanderZoneChoiceCheck(DecisionHandler()).check(driver.state)
        result.isPaused shouldBe false
    }

    // =========================================================================
    // Life totals stay per-seat
    // =========================================================================

    test("a pod keeps four independent life totals — Commander shares nothing (CR 808.5 / 810.4 do not apply)") {
        val (driver, players) = podOfFour()
        driver.setLifeTotal(players[1], 12)

        driver.state.getEntity(players[1])!!.get<LifeTotalComponent>()!!.life shouldBe 12
        for (other in listOf(players[0], players[2], players[3])) {
            driver.getLifeTotal(other) shouldBe 40
        }
    }
})
