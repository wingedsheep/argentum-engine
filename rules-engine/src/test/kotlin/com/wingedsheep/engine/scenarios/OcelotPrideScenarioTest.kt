package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.citysblessing.CitysBlessingService
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ocelot Pride (MH3) — three chained pieces, tested apart from each other.
 *
 * 1. **Ascend is continuous** (CR 702.131b). It is the keyword and nothing else; the engine's
 *    702.131b state-based action grants the city's blessing whenever the tenth permanent shows up,
 *    not only on the turn Ocelot Pride entered. For a one-mana 1/1 that distinction is the whole
 *    card: you essentially never control ten permanents on turn one, so an ETB-trigger reading of
 *    ascend would leave the second half of the last ability permanently dead.
 * 2. **The end-step ability is a genuine intervening-if** (CR 603.4) — it doesn't trigger unless you
 *    gained life before the end step began.
 * 3. **"Then if you have the city's blessing…" is resolution-time only**, and per the 2024-06-07
 *    ruling the Cat created earlier in the *same* resolution counts, both toward the ten-permanent
 *    ascend threshold and as a token that entered this turn.
 */
class OcelotPrideScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    /**
     * Does [playerId] have the city's blessing, as every consumer of it sees the question — the
     * `YouHaveCitysBlessing` condition, the static-ability projection and the client badge all read
     * through [CitysBlessingService]. Not a bare component read: the marker is written by a
     * state-based action, and CR 702.131b's ascend is continuous, so the designation is yours
     * before the next poll.
     */
    fun hasCitysBlessing(d: GameTestDriver, playerId: EntityId): Boolean =
        CitysBlessingService.has(d.state, playerId)

    /** The persisted marker specifically — what survives dropping back below ten permanents. */
    fun blessingMarkerWritten(d: GameTestDriver, playerId: EntityId): Boolean =
        d.state.getEntity(playerId)?.has<PlayerCitysBlessingComponent>() == true

    fun catTokens(d: GameTestDriver, playerId: EntityId): Int =
        d.getCreatures(playerId).count { d.getCardName(it) == "Cat Token" }

    /** "if you gained life this turn" reads LifeGainedAmountThisTurnComponent — seed it directly. */
    fun seedLifeGained(d: GameTestDriver, playerId: EntityId, amount: Int) {
        d.replaceState(
            d.state.updateEntity(playerId) { it.with(LifeGainedAmountThisTurnComponent(amount)) }
        )
    }

    fun grantCitysBlessing(d: GameTestDriver, playerId: EntityId) {
        d.replaceState(d.state.updateEntity(playerId) { it.with(PlayerCitysBlessingComponent) })
    }

    /**
     * Drain the stack and any pending decisions, then stop. Deliberately does *not* pass on an
     * empty stack — an unconditional `bothPass()` loop walks the turn forward and skips the very
     * end step these tests are trying to observe.
     */
    fun settle(d: GameTestDriver, maxSteps: Int = 20) {
        repeat(maxSteps) {
            when {
                d.pendingDecision != null -> d.autoResolveDecision()
                d.getTopOfStack() != null -> d.bothPass()
                else -> return
            }
        }
    }

    /**
     * Advance to the next turn's draw step, which is one of the points the engine polls state-based
     * actions (the others being after a resolution, after combat damage and on the end-the-turn
     * path — notably *not* a bare priority pass).
     *
     * Only the tests that assert on the persisted marker need this. The battlefield helpers write
     * straight into the state instead of going through the play-land and cast pipelines, so nothing
     * has polled since the board was assembled; every *rules* read of the blessing goes through
     * [CitysBlessingService] and doesn't wait for a poll.
     */
    fun pollStateBasedActions(d: GameTestDriver) {
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.DRAW)
        settle(d)
    }

    test("crossing ten permanents after Ocelot Pride entered still grants the city's blessing (CR 702.131b)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast it for real so that, on a board of two permanents, ascend genuinely finds fewer than
        // ten — exactly as a turn-one Ocelot Pride would.
        d.giveMana(active, Color.WHITE, 1)
        val prideCard = d.putCardInHand(active, "Ocelot Pride")
        d.castSpell(active, prideCard)
        settle(d)

        d.findPermanent(active, "Ocelot Pride") shouldNotBe null
        hasCitysBlessing(d, active) shouldBe false

        // Now climb well past ten permanents, as any real game does a few turns later.
        repeat(12) { d.putLandOnBattlefield(active, "Plains") }
        d.getPermanents(active).size shouldBeGreaterThanOrEqual 10

        // Ascend is static and continuous: once the tenth permanent is there the blessing is yours,
        // with no second enters-the-battlefield event to wait for.
        hasCitysBlessing(d, active) shouldBe true
    }

    test("the city's blessing survives dropping back below ten permanents (CR 702.131b/c)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pride = d.putCreatureOnBattlefield(active, "Ocelot Pride")
        val lands = (1..12).map { d.putLandOnBattlefield(active, "Plains") }
        pollStateBasedActions(d)
        blessingMarkerWritten(d, active) shouldBe true

        // Wrath the board back down under the threshold — including the ascend permanent itself.
        d.moveToGraveyard(pride)
        lands.forEach { d.moveToGraveyard(it) }
        pollStateBasedActions(d)
        d.getPermanents(active).size shouldBe 0

        // "For the rest of the game": the designation is a one-way marker, not a continuous effect
        // that switches off with the board that produced it.
        blessingMarkerWritten(d, active) shouldBe true
        hasCitysBlessing(d, active) shouldBe true
    }

    test("with ten-plus permanents reached later, the end-step ability still doubles the Cat") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(active, Color.WHITE, 1)
        val prideCard = d.putCardInHand(active, "Ocelot Pride")
        d.castSpell(active, prideCard)
        settle(d)

        repeat(12) { d.putLandOnBattlefield(active, "Plains") }
        seedLifeGained(d, active, 1)
        catTokens(d, active) shouldBe 0

        d.passPriorityUntil(Step.END)
        settle(d)

        // One Cat from the first clause, plus one copy of it from the city's-blessing clause
        // (per the 2024-06-07 ruling, the Cat made earlier in this same resolution has already
        // "entered this turn" by the time the second clause is reached).
        catTokens(d, active) shouldBe 2
    }

    test("the Cat that is your tenth permanent grants the blessing before the same resolution checks for it") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Nine permanents: Ocelot Pride plus eight lands. The Cat the trigger is about to make is
        // the tenth.
        d.putCreatureOnBattlefield(active, "Ocelot Pride")
        repeat(8) { d.putLandOnBattlefield(active, "Plains") }
        settle(d)
        d.getPermanents(active).size shouldBe 9
        hasCitysBlessing(d, active) shouldBe false

        seedLifeGained(d, active, 1)
        d.passPriorityUntil(Step.END)
        settle(d)

        // 2024-06-07 ruling: "If the creature token created by Ocelot Pride's last ability is your
        // tenth permanent, you'll get the city's blessing before the ability would check to see if
        // you have the city's blessing." State-based actions aren't polled mid-resolution, so this
        // only comes out right because ascend is read live rather than waiting for the marker.
        hasCitysBlessing(d, active) shouldBe true
        catTokens(d, active) shouldBe 2
    }

    test("one permanent short, the Cat is not doubled") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Eight permanents; the Cat makes nine, one short of ascend's threshold.
        d.putCreatureOnBattlefield(active, "Ocelot Pride")
        repeat(7) { d.putLandOnBattlefield(active, "Plains") }
        settle(d)

        seedLifeGained(d, active, 1)
        d.passPriorityUntil(Step.END)
        settle(d)

        hasCitysBlessing(d, active) shouldBe false
        catTokens(d, active) shouldBe 1
    }

    test("without life gained this turn the ability doesn't trigger at all (CR 603.4)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Ocelot Pride")
        repeat(12) { d.putLandOnBattlefield(active, "Plains") }
        hasCitysBlessing(d, active) shouldBe true

        d.passPriorityUntil(Step.END)
        settle(d)

        catTokens(d, active) shouldBe 0
    }

    test("control: granted the city's blessing directly, the doubling works without ascend") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Ocelot Pride")
        grantCitysBlessing(d, active)
        seedLifeGained(d, active, 1)
        catTokens(d, active) shouldBe 0

        d.passPriorityUntil(Step.END)
        settle(d)

        catTokens(d, active) shouldBe 2
    }
})
