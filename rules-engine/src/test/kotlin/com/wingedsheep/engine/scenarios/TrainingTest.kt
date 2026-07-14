package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.CloakedCadet
import com.wingedsheep.mtg.sets.definitions.vow.cards.GryffRider
import com.wingedsheep.mtg.sets.definitions.vow.cards.Torens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Training (CR 702.149, Innistrad: Midnight Hunt / VOW) — the attack-triggered self-buff proven
 * end-to-end.
 *
 * CR 702.149a — "Training is a triggered ability. 'Training' means 'Whenever this creature and at
 * least one other creature with power greater than this creature's power attack, put a +1/+1
 * counter on this creature.'"
 * CR 702.149b — "If a creature has multiple instances of training, each triggers separately."
 *
 * The engine models this as an attack trigger gated by
 * [com.wingedsheep.sdk.scripting.events.AttackPredicate.AttackedAlongsideGreaterPower], whose
 * matcher reads **projected** power for every attacker (Rule 613 layers). The test matrix mirrors
 * `training.md` §5.4:
 *  1. attacks alone → no counter;
 *  2. attacks alongside only lower/equal power → no counter;
 *  3. attacks alongside strictly-greater power → exactly one counter;
 *  4. a pump on the *other* attacker flips no-trigger → trigger (proving the comparison is over
 *     projected P/T, not printed);
 *  plus 702.149b (two instances trigger separately → two counters), Cloaked Cadet's draw watcher
 *  (its own Training counter feeds it, and it draws only once per turn), and Torens' Human Soldier
 *  token training on its own.
 */
class TrainingTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GryffRider)
        driver.registerCard(CloakedCadet)
        driver.registerCard(Torens)
        driver.registerCard(TwinTrainer)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        return driver
    }

    /** Read the +1/+1 counter count on a permanent. */
    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * Resolve everything currently on the stack (and any decisions it raises) without advancing the
     * turn past it — the multi-trigger drain used by other scenario tests. Guarded by stack size so
     * we never pass out of the step once the triggers are gone.
     */
    fun GameTestDriver.drainStack(player: EntityId) {
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard < 20) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /** Find a single created token by its display name. */
    fun GameTestDriver.tokenNamed(playerId: EntityId, name: String): EntityId? =
        getPermanents(playerId).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    // ─────────────────────────────────────────────────────────────────────────
    // §5.4 case 1 — attacks alone → no counter
    // ─────────────────────────────────────────────────────────────────────────
    test("training does not trigger when the creature attacks alone") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val gryff = driver.putCreatureOnBattlefield(me, "Gryff Rider")
        driver.removeSummoningSickness(gryff)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gryff), opp)
        driver.drainStack(me)

        driver.plusOneCounters(gryff) shouldBe 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §5.4 case 2 — attacks alongside only lower/equal power → no counter
    // ─────────────────────────────────────────────────────────────────────────
    test("training does not trigger when no other attacker has strictly greater power") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val gryff = driver.putCreatureOnBattlefield(me, "Gryff Rider")          // power 2
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")       // power 1 (lower)
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Guide")        // power 2 (equal)
        listOf(gryff, lions, goblin).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gryff, lions, goblin), opp)
        driver.drainStack(me)

        // Neither a lower-power (1) nor an equal-power (2) partner satisfies "strictly greater".
        driver.plusOneCounters(gryff) shouldBe 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §5.4 case 3 — attacks alongside strictly-greater power → exactly one counter
    // ─────────────────────────────────────────────────────────────────────────
    test("training triggers once when attacking alongside a creature of strictly greater power") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val gryff = driver.putCreatureOnBattlefield(me, "Gryff Rider")          // power 2
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")    // power 3 (greater)
        listOf(gryff, centaur).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gryff, centaur), opp)
        driver.drainStack(me)

        driver.plusOneCounters(gryff) shouldBe 1
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §5.4 case 4 — a pump on the OTHER attacker flips no-trigger → trigger.
    // Same board as case 2's equal-power partner (Goblin Guide, power 2), but Giant Growth makes it
    // 5/4 before attackers are declared. The matcher reads projected power, so the +3/+3 on the
    // *other* creature is what tips Gryff Rider (power 2) into "attacked alongside greater power".
    // ─────────────────────────────────────────────────────────────────────────
    test("a pump on the other attacker makes training trigger (projected power, not printed)") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val gryff = driver.putCreatureOnBattlefield(me, "Gryff Rider")          // power 2
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Guide")        // power 2 (equal, until pumped)
        listOf(gryff, goblin).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val giantGrowth = driver.putCardInHand(me, "Giant Growth")              // {G}: +3/+3 EOT
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, giantGrowth, targets = listOf(goblin)).isSuccess shouldBe true
        driver.drainStack(me)                                                   // resolve Giant Growth → Goblin Guide is 5/4

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(gryff, goblin), opp)
        driver.drainStack(me)

        // Goblin Guide's projected power (5) now exceeds Gryff Rider's (2) → Training fires.
        driver.plusOneCounters(gryff) shouldBe 1
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CR 702.149b — multiple instances of training trigger separately → two counters.
    // ─────────────────────────────────────────────────────────────────────────
    test("two instances of training put two counters (CR 702.149b)") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val twin = driver.putCreatureOnBattlefield(me, "Twin Trainer")          // power 2, training x2
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")    // power 3 (greater)
        listOf(twin, centaur).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(twin, centaur), opp)
        driver.drainStack(me)

        // Both instances triggered off the single attack; each resolved a +1/+1 counter.
        driver.plusOneCounters(twin) shouldBe 2
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cloaked Cadet — its own Training counter feeds the draw watcher, which draws only once per
    // turn no matter how many +1/+1 counters land on Humans that turn.
    // ─────────────────────────────────────────────────────────────────────────
    test("Cloaked Cadet draws exactly once even when two Humans train the same turn") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val cadet = driver.putCreatureOnBattlefield(me, "Cloaked Cadet")        // Human, power 2, training
        val gryff = driver.putCreatureOnBattlefield(me, "Gryff Rider")          // Human, power 2, training
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")    // power 3 (greater)
        listOf(cadet, gryff, centaur).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHand(me).size
        driver.declareAttackers(me, listOf(cadet, gryff, centaur), opp)
        driver.drainStack(me)

        // Both Humans trained (one +1/+1 counter each) — two separate counter-placement events…
        driver.plusOneCounters(cadet) shouldBe 1
        driver.plusOneCounters(gryff) shouldBe 1
        // …but the "draw a card" ability triggers only once each turn (oncePerTurn): exactly +1.
        driver.getHand(me).size shouldBe handBefore + 1
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Torens, Fist of the Angels — the 1/1 Human Soldier token it makes has Training and trains on
    // its own when it later attacks alongside a creature of greater power.
    // ─────────────────────────────────────────────────────────────────────────
    test("Torens' Human Soldier token has training and trains itself") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val torens = driver.putCreatureOnBattlefield(me, "Torens, Fist of the Angels")
        driver.removeSummoningSickness(torens)

        // Cast a creature spell → Torens creates a 1/1 Human Soldier token with training. The
        // Centaur (power 3) will be the greater-power partner the token trains alongside.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val centaurCard = driver.putCardInHand(me, "Centaur Courser")           // {2}{G}
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, centaurCard).isSuccess shouldBe true
        driver.drainStack(me)                                                   // resolve Torens' trigger + the Centaur spell

        val tokenId = driver.tokenNamed(me, "Human Soldier Token")
            ?: error("Torens should have created a Human Soldier Token")
        driver.plusOneCounters(tokenId) shouldBe 0

        // Freshly-created this turn — clear sickness on the token and the resolved Centaur so both
        // can attack.
        val centaur = driver.getPermanents(me).first { driver.getCardName(it) == "Centaur Courser" }
        listOf(tokenId, centaur).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(tokenId, centaur), opp)              // token power 1 alongside Centaur power 3
        driver.drainStack(me)

        // The token's own Training put a +1/+1 counter on it.
        driver.plusOneCounters(tokenId) shouldBe 1
    }
})

/**
 * Test-only creature carrying two instances of Training (CR 702.149b). Not registered in any set —
 * it exists purely to prove that two `training()` calls install two independent attack triggers
 * that each add a +1/+1 counter. Defined via the same `card { training() }` builder real cards use.
 */
private val TwinTrainer = card("Twin Trainer") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    training()
    training()
}
