package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.LoreholdCharm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Lorehold Charm (SOS #200).
 *
 * Lorehold Charm {R}{W}
 * Instant
 * Choose one —
 * • Each opponent sacrifices a nontoken artifact of their choice.
 * • Return target artifact or creature card with mana value 2 or less from your graveyard to the battlefield.
 * • Creatures you control get +1/+1 and gain trample until end of turn.
 */
class LoreholdCharmTest : FunSpec({

    val SmallArtifact = CardDefinition.artifact(
        name = "Small Artifact",
        manaCost = ManaCost.parse("{1}")
    )

    val SmallCreature = CardDefinition.creature(
        name = "Lorehold Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LoreholdCharm, SmallArtifact, SmallCreature))
        return driver
    }

    // Select a mode by its text. The engine prunes modes with no legal target, so we
    // can't rely on a fixed index — match the option string instead.
    fun castAndChooseMode(driver: GameTestDriver, caster: com.wingedsheep.sdk.model.EntityId, modePrefix: String) {
        driver.giveMana(caster, Color.RED, 1)
        driver.giveMana(caster, Color.WHITE, 1)
        val charm = driver.putCardInHand(caster, "Lorehold Charm")
        driver.castSpell(caster, charm)
        val modeDecision = driver.pendingDecision
        modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val idx = modeDecision.options.indexOfFirst { it.startsWith(modePrefix) }
        require(idx >= 0) { "Mode '$modePrefix' not offered; options=${modeDecision.options}" }
        driver.submitDecision(caster, OptionChosenResponse(modeDecision.id, idx))
    }

    test("mode 1 - each opponent sacrifices a nontoken artifact of their choice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Plains" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(opp, "Small Artifact")

        castAndChooseMode(driver, me, "Each opponent sacrifices")
        driver.bothPass()

        // The opponent's only nontoken artifact is auto-sacrificed.
        driver.findPermanent(opp, "Small Artifact") shouldBe null
    }

    test("mode 2 - return a mana-value-2-or-less artifact or creature from graveyard to battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Plains" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val deadBear = driver.putCardInGraveyard(me, "Lorehold Bear")

        castAndChooseMode(driver, me, "Return target artifact")
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targetDecision.legalTargets[0]!!.contains(deadBear) shouldBe true
        driver.submitDecision(me, TargetsResponse(targetDecision.id, mapOf(0 to listOf(deadBear))))
        driver.bothPass()

        // The bear is back on the battlefield under our control.
        driver.findPermanent(me, "Lorehold Bear") shouldBe deadBear
    }

    test("mode 3 - creatures you control get +1/+1 and gain trample until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Plains" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(me, "Lorehold Bear") // 2/2

        castAndChooseMode(driver, me, "Creatures you control")
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 3
        projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
    }
})
