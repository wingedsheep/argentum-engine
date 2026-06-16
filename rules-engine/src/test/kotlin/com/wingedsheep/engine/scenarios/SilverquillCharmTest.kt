package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.SilverquillCharm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Silverquill Charm (SOS #225).
 *
 * Silverquill Charm {W}{B}
 * Instant
 * Choose one —
 * • Put two +1/+1 counters on target creature.
 * • Exile target creature with power 2 or less.
 * • Each opponent loses 3 life and you gain 3 life.
 */
class SilverquillCharmTest : FunSpec({

    val SmallCreature = CardDefinition.creature(
        name = "Silverquill Pupil",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1
    )

    val BigCreature = CardDefinition.creature(
        name = "Silverquill Brute",
        manaCost = ManaCost.parse("{3}"),
        subtypes = emptySet(),
        power = 4,
        toughness = 4
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillCharm, SmallCreature, BigCreature))
        return driver
    }

    fun castAndChooseMode(driver: GameTestDriver, caster: EntityId, modePrefix: String) {
        driver.giveMana(caster, Color.WHITE, 1)
        driver.giveMana(caster, Color.BLACK, 1)
        val charm = driver.putCardInHand(caster, "Silverquill Charm")
        driver.castSpell(caster, charm)
        val modeDecision = driver.pendingDecision
        modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val idx = modeDecision.options.indexOfFirst { it.startsWith(modePrefix) }
        require(idx >= 0) { "Mode '$modePrefix' not offered; options=${modeDecision.options}" }
        driver.submitDecision(caster, OptionChosenResponse(modeDecision.id, idx))
    }

    test("mode 1 - put two +1/+1 counters on target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pupil = driver.putCreatureOnBattlefield(me, "Silverquill Pupil") // 1/1

        castAndChooseMode(driver, me, "Put two +1/+1 counters")
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitDecision(me, TargetsResponse(targetDecision.id, mapOf(0 to listOf(pupil))))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(pupil) shouldBe 3
        projected.getToughness(pupil) shouldBe 3
    }

    test("mode 2 - exile target creature with power 2 or less; big creature is not a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pupil = driver.putCreatureOnBattlefield(opp, "Silverquill Pupil") // 1/1
        val brute = driver.putCreatureOnBattlefield(opp, "Silverquill Brute") // 4/4

        castAndChooseMode(driver, me, "Exile target creature")
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = targetDecision.legalTargets[0]!!
        legal.contains(pupil) shouldBe true
        legal.contains(brute) shouldBe false
        driver.submitDecision(me, TargetsResponse(targetDecision.id, mapOf(0 to listOf(pupil))))
        driver.bothPass()

        driver.findPermanent(opp, "Silverquill Pupil") shouldBe null
    }

    test("mode 3 - each opponent loses 3 life and you gain 3 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        castAndChooseMode(driver, me, "Each opponent loses")
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 17
        driver.getLifeTotal(me) shouldBe 23
    }
})
