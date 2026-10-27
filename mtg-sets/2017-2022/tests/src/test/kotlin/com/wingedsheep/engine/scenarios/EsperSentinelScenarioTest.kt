package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.EsperSentinel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Esper Sentinel — "Whenever an opponent casts their first noncreature spell each turn, draw a card
 * unless that player pays {X}, where X is this creature's power."
 *
 * Pins the parts a reader can't confirm from the script: the caster is the one asked and charged,
 * the draw is the *unpaid* branch, only the first noncreature spell counts (a creature spell neither
 * triggers nor closes the window), and X is the Sentinel's last-known power when it has died.
 */
class EsperSentinelScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EsperSentinel))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.redFloating(playerId: EntityId): Int =
        (state.getEntity(playerId)!!.get<ManaPoolComponent>() ?: ManaPoolComponent()).red

    /** The caster bolts the Sentinel's controller with [red] red mana floating; resolves up to the pay prompt. */
    fun GameTestDriver.castBoltIntoSentinel(caster: EntityId, sentinelOwner: EntityId, red: Int) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, red)
        castSpell(caster, bolt, targets = listOf(sentinelOwner)).error shouldBe null
        stackSize shouldBe 2
        bothPass()
    }

    test("the caster declines to pay - the Sentinel's controller draws") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putCreatureOnBattlefield(owner, "Esper Sentinel")
        val handBefore = d.getHandSize(owner)

        d.castBoltIntoSentinel(caster, owner, red = 2)
        val prompt = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        prompt.playerId shouldBe caster
        d.submitYesNo(caster, false)

        d.getHandSize(owner) shouldBe handBefore + 1
        d.redFloating(caster) shouldBe 1
    }

    test("the caster pays {X} = the Sentinel's power - no draw") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putCreatureOnBattlefield(owner, "Esper Sentinel")
        val handBefore = d.getHandSize(owner)

        d.castBoltIntoSentinel(caster, owner, red = 2)
        d.submitYesNo(caster, true)

        d.getHandSize(owner) shouldBe handBefore
        d.redFloating(caster) shouldBe 0
    }

    test("a caster who can't pay can only refuse - the draw happens") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putCreatureOnBattlefield(owner, "Esper Sentinel")
        val handBefore = d.getHandSize(owner)

        d.castBoltIntoSentinel(caster, owner, red = 1)

        // The caster's hand could hold a Spirit Guide, so they are asked — refusal only.
        d.pendingDecision!!.playerId shouldBe caster
        d.answerForcedPayments()
        d.pendingDecision shouldBe null
        d.getHandSize(owner) shouldBe handBefore + 1
    }

    test("only the first noncreature spell each turn triggers it") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putCreatureOnBattlefield(owner, "Esper Sentinel")

        d.castBoltIntoSentinel(caster, owner, red = 1)
        d.answerForcedPayments()
        while (d.stackSize > 0) d.bothPass()

        val second = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, second, targets = listOf(owner)).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("a creature spell neither triggers it nor uses up the first noncreature spell") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putCreatureOnBattlefield(owner, "Esper Sentinel")

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
        val handBefore = d.getHandSize(owner)

        // The Bolt is still their first noncreature spell: castBoltIntoSentinel sees the trigger.
        d.castBoltIntoSentinel(caster, owner, red = 1)
        d.answerForcedPayments()
        d.getHandSize(owner) shouldBe handBefore + 1
    }

    test("its own controller's noncreature spells don't trigger it") {
        val d = createDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Esper Sentinel")
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, targets = listOf(opp)).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("X is the Sentinel's last-known power if it died before the trigger resolved") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        val sentinel = d.putCreatureOnBattlefield(owner, "Esper Sentinel")

        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        val kill = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 3)
        d.castSpell(caster, bolt, targets = listOf(owner)).error shouldBe null
        d.stackSize shouldBe 2
        // The second Bolt is not their first noncreature spell, so it adds no trigger.
        d.castSpell(caster, kill, targets = listOf(sentinel)).error shouldBe null
        d.stackSize shouldBe 3
        d.bothPass()
        d.getGraveyard(owner) shouldContain sentinel

        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe caster
        d.submitYesNo(caster, true)
        // The last-known power was 1, so paying cost exactly the one red left floating.
        d.redFloating(caster) shouldBe 0
    }
})
