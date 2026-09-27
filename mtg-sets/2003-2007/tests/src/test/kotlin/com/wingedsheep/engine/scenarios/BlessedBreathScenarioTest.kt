package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Blessed Breath — {W} Instant — Arcane (Champions of Kamigawa).
 *
 * "Target creature you control gains protection from the color of your choice until end of turn.
 *  Splice onto Arcane {W}"
 */
class BlessedBreathScenarioTest : FunSpec({

    val arcaneSpark = card("Test Arcane Spark") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Instant — Arcane"
        spell {
            val t = target(Targets.Player)
            effect = Effects.DealDamage(1, t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(arcaneSpark)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.chooseColor(player: com.wingedsheep.sdk.model.EntityId, color: Color) {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(player, ColorChosenResponse(decision.id, color)).error shouldBe null
    }

    test("grants protection from the chosen colour until end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val breath = driver.putCardInHand(player, "Blessed Breath")
        driver.giveMana(player, Color.WHITE, 1)

        driver.submit(
            CastSpell(
                player, breath,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()
        driver.chooseColor(player, Color.RED)

        driver.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe true
        driver.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_BLACK") shouldBe false
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain breath

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe false
    }

    test("cannot target a creature an opponent controls") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val breath = driver.putCardInHand(player, "Blessed Breath")
        driver.giveMana(player, Color.WHITE, 1)

        driver.submit(
            CastSpell(
                player, breath,
                targets = listOf(ChosenTarget.Permanent(theirBears)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldNotBe null
    }

    test("spliced onto an Arcane spell it grants protection and stays in hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spark = driver.putCardInHand(player, "Test Arcane Spark")
        val breath = driver.putCardInHand(player, "Blessed Breath")
        // {W} for the Spark + {W} splice.
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(
            CastSpell(
                player, spark,
                targets = listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(bears)),
                splicedCardIds = listOf(breath),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()
        driver.chooseColor(player, Color.GREEN)

        driver.getLifeTotal(opponent) shouldBe 19
        driver.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_GREEN") shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain breath
    }
})
