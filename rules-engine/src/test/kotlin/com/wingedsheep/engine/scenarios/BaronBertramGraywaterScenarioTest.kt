package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BaronBertramGraywater
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Baron Bertram Graywater — {2}{W}{B} 3/4 Legendary Creature — Vampire Noble
 *
 * 1. "Whenever one or more tokens you control enter, create a 1/1 black Vampire Rogue creature
 *     token with lifelink. This ability triggers only once each turn."
 * 2. "{1}{B}, Sacrifice another creature or artifact: Draw a card."
 */
class BaronBertramGraywaterScenarioTest : FunSpec({

    val drawAbilityId = BaronBertramGraywater.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BaronBertramGraywater))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun vampireTokens(driver: GameTestDriver, playerId: EntityId): List<EntityId> =
        driver.getCreatures(playerId).filter {
            val e = driver.state.getEntity(it)
            e?.get<CardComponent>()?.name == "Vampire Rogue Token" && e.has<TokenComponent>()
        }

    fun mercenaryTokens(driver: GameTestDriver, playerId: EntityId): List<EntityId> =
        driver.getCreatures(playerId).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Mercenary Token"
        }

    fun castFormAPosse(driver: GameTestDriver, playerId: EntityId, x: Int) {
        driver.giveMana(playerId, Color.RED, 1)
        driver.giveMana(playerId, Color.WHITE, 1)
        driver.giveColorlessMana(playerId, x)
        val posse = driver.putCardInHand(playerId, "Form a Posse")
        driver.castXSpell(playerId, posse, xValue = x).error shouldBe null
        // Resolve Form a Posse and the Baron trigger(s) it created, but stop once the stack is empty
        // so we stay in the precombat main phase (Form a Posse is a sorcery; a later cast needs it).
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 20) {
            driver.bothPass()
        }
    }

    test("one or more tokens entering creates a single Vampire Rogue — once per turn") {
        val driver = createDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(me, "Baron Bertram Graywater")

        // Make two Mercenary tokens — the trigger fires once for the batch, making ONE Vampire.
        castFormAPosse(driver, me, x = 2)
        vampireTokens(driver, me).size shouldBe 1

        // A second batch the same turn does not make another Vampire (once per turn).
        castFormAPosse(driver, me, x = 1)
        vampireTokens(driver, me).size shouldBe 1
    }

    test("activated ability: sacrifice another creature to draw a card") {
        val driver = createDriver()
        val me = driver.player1
        val baron = driver.putCreatureOnBattlefield(me, "Baron Bertram Graywater")
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        val handBefore = driver.getHandSize(me)

        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = baron,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve the draw

        driver.findPermanent(me, "Grizzly Bears") shouldBe null
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("activated ability cannot sacrifice Baron himself (\"another\")") {
        val driver = createDriver()
        val me = driver.player1
        val baron = driver.putCreatureOnBattlefield(me, "Baron Bertram Graywater")

        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = baron,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(baron)),
            )
        )
        result.isSuccess shouldBe false
    }
})
