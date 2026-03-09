package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.TemurCharm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Temur Charm (KTK #208).
 *
 * Temur Charm {G}{U}{R}
 * Instant
 * Choose one —
 * • Target creature you control gets +1/+1 until end of turn. It fights target creature you don't control.
 * • Counter target spell unless its controller pays {3}.
 * • Creatures with power 3 or less can't block this turn.
 */
class TemurCharmTest : FunSpec({

    val SmallCreature = CardDefinition.creature(
        name = "Small Creature",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2
    )

    val BigCreature = CardDefinition.creature(
        name = "Big Creature",
        manaCost = ManaCost.parse("{4}"),
        subtypes = emptySet(),
        power = 5,
        toughness = 5
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TemurCharm, SmallCreature, BigCreature))
        return driver
    }

    test("mode 1 - fight: creature gets +1/+1 and fights opponent's creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCreature = driver.putCreatureOnBattlefield(activePlayer, "Small Creature") // 2/2
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Big Creature") // 5/5

        // Cast Temur Charm
        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveMana(activePlayer, Color.RED, 1)
        val charm = driver.putCardInHand(activePlayer, "Temur Charm")
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → modal choice
        driver.bothPass()

        val modeDecision = driver.pendingDecision
        modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 0))

        // Now we should get a ChooseTargetsDecision with TWO target requirements
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targetDecision.targetRequirements.size shouldBe 2

        // Requirement 0: creature you control
        val yourCreatureTargets = targetDecision.legalTargets[0]!!
        yourCreatureTargets.contains(myCreature) shouldBe true

        // Requirement 1: creature you don't control
        val theirCreatureTargets = targetDecision.legalTargets[1]!!
        theirCreatureTargets.contains(theirCreature) shouldBe true

        // Submit both targets
        driver.submitDecision(
            activePlayer,
            TargetsResponse(targetDecision.id, mapOf(0 to listOf(myCreature), 1 to listOf(theirCreature)))
        )

        // Small Creature (2/2 + 1/1 = 3/3) deals 3 damage to Big Creature (5/5) - survives
        val bigDamage = driver.state.getEntity(theirCreature)?.get<DamageComponent>()?.amount ?: 0
        bigDamage shouldBe 3

        // Big Creature (5/5) deals 5 damage to Small Creature (3/3) - dies
        driver.findPermanent(activePlayer, "Small Creature") shouldBe null
    }
})
