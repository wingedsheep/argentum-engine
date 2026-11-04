package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rix.cards.InducedAmnesia
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Induced Amnesia (RIX #40) — {2}{U} Enchantment.
 *
 * "When this enchantment enters, target player exiles all cards from their hand face down, then
 *  draws that many cards.
 *  When this enchantment is put into a graveyard from the battlefield, return the exiled cards to
 *  their owner's hand."
 *
 * Proves the opponent's hand is exiled face down and replaced card for card, that destroying the
 * enchantment returns those cards to the *opponent's* hand (their owner, not the enchantment's
 * controller), and that bouncing it instead leaves them exiled (ruling).
 */
class InducedAmnesiaScenarioTest : FunSpec({

    val shatter = card("Amnesia Test Shatter") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.Destroy(t) }
    }
    val unsummon = card("Amnesia Test Unsummon") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.ReturnToHand(t) }
    }

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(InducedAmnesia, shatter, unsummon))
        initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle(maxSteps: Int = 40) {
        repeat(maxSteps) {
            when {
                pendingDecision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    /** Casts Induced Amnesia for [me] targeting [victim]; returns (enchantment, victim's old hand). */
    fun GameTestDriver.castAmnesia(me: EntityId, victim: EntityId): Pair<EntityId, List<EntityId>> {
        val oldHand = getHand(victim)
        val amnesia = putCardInHand(me, InducedAmnesia.name)
        giveMana(me, Color.BLUE, 1)
        giveColorlessMana(me, 2)
        castSpell(me, amnesia).error shouldBe null
        bothPass()
        pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        submitTargetSelection(me, listOf(victim)).error shouldBe null
        settle()
        return amnesia to oldHand
    }

    test("target player exiles their hand face down and draws that many") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val (_, oldHand) = driver.castAmnesia(me, opponent)

        (oldHand.isNotEmpty()).shouldBeTrue()
        driver.getExile(opponent) shouldContainAll oldHand
        oldHand.forEach { driver.state.getEntity(it)!!.has<FaceDownComponent>().shouldBeTrue() }
        driver.getHandSize(opponent) shouldBe oldHand.size
        driver.getHand(opponent) shouldNotContainAnyOf oldHand
    }

    test("when it is destroyed, the exiled cards return to their owner's hand") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val myHandBefore = driver.getHand(me)
        val (amnesia, oldHand) = driver.castAmnesia(me, opponent)
        val drawn = driver.getHand(opponent)

        driver.castSpell(me, driver.putCardInHand(me, shatter.name), listOf(amnesia)).error shouldBe null
        driver.settle()

        driver.state.getZone(me, Zone.GRAVEYARD).contains(amnesia).shouldBeTrue()
        driver.getHand(opponent) shouldContainExactlyInAnyOrder (drawn + oldHand)
        driver.getHand(me) shouldContainExactlyInAnyOrder myHandBefore
    }

    test("when it leaves without going to a graveyard, the exiled cards stay exiled") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val (amnesia, oldHand) = driver.castAmnesia(me, opponent)

        driver.castSpell(me, driver.putCardInHand(me, unsummon.name), listOf(amnesia)).error shouldBe null
        driver.settle()

        driver.getHand(me).contains(amnesia).shouldBeTrue()
        driver.getExile(opponent) shouldContainAll oldHand
        driver.getHand(opponent) shouldNotContainAnyOf oldHand
    }
})
