package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.ThunderOfUnity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for Thunder of Unity (Tarkir: Dragonstorm #231) and, through it, the *filter-scoped*
 * event-based delayed triggered ability — "Whenever a creature you control enters this turn, …".
 *
 * {R}{W}{B} · Enchantment — Saga
 *   I — You draw two cards and you lose 2 life.
 *   II, III — Whenever a creature you control enters this turn, each opponent loses 1 life and
 *             you gain 1 life.
 *
 * Chapters II/III install a `CreateDelayedTriggerEffect` whose trigger is
 * `entersBattlefield(Creature.youControl(), binding = ANY)`, `fireOnce = false`,
 * `expiry = EndOfTurn`. Because this delayed trigger has no single *watched entity*, the delayed
 * trigger detector must scope it by the spec's GameObjectFilter — both the `IsCreature` type
 * predicate and the `you control` controller predicate. Before the fix,
 * `TriggerDetector.matchesEventForWatchedEntity` ignored the filter on ZoneChange delayed triggers
 * and fired for *every* permanent that entered (opponents' permanents, lands, the Saga itself,
 * etc.). The "does NOT ping" tests below are the regression guard for that bug.
 *
 * Note on turn numbers: `GameState.turnNumber` is the round number (both players' turns within a
 * round share it — see `TurnManager.startTurn`), so the controller's Saga gains a lore counter on
 * rounds 1/2/3 → chapter I is round 1, chapter II is round 2, chapter III is round 3 (after which
 * the Saga is sacrificed).
 */
class ThunderOfUnityTest : FunSpec({

    // A vanilla creature the controller can cast so a "creature you control" enters.
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // A non-creature permanent the controller can cast — must NOT ping the trigger (type filter).
    val totem = card("Test Totem") {
        manaCost = "{1}"
        typeLine = "Enchantment"
    }

    // A sorcery the controller casts that puts a 2/2 onto the battlefield under each OPPONENT's
    // control — an opponent-controlled creature entering must NOT ping the trigger (controller
    // filter), even though it happens on the controller's turn while the trigger is live.
    val giftBear = card("Gift a Bear") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = CreateTokenEffect(
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Bear"),
                name = "Bear",
                controller = EffectTarget.PlayerRef(Player.EachOpponent)
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThunderOfUnity, bear, totem, giftBear))
        return driver
    }

    /** Fully resolve the stack, resolving every triggered ability that lands on it. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            bothPass()
            guard++
        }
    }

    /**
     * Advance priority pass-by-pass until the active player's [targetRound] precombat main begins.
     * Stops the instant we enter that step, so any Saga chapter trigger queued by the lore-counter
     * turn-based action is still sitting on the stack (resolve it with [resolveStack]).
     */
    fun GameTestDriver.advanceToMain(targetRound: Int) {
        var guard = 0
        while (!(state.turnNumber == targetRound && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to round $targetRound")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
    }

    /** Cast Thunder of Unity on the first turn and resolve it + its chapter I. */
    fun GameTestDriver.castThunderOfUnity(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.RED, 1)
        giveMana(controller, Color.WHITE, 1)
        giveMana(controller, Color.BLACK, 1)
        val saga = putCardInHand(controller, "Thunder of Unity")
        castSpell(controller, saga)
        resolveStack() // saga enters (lore 1 → chapter I), then chapter I resolves
    }

    /** Cast a single Test Bear (the controller's creature) and resolve it + any triggers. */
    fun GameTestDriver.castBear(controller: EntityId) {
        giveMana(controller, Color.GREEN, 2)
        val bearCard = putCardInHand(controller, "Test Bear")
        castSpell(controller, bearCard)
        resolveStack()
    }

    /** Cast Thunder of Unity, then advance to its chapter II (round 2) and resolve it. */
    fun GameTestDriver.buildToChapterII(controller: EntityId) {
        castThunderOfUnity(controller)
        advanceToMain(2) // controller's second turn → lore 2 → chapter II
        resolveStack()
    }

    test("Chapter I — you draw two cards and lose 2 life; no delayed trigger yet") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        val handBefore = driver.getHandSize(controller)
        driver.castThunderOfUnity(controller)

        // Drew two; the Saga was injected into hand for the test and then cast (net zero), so the
        // observable change vs. the pre-cast hand is +2.
        driver.getHandSize(controller) shouldBe handBefore + 2
        driver.assertLifeTotal(controller, 18)
        // Chapter I does not install any delayed trigger.
        driver.state.delayedTriggers.size shouldBe 0
    }

    test("Chapter II — installs a turn-bounded 'creature you control enters' delayed trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.buildToChapterII(controller)

        // The Saga is still on the battlefield (lore 2 < final chapter 3).
        driver.findPermanent(controller, "Thunder of Unity").shouldNotBeNull()

        val delayed = driver.state.delayedTriggers
        delayed.size shouldBe 1
        delayed.first().trigger shouldBe Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY
        )
        // Fires on *every* matching enter (not one-shot) and lasts until end of turn.
        delayed.first().fireOnce shouldBe false
        delayed.first().expiry shouldBe DelayedTriggerExpiry.EndOfTurn
    }

    test("A creature you control entering pings; a second one pings again (fireOnce = false)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.buildToChapterII(controller)

        val ctrlLife = driver.getLifeTotal(controller)
        val oppLife = driver.getLifeTotal(opponent)

        // First creature you control enters → each opponent loses 1, you gain 1.
        driver.castBear(controller)
        driver.assertLifeTotal(opponent, oppLife - 1)
        driver.assertLifeTotal(controller, ctrlLife + 1)

        // Second creature enters the same turn → fires again (the ability is not one-shot).
        driver.castBear(controller)
        driver.assertLifeTotal(opponent, oppLife - 2)
        driver.assertLifeTotal(controller, ctrlLife + 2)
    }

    test("Regression: a NON-creature you control entering does NOT ping (type filter)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.buildToChapterII(controller)

        val ctrlLife = driver.getLifeTotal(controller)
        val oppLife = driver.getLifeTotal(opponent)

        // Cast a non-creature permanent (an enchantment) you control — it enters but is not a
        // creature, so the trigger must not fire. (Before the fix this fired for any permanent.)
        driver.giveMana(controller, Color.RED, 1)
        val totemCard = driver.putCardInHand(controller, "Test Totem")
        driver.castSpell(controller, totemCard)
        driver.resolveStack()

        driver.assertLifeTotal(opponent, oppLife)
        driver.assertLifeTotal(controller, ctrlLife)
    }

    test("Regression: an OPPONENT's creature entering does NOT ping (controller filter)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.buildToChapterII(controller)

        val ctrlLife = driver.getLifeTotal(controller)
        val oppLife = driver.getLifeTotal(opponent)

        // Controller casts a sorcery that puts a 2/2 onto the battlefield under the OPPONENT's
        // control. A creature enters this turn, but it isn't one *you* control, so the trigger
        // must not fire. (Before the fix, the "you control" predicate was ignored and this pinged.)
        driver.giveMana(controller, Color.RED, 1)
        val gift = driver.putCardInHand(controller, "Gift a Bear")
        driver.castSpell(controller, gift)
        driver.resolveStack()

        // The opponent really did get a creature…
        driver.getCreatures(opponent).isEmpty().shouldBeFalse()
        // …but no life changed.
        driver.assertLifeTotal(opponent, oppLife)
        driver.assertLifeTotal(controller, ctrlLife)
    }

    test("The delayed trigger expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        // Chapter III (round 3) installs the delayed trigger, then the Saga is sacrificed. Using
        // chapter III means no later chapter re-installs the ability, so we can observe the expiry.
        driver.castThunderOfUnity(controller)
        driver.advanceToMain(3)
        driver.resolveStack()
        driver.state.delayedTriggers.size shouldBe 1

        // Advance to the controller's next turn (round 4); the "this turn" trigger is gone and no
        // chapter re-installs it (the Saga left after III), so a creature entering does nothing.
        driver.advanceToMain(4)
        driver.state.delayedTriggers.size shouldBe 0

        val ctrlLife = driver.getLifeTotal(controller)
        val oppLife = driver.getLifeTotal(opponent)
        driver.castBear(controller)
        driver.assertLifeTotal(opponent, oppLife)
        driver.assertLifeTotal(controller, ctrlLife)
    }

    test("Chapter III installs the trigger and it survives the Saga's post-III sacrifice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.castThunderOfUnity(controller)
        driver.advanceToMain(3) // lore 3 → chapter III on the stack
        driver.resolveStack() // chapter III resolves (installs trigger), then the Saga is sacrificed

        // Saga is gone (sacrificed as a state-based action after its final chapter)…
        driver.findPermanent(controller, "Thunder of Unity") shouldBe null
        // …but the delayed trigger persists this turn, decoupled from its source.
        driver.state.delayedTriggers.size shouldBe 1

        // A creature you control entering still pings, even though the Saga that created the
        // ability has left the battlefield.
        val ctrlLife = driver.getLifeTotal(controller)
        val oppLife = driver.getLifeTotal(opponent)
        driver.castBear(controller)
        driver.assertLifeTotal(opponent, oppLife - 1)
        driver.assertLifeTotal(controller, ctrlLife + 1)
    }
})
