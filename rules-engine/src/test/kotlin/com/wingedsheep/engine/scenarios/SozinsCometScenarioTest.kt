package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ForetoldComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.SozinsComet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sozin's Comet (TLA) — {3}{R}{R} — Sorcery — Mythic.
 *
 * - Each creature you control gains firebending 5 until end of turn.
 *   (Whenever it attacks, add {R}{R}{R}{R}{R}. This mana lasts until end of combat.)
 * - Foretell {2}{R}.
 *
 * The mass firebending grant reuses the single-target GrantFirebending inside a ForEachInGroup over
 * "creatures you control", so each of your creatures installs the same attack-trigger→combat-mana
 * behavior the printed keyword does — asserted here via attack→five combat-duration reds (firebending
 * is a granted triggered ability, not a projected keyword, so it can't be read off projected state).
 *
 * Foretell is the genuine new keyword (CR 702.143): a sorcery-speed special action that pays {2} and
 * exiles the card face down, castable from exile for its foretell cost {2}{R} on a *later* turn. The
 * exile-and-recast plumbing reuses Plot's structure plus Airbend's fixed alternative cast cost.
 */
class SozinsCometScenarioTest : FunSpec({

    // Vanilla beater used to carry the firebending grant and attack.
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SozinsComet, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Fully resolve the stack, resolving every triggered ability that lands on it. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.pendingDecision != null || state.stack.isNotEmpty()) && guard < 50) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.combatMana(playerId: EntityId) =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .filter { it.expiry == ManaExpiry.END_OF_COMBAT }

    /**
     * Advance play from the current (my) turn to my next precombat main phase.
     *
     * Each iteration ends the current turn and stops at the *following* turn's precombat main
     * (turnNumber is round-based — it increments only when the starting player begins a turn), so
     * the loop exits precisely on my own main phase in a later turn. Ending each iteration on
     * `PRECOMBAT_MAIN` (rather than looping to END and only afterwards advancing to a main) is what
     * keeps it from overshooting onto the opponent's turn.
     */
    fun GameTestDriver.advanceToMyNextMain(me: EntityId) {
        val startTurn = state.turnNumber
        do {
            passPriorityUntil(Step.END)
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (activePlayer != me || state.turnNumber == startTurn)
    }

    test("normal cast: each creature you control gains firebending 5; an attacker adds {R}{R}{R}{R}{R}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(me, "Test Bear")
        driver.removeSummoningSickness(creature)

        val comet = driver.putCardInHand(me, "Sozin's Comet")
        driver.giveMana(me, Color.RED, 5) // {3}{R}{R}
        driver.castSpell(me, comet).isSuccess shouldBe true
        driver.resolveStack()

        // The creature carries the granted firebending trigger.
        driver.state.grantedTriggeredAbilities.any { it.entityId == creature } shouldBe true

        // When it attacks this turn, firebending 5 adds five combat-duration reds.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(creature), opponent)
        driver.resolveStack()

        val combat = driver.combatMana(me)
        combat.size shouldBe 5
        combat.all { it.color == Color.RED } shouldBe true
    }

    test("foretell: pay {2} to exile it face down; it can't be cast the same turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val comet = driver.putCardInHand(me, "Sozin's Comet")
        driver.giveMana(me, Color.RED, 2) // foretell setup cost {2}

        driver.submitSuccess(ForetellCard(me, comet))

        // Left hand for exile, foretold + face down (hidden).
        driver.getHand(me).contains(comet) shouldBe false
        driver.getExile(me).contains(comet) shouldBe true
        driver.state.getEntity(comet)?.get<ForetoldComponent>() shouldNotBe null
        driver.state.getEntity(comet)?.has<FaceDownComponent>() shouldBe true

        // It cannot be cast the turn it was foretold (CR 702.143a).
        driver.giveMana(me, Color.RED, 3) // enough for {2}{R}, so the failure is timing, not mana
        driver.castSpell(me, comet).isSuccess shouldBe false
    }

    test("foretell: cast it from exile for {2}{R} on a later turn; the firebending grant happens") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(me, "Test Bear")
        val comet = driver.putCardInHand(me, "Sozin's Comet")
        driver.giveMana(me, Color.RED, 2)
        driver.submitSuccess(ForetellCard(me, comet))
        driver.getExile(me).contains(comet) shouldBe true

        // Advance to my next turn — now it may be cast from exile for its foretell cost.
        driver.advanceToMyNextMain(me)
        driver.removeSummoningSickness(creature)

        driver.giveMana(me, Color.RED, 3) // {2}{R}
        driver.castSpell(me, comet).isSuccess shouldBe true
        driver.resolveStack()

        // Resolved into a firebending grant on the creature I control.
        driver.state.grantedTriggeredAbilities.any { it.entityId == creature } shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(creature), opponent)
        driver.resolveStack()

        val combat = driver.combatMana(me)
        combat.size shouldBe 5
        combat.all { it.color == Color.RED } shouldBe true
    }
})
