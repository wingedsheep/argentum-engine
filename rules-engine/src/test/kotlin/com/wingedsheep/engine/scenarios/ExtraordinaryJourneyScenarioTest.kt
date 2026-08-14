package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.ExtraordinaryJourney
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Extraordinary Journey {X}{X}{U}{U} — Enchantment.
 * "When this enchantment enters, exile up to X target creatures. For each of those cards, its owner
 *  may play it for as long as it remains exiled."
 * "Whenever one or more nontoken creatures enter, if one or more of them entered from exile or was
 *  cast from exile, you draw a card. This ability triggers only once each turn."
 *
 * Covers the X-clamped ETB (via `DynamicAmount.CastX`, since the spell is gone by the time the
 * trigger resolves), the owner-scoped exile permission, and the new
 * `Conditions.AnyEnteredOrWasCastFromExile` intervening-"if" — including the interaction that
 * motivated modelling it as a *trigger* condition rather than a resolution gate: a batch with no
 * exile arrivals must not consume the `oncePerTurn` firing.
 */
class ExtraordinaryJourneyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ExtraordinaryJourney))
        return driver
    }

    fun mayPlay(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean {
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        return enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            .any { it.actionType == "CastSpell" && (it.action as? CastSpell)?.cardId == cardId }
    }

    fun handSize(driver: GameTestDriver, player: EntityId): Int =
        driver.state.getZone(ZoneKey(player, Zone.HAND)).size

    /**
     * Pass priority until the stack is empty. `bothPass()` resolves a single object, and a creature
     * spell resolving here puts the draw trigger on the stack behind it, so one pass is never
     * enough to see the payoff.
     */
    fun drainStack(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) {
            driver.bothPass()
        }
    }

    /** Cast [cardId] with auto-pay and let the whole stack (spell + any triggers) resolve. */
    fun castAndResolve(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        driver.submit(
            CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.AutoPay)
        ).isSuccess shouldBe true
        drainStack(driver)
    }

    /**
     * Cast Extraordinary Journey for [x], then answer its enters-trigger by exiling [targets].
     * The enchantment spell itself has no targets — the "up to X target creatures" belong to the
     * ETB trigger, chosen as it goes on the stack (CR 603.3d), so they arrive as a decision.
     */
    fun castJourney(
        driver: GameTestDriver,
        caster: EntityId,
        x: Int,
        targets: List<EntityId>
    ) {
        val spell = driver.putCardInHand(caster, "Extraordinary Journey")
        repeat(2 * x + 2) { driver.putLandOnBattlefield(caster, "Island") }
        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                paymentStrategy = PaymentStrategy.AutoPay,
                xValue = x
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        if (driver.pendingDecision != null) {
            driver.submitTargetSelection(caster, targets).isSuccess shouldBe true
        }
        drainStack(driver)
    }

    test("the ETB exiles up to X creatures and their owners may play them from exile") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        castJourney(driver, me, x = 2, targets = listOf(mine, theirs))

        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain mine
        driver.state.getZone(ZoneKey(opp, Zone.EXILE)) shouldContain theirs

        // "its owner may play it" — each owner, not the caster.
        mayPlay(driver, me, mine) shouldBe true
        mayPlay(driver, opp, theirs) shouldBe true
        mayPlay(driver, me, theirs) shouldBe false
    }

    test("X = 0 resolves cleanly with nothing exiled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castJourney(driver, me, x = 0, targets = emptyList())

        driver.state.getZone(ZoneKey(me, Zone.EXILE)).contains(bears) shouldBe false
        driver.state.getBattlefield() shouldContain bears
    }

    test("recasting an exiled creature from exile draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castJourney(driver, me, x = 1, targets = listOf(bears))
        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain bears

        // Grizzly Bears is {1}{G}; the Journey's permission only waives the zone, not the cost.
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        val before = handSize(driver, me)

        castAndResolve(driver, me, bears)

        driver.state.getBattlefield() shouldContain bears
        handSize(driver, me) shouldBe before + 1
    }

    test("a creature entering from hand neither draws nor spends the turn's single firing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castJourney(driver, me, x = 1, targets = listOf(bears))

        // A plain hand-cast creature: the intervening-"if" is false, so the ability never triggers
        // (CR 603.4) and must not stamp the once-per-turn tracker.
        val fromHand = driver.putCardInHand(me, "Grizzly Bears")
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        var before = handSize(driver, me)
        castAndResolve(driver, me, fromHand)
        handSize(driver, me) shouldBe before - 1 // the card left hand; no draw

        // The exile arrival later this turn still draws — the "once" was never consumed.
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        before = handSize(driver, me)
        castAndResolve(driver, me, bears)
        handSize(driver, me) shouldBe before + 1
    }

    test("only once each turn — a second exile arrival in the same turn draws nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val first = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castJourney(driver, me, x = 2, targets = listOf(first, second))

        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        var before = handSize(driver, me)
        castAndResolve(driver, me, first)
        handSize(driver, me) shouldBe before + 1

        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        before = handSize(driver, me)
        castAndResolve(driver, me, second)
        handSize(driver, me) shouldBe before // capped at one draw per turn
    }
})
