package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that the WasKicked condition correctly checks a component on the permanent,
 * not just the EffectContext (which defaults to false for triggered abilities).
 */
class KickerConditionTest : FunSpec({

    // Inline card mimicking Skizzik: kicker, sacrifice at end step if not kicked
    val testKickerCreature = card("Test Kicker Creature") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Elemental"
        power = 5
        toughness = 3

        keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{R}")))
        keywords(Keyword.HASTE)

        triggeredAbility {
            trigger = Triggers.EachEndStep
            triggerCondition = NotCondition(WasKicked)
            effect = SacrificeSelfEffect
        }
    }

    test("kicked creature should NOT be sacrificed at end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(testKickerCreature)

        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Test Kicker Creature" to 10)
        )

        val p1 = driver.player1
        val cardId = driver.putCardInHand(p1, "Test Kicker Creature")

        // Put 5 mountains on battlefield (4 for spell + 1 for kicker)
        repeat(5) { driver.putLandOnBattlefield(p1, "Mountain") }

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast with kicker
        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = cardId,
                wasKicked = true,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        // Resolve the spell
        driver.passPriority(p1)
        driver.passPriority(driver.player2)

        // Verify the permanent has WasKickedComponent
        val creatures = driver.getCreatures(p1)
        creatures.size shouldBe 1
        val creatureId = creatures.first()
        driver.state.getEntity(creatureId)?.has<WasKickedComponent>() shouldBe true

        // Advance to end step - the trigger should NOT fire because it was kicked
        driver.passPriorityUntil(Step.END)

        // Creature should still be on the battlefield
        val creaturesAfter = driver.getCreatures(p1)
        creaturesAfter.size shouldBe 1
    }

    test("non-kicked creature should be sacrificed at end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(testKickerCreature)

        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Test Kicker Creature" to 10)
        )

        val p1 = driver.player1
        val cardId = driver.putCardInHand(p1, "Test Kicker Creature")

        // Put 4 mountains on battlefield (just enough for the base cost)
        repeat(4) { driver.putLandOnBattlefield(p1, "Mountain") }

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast without kicker
        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = cardId,
                wasKicked = false,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        // Resolve the spell
        driver.passPriority(p1)
        driver.passPriority(driver.player2)

        // Verify no WasKickedComponent
        val creatures = driver.getCreatures(p1)
        creatures.size shouldBe 1
        val creatureId = creatures.first()
        driver.state.getEntity(creatureId)?.has<WasKickedComponent>() shouldBe false

        // Advance to end step - the trigger SHOULD fire and sacrifice the creature
        driver.passPriorityUntil(Step.END)

        // Resolve the sacrifice trigger on the stack
        driver.bothPass()

        // Creature should be gone
        val creaturesAfter = driver.getCreatures(p1)
        creaturesAfter.size shouldBe 0
    }
})
