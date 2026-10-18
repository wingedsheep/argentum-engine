package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Durations that reach into a later turn:
 *  - [Duration.EndOfYourNextUpkeep] — survives the upkeep it was created in and every other
 *    player's turn, and ends as the controller's following upkeep step ends (Halfdane).
 *  - [Duration.EndOfAffectedControllersNextTurn] — ends at the cleanup of the affected creature's
 *    controller's next turn; the creating turn never counts, even when it is that player's own
 *    (Wall of Dust).
 *  - [Duration.UntilAffectedOwnersNextTurn] — ends as the affected object's owner's next turn
 *    begins, whoever created it.
 *  - [Duration.UntilAfterAffectedControllersNextUntap] with `count = 2` — counts the affected creature's
 *    controller's untap steps down, one per step (Telekinesis).
 */
class LaterTurnDurationsTest : FunSpec({

    val upkeepBuff = card("Upkeep Window Buff") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, t, Duration.EndOfYourNextUpkeep)
        }
    }
    val attackLock = card("Next Turn Attack Lock") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.CantAttack(t, Duration.EndOfAffectedControllersNextTurn)
        }
    }
    val longFreeze = card("Two Step Freeze") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Tap(t) then
                Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, t, Duration.UntilAfterAffectedControllersNextUntap(count = 2))
        }
    }

    val ownerWindowBuff = card("Owner Window Buff") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, t, Duration.UntilAffectedOwnersNextTurn)
        }
    }

    // Built once with the spec rather than inside the first test, so loading the card corpus
    // doesn't count against that test's timeout.
    val cards = TestCards.all + listOf(upkeepBuff, attackLock, longFreeze, ownerWindowBuff)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player1)
        return driver
    }

    fun GameTestDriver.castAndResolve(player: EntityId, name: String, target: EntityId) {
        castSpell(player, putCardInHand(player, name), listOf(target)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty()) {
            check(guard++ < 10) { "stack didn't resolve: pending ${state.pendingDecision}" }
            bothPass()
        }
    }

    fun GameTestDriver.has(duration: Duration) = state.floatingEffects.any { it.duration == duration }
    fun GameTestDriver.power(id: EntityId) = state.projectedState.getPower(id)

    test("until the end of your next upkeep: outlives the creating upkeep, ends as the next own upkeep ends") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.UPKEEP, p1)
        val creatingTurn = d.state.turnNumber
        d.castAndResolve(p1, upkeepBuff.name, bear)
        d.power(bear) shouldBe 4

        d.passPriorityUntil(Step.DRAW, p1)
        withClue("the upkeep it was created in never counts") { d.power(bear) shouldBe 4 }

        d.passPriorityUntil(Step.DRAW, p2)
        withClue("the opponent's upkeep doesn't end it") { d.power(bear) shouldBe 4 }

        d.passPriorityUntil(Step.UPKEEP, p1)
        d.state.turnNumber shouldBe creatingTurn + 2
        withClue("still in force during the next own upkeep") { d.power(bear) shouldBe 4 }

        d.passPriorityUntil(Step.DRAW, p1)
        withClue("gone once that upkeep step has ended") {
            d.power(bear) shouldBe 2
            d.has(Duration.EndOfYourNextUpkeep) shouldBe false
        }
    }

    test("during its controller's next turn: created on that controller's own turn, it lasts through their following turn") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")

        d.castAndResolve(p1, attackLock.name, bear)
        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("the creating turn never counts, even when it is the controller's own") {
            d.has(Duration.EndOfAffectedControllersNextTurn) shouldBe true
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS, p1)
        withClue("in force throughout the controller's next turn") {
            d.has(Duration.EndOfAffectedControllersNextTurn) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("ended at the cleanup of that turn") {
            d.has(Duration.EndOfAffectedControllersNextTurn) shouldBe false
        }
    }

    test("during its controller's next turn: created on another player's turn, it ends with the controller's next turn") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val opposingBear = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        d.castAndResolve(p1, attackLock.name, opposingBear)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, p2)
        withClue("in force on the affected creature's controller's next turn") {
            d.has(Duration.EndOfAffectedControllersNextTurn) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP, p1)
        withClue("ended at the cleanup of that turn") {
            d.has(Duration.EndOfAffectedControllersNextTurn) shouldBe false
        }
    }

    test("next two untap steps: the creature stays tapped through two of its controller's untap steps") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val bear = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        d.castAndResolve(p1, longFreeze.name, bear)
        d.isTapped(bear) shouldBe true

        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("first untap step: still tapped, one step left") {
            d.isTapped(bear) shouldBe true
            d.has(Duration.UntilAfterAffectedControllersNextUntap()) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP, p1)
        withClue("another player's untap step doesn't count") {
            d.isTapped(bear) shouldBe true
            d.has(Duration.UntilAfterAffectedControllersNextUntap()) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("second untap step: still tapped, and the effect is spent") {
            d.isTapped(bear) shouldBe true
            d.has(Duration.UntilAfterAffectedControllersNextUntap()) shouldBe false
        }

        d.passPriorityUntil(Step.UPKEEP, p1)
        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("third untap step: it untaps") { d.isTapped(bear) shouldBe false }
    }

    test("until its owner's next turn: keyed to the affected object's owner, not the effect's controller") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val opposingBear = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val ownBear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")

        d.castAndResolve(p1, ownerWindowBuff.name, opposingBear)
        d.castAndResolve(p1, ownerWindowBuff.name, ownBear)
        d.power(opposingBear) shouldBe 4
        d.power(ownBear) shouldBe 4

        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("the opposing owner's turn ends that one, not the caster's") {
            d.power(opposingBear) shouldBe 2
            d.power(ownBear) shouldBe 4
        }

        d.passPriorityUntil(Step.UPKEEP, p1)
        withClue("the caster-owned creature's lasts until its owner's next turn") { d.power(ownBear) shouldBe 2 }
    }

    test("the untap-step count must be positive and reads as a number of steps") {
        runCatching { Duration.UntilAfterAffectedControllersNextUntap(count = 0) }.isFailure shouldBe true
        Duration.UntilAfterAffectedControllersNextUntap(count = 2).description shouldBe
            "during its controller's next two untap steps"
    }
})
