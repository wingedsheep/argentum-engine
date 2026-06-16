package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BreechesTheBlastmaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Breeches, the Blastmaker {1}{U}{R} — Goblin Pirate 3/3, Menace.
 * "Whenever you cast your second spell each turn, you may sacrifice an artifact. If you do,
 *  flip a coin. When you win the flip, copy that spell. You may choose new targets for the copy.
 *  When you lose the flip, Breeches deals damage equal to that spell's mana value to any target."
 *
 * These tests pin the load-bearing composition: the second-spell trigger, the optional
 * sacrifice gate (no sacrifice → no flip), and both flip branches (win → no self-damage,
 * lose → damage equal to the triggering spell's mana value). The coin flip is random, so the
 * branch tests loop until they observe each outcome (à la SkittishValeskTest).
 */
class BreechesTheBlastmakerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BreechesTheBlastmaker))
        return driver
    }

    /** Put Breeches on the battlefield for the active player; give an artifact to sacrifice. */
    fun GameTestDriver.setup(): Triple<EntityId, EntityId, EntityId> {
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = activePlayer!!
        val opponent = getOpponent(you)
        putCreatureOnBattlefield(you, "Breeches, the Blastmaker")
        val artifact = putPermanentOnBattlefield(you, "Artifact Creature")
        return Triple(you, opponent, artifact)
    }

    /** Cast a Lightning Bolt at the opponent (the "first spell" of the turn) and resolve it. */
    fun GameTestDriver.castFirstSpell(you: EntityId, opponent: EntityId) {
        giveMana(you, Color.RED, 1)
        val bolt1 = putCardInHand(you, "Lightning Bolt")
        castSpellWithTargets(you, bolt1, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        bothPass() // resolve bolt1
    }

    /** Cast the second Lightning Bolt at the opponent. */
    fun GameTestDriver.castSecondSpell(you: EntityId, opponent: EntityId): ExecutionResult {
        giveMana(you, Color.RED, 1)
        val bolt2 = putCardInHand(you, "Lightning Bolt")
        return castSpellWithTargets(you, bolt2, listOf(ChosenTarget.Player(opponent)))
    }

    /**
     * Resolve the whole stack after the second spell is cast, optionally accepting Breeches'
     * sacrifice gate. Handles three decision shapes:
     *  - the "any target" Breeches locks at trigger time (a target-selection → the opponent),
     *  - the "you may sacrifice an artifact" may-pay yes/no gate,
     *  - the follow-up selection of which artifact to sacrifice.
     * Accumulates every emitted event so the coin flip is observable regardless of which
     * continuation frame surfaces it. Returns the list of [CoinFlipEvent]s seen.
     */
    fun GameTestDriver.resolveBreechesTrigger(
        you: EntityId,
        opponent: EntityId,
        artifact: EntityId,
        acceptSacrifice: Boolean
    ): List<CoinFlipEvent> {
        val flips = mutableListOf<CoinFlipEvent>()
        var answeredGate = false
        var guard = 0
        while (stackSize > 0 && guard++ < 40) {
            val decision = pendingDecision
            when {
                decision == null -> {
                    flips += bothPass().events.filterIsInstance<CoinFlipEvent>()
                }
                decision is ChooseTargetsDecision -> {
                    // Breeches' "any target" for the lose-flip damage — point it at the opponent.
                    submitTargetSelection(you, listOf(opponent))
                }
                decision is YesNoDecision && !answeredGate -> {
                    answeredGate = true
                    flips += submitYesNo(you, acceptSacrifice).events.filterIsInstance<CoinFlipEvent>()
                }
                decision is SelectCardsDecision -> {
                    // Choosing which artifact to sacrifice for the may-pay cost.
                    flips += submitCardSelection(you, listOf(artifact)).events
                        .filterIsInstance<CoinFlipEvent>()
                }
                else -> autoResolveDecision()
            }
        }
        return flips
    }

    test("the second spell each turn triggers the optional sacrifice; declining skips the flip") {
        val driver = createDriver()
        val (you, opponent, artifact) = driver.setup()

        driver.castFirstSpell(you, opponent)
        driver.castSecondSpell(you, opponent).error shouldBe null

        val flips = driver.resolveBreechesTrigger(you, opponent, artifact, acceptSacrifice = false)
        // Declining the sacrifice means no coin is flipped.
        flips.isEmpty().shouldBeTrue()
        // Artifact survives (still on the battlefield).
        (driver.state.getEntity(artifact) != null).shouldBeTrue()
    }

    test("winning the flip deals no damage to the opponent beyond the two bolts") {
        var foundWin = false
        var iterations = 0
        while (!foundWin && iterations++ < 80) {
            val driver = createDriver()
            val (you, opponent, artifact) = driver.setup()
            val oppLifeBefore = driver.getLifeTotal(opponent)

            driver.castFirstSpell(you, opponent)
            driver.castSecondSpell(you, opponent).error shouldBe null

            val flips = driver.resolveBreechesTrigger(you, opponent, artifact, acceptSacrifice = true)
            val flip = flips.firstOrNull() ?: continue

            if (flip.won) {
                foundWin = true
                // Two bolts (3 + 3) only; the win branch copies the spell, it deals no Breeches damage.
                // (The copy targets the opponent too, dealing another 3 — total 9 from win.)
                driver.getLifeTotal(opponent) shouldBe (oppLifeBefore - 9)
            }
        }
        foundWin.shouldBeTrue()
    }

    test("losing the flip deals damage equal to the triggering spell's mana value") {
        var foundLoss = false
        var iterations = 0
        while (!foundLoss && iterations++ < 80) {
            val driver = createDriver()
            val (you, opponent, artifact) = driver.setup()
            val oppLifeBefore = driver.getLifeTotal(opponent)

            driver.castFirstSpell(you, opponent)
            driver.castSecondSpell(you, opponent).error shouldBe null

            val flips = driver.resolveBreechesTrigger(you, opponent, artifact, acceptSacrifice = true)
            val flip = flips.firstOrNull() ?: continue

            if (!flip.won) {
                foundLoss = true
                // First bolt (3) + second bolt (3) + Breeches lose-flip damage = bolt2 mana value (1).
                driver.getLifeTotal(opponent) shouldBe (oppLifeBefore - 7)
            }
        }
        foundLoss.shouldBeTrue()
    }
})
