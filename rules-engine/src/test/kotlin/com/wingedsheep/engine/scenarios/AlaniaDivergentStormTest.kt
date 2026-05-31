package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.AlaniaDivergentStorm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Alania, Divergent Storm {3}{U}{R} — Otter Wizard 3/5.
 * "Whenever you cast a spell, if it's the first instant spell, the first sorcery spell,
 *  or the first Otter spell other than Alania you've cast this turn, you may have target
 *  opponent draw a card. If you do, copy that spell."
 *
 * The intervening-if is built from `Conditions.YouCastFirstSpellOfTypeThisTurn(filter)`,
 * which composes `TriggeringSpellMatches(filter)` with `Not(YouCastSpellsThisTurn(2, filter))`
 * on top of the shared `PlayerCastSpellsThisTurn` count primitive (no bespoke counting).
 *
 * These tests pin the load-bearing part of that composition: the `TriggeringSpellMatches`
 * guard. Without it, casting a *non-matching* spell after one matching spell would wrongly
 * satisfy the count-based half and fire the ability — the exact bug a count-only
 * decomposition would introduce.
 */
class AlaniaDivergentStormTest : FunSpec({

    // Inline Otter creature for the "first Otter spell" branch — avoids depending on
    // set-specific Otters and keeps the cast cheap.
    val TestOtter = CardDefinition.creature(
        name = "Test Otter",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Otter")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AlaniaDivergentStorm, TestOtter))
        return driver
    }

    /**
     * Resolve the entire stack after a cast, declining Alania's optional "may have target
     * opponent draw a card" prompt so the board stays clean for the next cast. Returns
     * whether that prompt was ever offered — i.e. whether Alania's ability triggered.
     */
    fun GameTestDriver.castAndDidAlaniaFire(controller: EntityId, cast: () -> Unit): Boolean {
        cast()
        var fired = false
        var guard = 0
        while (stackSize > 0 && guard++ < 60) {
            val decision = pendingDecision
            when {
                decision is YesNoDecision -> {
                    fired = true
                    submitYesNo(controller, false) // decline the optional draw+copy
                }
                decision != null -> autoResolveDecision() // e.g. Careful Study's discard
                else -> bothPass()
            }
        }
        return fired
    }

    fun GameTestDriver.setup(): Pair<EntityId, EntityId> {
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = activePlayer!!
        val opponent = getOpponent(you)
        // Put Alania directly on the battlefield so casting it doesn't count toward the turn.
        putCreatureOnBattlefield(you, "Alania, Divergent Storm")
        return you to opponent
    }

    test("fires on the first instant spell of the turn") {
        val driver = createDriver()
        val (you, opponent) = driver.setup()

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")

        val fired = driver.castAndDidAlaniaFire(you) {
            driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        }
        fired shouldBe true
    }

    test("fires on the first sorcery spell of the turn") {
        val driver = createDriver()
        val (you, _) = driver.setup()

        driver.giveMana(you, Color.BLACK, 1)
        val study = driver.putCardInHand(you, "Careful Study")

        val fired = driver.castAndDidAlaniaFire(you) {
            driver.castSpell(you, study, emptyList()).error shouldBe null
        }
        fired shouldBe true
    }

    test("fires on the first Otter spell of the turn") {
        val driver = createDriver()
        val (you, _) = driver.setup()

        driver.giveMana(you, Color.BLUE, 1)
        val otter = driver.putCardInHand(you, "Test Otter")

        val fired = driver.castAndDidAlaniaFire(you) {
            driver.castSpell(you, otter, emptyList()).error shouldBe null
        }
        fired shouldBe true
    }

    test("does NOT fire on a second instant cast the same turn") {
        val driver = createDriver()
        val (you, opponent) = driver.setup()

        driver.giveMana(you, Color.RED, 2)
        val bolt1 = driver.putCardInHand(you, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(you, "Lightning Bolt")

        driver.castAndDidAlaniaFire(you) {
            driver.castSpellWithTargets(you, bolt1, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        } shouldBe true

        // Second instant this turn — the count half (Not(>= 2 instants)) is now false.
        driver.castAndDidAlaniaFire(you) {
            driver.castSpellWithTargets(you, bolt2, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        } shouldBe false
    }

    test("guard: does NOT fire on a non-matching spell cast after one matching spell") {
        val driver = createDriver()
        val (you, opponent) = driver.setup()

        // First an instant — fires.
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castAndDidAlaniaFire(you) {
            driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        } shouldBe true

        // Then a vanilla (non-instant, non-sorcery, non-Otter) creature. One instant has been
        // cast this turn, so a count-only decomposition would wrongly fire the instant branch.
        // The TriggeringSpellMatches guard keeps it correct: the creature matches no branch.
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 2)
        val courser = driver.putCardInHand(you, "Centaur Courser")
        driver.castAndDidAlaniaFire(you) {
            driver.castSpell(you, courser, emptyList()).error shouldBe null
        } shouldBe false
    }
})
