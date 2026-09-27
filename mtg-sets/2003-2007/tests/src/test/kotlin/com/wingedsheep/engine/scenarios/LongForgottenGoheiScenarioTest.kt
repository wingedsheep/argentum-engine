package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.GlacialRay
import com.wingedsheep.mtg.sets.definitions.chk.cards.HanaKami
import com.wingedsheep.mtg.sets.definitions.chk.cards.LongForgottenGohei
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Long-Forgotten Gohei — {3} Artifact.
 * Arcane spells you cast cost {1} less to cast.
 * Spirit creatures you control get +1/+1.
 */
class LongForgottenGoheiScenarioTest : FunSpec({

    val projector = StateProjector()
    val cards = listOf(LongForgottenGohei, HanaKami, GlacialRay)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        cards.forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 10, "Forest" to 10), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun calculator(): Pair<CardRegistry, CostCalculator> {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        cards.forEach { registry.register(it) }
        return registry to CostCalculator(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
    }

    test("Spirit creatures you control get +1/+1; non-Spirits and opponents' Spirits do not") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Long-Forgotten Gohei")
        val mySpirit = driver.putCreatureOnBattlefield(me, "Hana Kami")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirSpirit = driver.putCreatureOnBattlefield(driver.player2, "Hana Kami")

        val projected = projector.project(driver.state)
        projected.getPower(mySpirit) shouldBe 2
        projected.getToughness(mySpirit) shouldBe 2
        projected.getPower(bears) shouldBe 2
        projected.getToughness(bears) shouldBe 2
        projected.getPower(theirSpirit) shouldBe 1
        projected.getToughness(theirSpirit) shouldBe 1
    }

    test("Arcane spells you cast cost {1} less; non-Arcane spells and opponents are unaffected") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Long-Forgotten Gohei")
        val (registry, calc) = calculator()

        // Glacial Ray {1}{R} Instant — Arcane -> {R}
        val ray = calc.calculateEffectiveCost(driver.state, registry.requireCard("Glacial Ray"), me)
        ray.genericAmount shouldBe 0
        ray.cmc shouldBe 1

        // Grizzly Bears {1}{G} is not Arcane -> unchanged
        val bears = calc.calculateEffectiveCost(driver.state, registry.requireCard("Grizzly Bears"), me)
        bears.genericAmount shouldBe 1

        // The opponent's Arcane spells are not discounted
        val theirRay = calc.calculateEffectiveCost(driver.state, registry.requireCard("Glacial Ray"), driver.player2)
        theirRay.genericAmount shouldBe 1
    }
})
