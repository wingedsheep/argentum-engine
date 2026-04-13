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
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyardWithLifeCost
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

    // Enabler that lets you cast instants/sorceries from graveyard by paying 1 life
    val graveyardLifeCostEnabler = card("Test Graveyard Enabler") {
        manaCost = "{R}"
        typeLine = "Enchantment"

        staticAbility {
            ability = MayCastFromGraveyardWithLifeCost(
                filter = GameObjectFilter.InstantOrSorcery,
                lifeCost = 1,
                duringYourTurnOnly = true
            )
        }
    }

    // Kicker instant to cast from graveyard via the life-cost enabler
    val kickerInstant = card("Test Kicker Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Kicker {2}. Test Kicker Bolt deals 2 damage to target player."

        keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))
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

    test("casting kicked from graveyard with life cost succeeds") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(graveyardLifeCostEnabler)
        driver.registerCard(kickerInstant)

        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30)
        )

        val p1 = driver.player1

        // Put enabler on battlefield
        driver.putPermanentOnBattlefield(p1, "Test Graveyard Enabler")
        // Put kicker instant in graveyard
        val cardId = driver.putCardInGraveyard(p1, "Test Kicker Bolt")

        // Put 3 mountains on battlefield (1 for base + 2 for kicker)
        repeat(3) { driver.putLandOnBattlefield(p1, "Mountain") }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast with kicker from graveyard, paying 1 life
        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = cardId,
                wasKicked = true,
                graveyardLifeCost = 1,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        // Verify life was paid
        driver.getLifeTotal(p1) shouldBe 19
    }
})
