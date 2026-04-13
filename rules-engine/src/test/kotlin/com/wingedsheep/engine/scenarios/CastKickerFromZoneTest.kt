package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that kicker/offspring variants are offered when casting from non-hand zones
 * (graveyard, exile) — regression test for the bug where CastFromZoneEnumerator
 * never generated kicked variants.
 */
class CastKickerFromZoneTest : FunSpec({

    val kickerCreatureFromGraveyard = card("Graveyard Kicker") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Elemental"
        power = 3
        toughness = 2

        keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{1}{R}")))

        staticAbility {
            ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD))
        }
    }

    test("casting kicked from graveyard applies WasKickedComponent") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(kickerCreatureFromGraveyard)

        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30)
        )

        val p1 = driver.player1
        val cardId = driver.putCardInGraveyard(p1, "Graveyard Kicker")

        // Put 5 mountains on battlefield (3 for base + 2 for kicker)
        repeat(5) { driver.putLandOnBattlefield(p1, "Mountain") }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast with kicker from graveyard
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

        // Verify kicked
        val creatures = driver.getCreatures(p1)
        creatures.size shouldBe 1
        driver.state.getEntity(creatures.first())?.has<WasKickedComponent>() shouldBe true
    }

    test("non-kicked cast from graveyard does not get WasKickedComponent") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(kickerCreatureFromGraveyard)

        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30)
        )

        val p1 = driver.player1
        val cardId = driver.putCardInGraveyard(p1, "Graveyard Kicker")

        // Put 3 mountains — enough for base cost only
        repeat(3) { driver.putLandOnBattlefield(p1, "Mountain") }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast without kicker from graveyard
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

        // Verify not kicked
        val creatures = driver.getCreatures(p1)
        creatures.size shouldBe 1
        driver.state.getEntity(creatures.first())?.has<WasKickedComponent>() shouldBe false
    }
})
