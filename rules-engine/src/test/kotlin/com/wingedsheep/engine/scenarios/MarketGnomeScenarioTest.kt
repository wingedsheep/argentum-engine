package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.mtg.sets.definitions.lci.cards.MarketGnome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Market Gnome (LCI #22) — the two "gain 1 life and draw a card" triggers.
 *
 *  1. "When this creature dies, you gain 1 life and draw a card." — plain [Triggers.Dies].
 *  2. "When this creature is exiled from the battlefield while you're activating a craft
 *     ability, you gain 1 life and draw a card." — the new [Triggers.ExiledAsCraftMaterial]
 *     SELF exile trigger, gated on the craft-material fact stamped by the Craft cost payment
 *     (CR 702.167).
 *
 * Also pins the edge cases the two abilities must keep separate:
 *  - Craft exile fires ability 2, NOT ability 1 (exile is not death).
 *  - A non-craft exile fires neither ability ("only while you're activating a craft ability").
 */
class MarketGnomeScenarioTest : FunSpec({

    // A test-local crafter: "Craft with a creature {1}" so Market Gnome can be the exiled material.
    val crafterFront = card("Test Gnome Crafter") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Craft with a creature {1}"
        craft(filter = GameObjectFilter.Creature, cost = "{1}", materialDescription = "a creature", minCount = 1, maxCount = 1)
    }
    val crafterBack = card("Test Crafted Idol") {
        manaCost = ""
        typeLine = "Artifact Creature — Golem"
        power = 3
        toughness = 3
        oracleText = ""
    }
    val crafter: CardDefinition = CardDefinition.doubleFacedPermanent(
        frontFace = crafterFront,
        backFace = crafterBack
    )

    // A plain non-craft exile instant, to prove ability 2 does NOT fire on removal-style exile.
    val banish = card("Test Banish") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "Exile target creature."
        spell {
            val t = target("target creature", TargetCreature())
            effect = Effects.Exile(t)
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarketGnome, crafter, banish))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        return driver
    }

    fun craftAbilityId() = crafter.activatedAbilities.single().id

    test("dies: gain 1 life and draw a card") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val gnome = driver.putPermanentOnBattlefield(p1, "Market Gnome")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lifeBefore = driver.getLifeTotal(p1)
        val handBefore = driver.getHandSize(p1)

        // Lightning Bolt for 3 kills the 0/3 gnome for real, so its dies trigger fires.
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 3)
        driver.castSpell(p1, bolt, targets = listOf(gnome)).isSuccess shouldBe true
        driver.bothPass() // resolve Bolt -> gnome dies, queue its dies trigger
        driver.bothPass() // resolve the dies trigger

        driver.state.getBattlefield().contains(gnome) shouldBe false
        driver.getLifeTotal(p1) shouldBe lifeBefore + 1
        driver.getHandSize(p1) shouldBe handBefore + 1
    }

    test("exiled as a craft material: gain 1 life and draw a card, and the dies trigger does not fire") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val crafterId = driver.putPermanentOnBattlefield(p1, "Test Gnome Crafter")
        val gnome = driver.putPermanentOnBattlefield(p1, "Market Gnome")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 1)

        val lifeBefore = driver.getLifeTotal(p1)
        val handBefore = driver.getHandSize(p1)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = crafterId,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(gnome))
            )
        )
        driver.bothPass() // resolve the craft (transform crafter) + queue the exile trigger
        driver.bothPass() // resolve Market Gnome's craft-exile trigger

        // Exiled, not in graveyard: the exile trigger fired, the dies trigger did not (each gives
        // exactly one life + one card, so a doubled amount would betray a double-fire).
        driver.state.getZone(ZoneKey(p1, Zone.EXILE)).contains(gnome) shouldBe true
        driver.getLifeTotal(p1) shouldBe lifeBefore + 1
        driver.getHandSize(p1) shouldBe handBefore + 1
    }

    test("exiled by a non-craft effect: neither ability fires") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val gnome = driver.putPermanentOnBattlefield(p1, "Market Gnome")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lifeBefore = driver.getLifeTotal(p1)
        val handBefore = driver.getHandSize(p1)

        val banishId = driver.putCardInHand(p1, "Test Banish")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, banishId, targets = listOf(gnome)).isSuccess shouldBe true
        driver.bothPass() // resolve Banish -> gnome exiled (non-craft)
        driver.bothPass()

        driver.state.getZone(ZoneKey(p1, Zone.EXILE)).contains(gnome) shouldBe true
        // "only while you're activating a craft ability" — a plain exile is not death and not a
        // craft material, so no life gained and no card drawn.
        driver.getLifeTotal(p1) shouldBe lifeBefore
        driver.getHandSize(p1) shouldBe handBefore
    }
})
