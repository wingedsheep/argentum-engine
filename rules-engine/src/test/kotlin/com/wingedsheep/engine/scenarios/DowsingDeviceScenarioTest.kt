package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.DowsingDevice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dowsing Device // Geode Grotto (LCI #146).
 *
 * Front — Dowsing Device: "Whenever this artifact or another artifact you control enters, up to
 * one target creature you control gets +1/+0 and gains haste until end of turn. Then transform
 * this artifact if you control four or more artifacts."
 *
 * Pins the self-or-other artifact ETB pump (+1/+0 and haste to the chosen creature) and the
 * transform gated on controlling four or more artifacts.
 */
class DowsingDeviceScenarioTest : FunSpec({

    val projector = StateProjector()

    // Local vanilla helpers with unambiguous stats: a 2/2 pump target (so +1/+0 reads as 3/2)
    // and a plain artifact to pad the artifact count / trigger the ETB watcher.
    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = "",
    )
    val testRelic = CardDefinition.artifact(
        name = "Test Relic",
        manaCost = ManaCost.parse("{1}"),
        oracleText = "",
    )

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DowsingDevice, bear, testRelic))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cardName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    // Cast Dowsing Device from hand and resolve its self-enter ETB, targeting [creature] with the
    // "up to one target" pump. Returns the Dowsing entity.
    fun castDowsing(driver: GameTestDriver, p1: EntityId, creature: EntityId): EntityId {
        val dowsing = driver.putCardInHand(p1, "Dowsing Device")
        driver.giveMana(p1, Color.RED, 2) // {1}{R}
        driver.castSpell(p1, dowsing).isSuccess shouldBe true

        var guard = 0
        var handledTarget = false
        while (guard++ < 40) {
            val d = driver.pendingDecision
            when {
                d is ChooseTargetsDecision -> {
                    driver.submitTargetSelection(p1, listOf(creature))
                    handledTarget = true
                }
                d != null -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                !handledTarget -> driver.bothPass() // flush the queued ETB trigger onto the stack
                else -> return dowsing
            }
        }
        return dowsing
    }

    test("entering as the fourth artifact pumps a creature and transforms into Geode Grotto") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        // Three artifacts already out; Dowsing Device will be the fourth.
        repeat(3) { driver.putPermanentOnBattlefield(p1, "Test Relic") }
        val bearId = driver.putCreatureOnBattlefield(p1, "Test Bear")

        val dowsing = castDowsing(driver, p1, bearId)

        val projected = projector.project(driver.state)
        projected.getPower(bearId) shouldBe 3 // 2/2 base + 1/+0
        projected.getToughness(bearId) shouldBe 2
        projected.hasKeyword(bearId, Keyword.HASTE) shouldBe true
        // Four artifacts controlled (3 relics + the device) → the device transforms.
        cardName(driver, dowsing) shouldBe "Geode Grotto"
    }

    test("entering with fewer than four artifacts pumps but does not transform") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val bearId = driver.putCreatureOnBattlefield(p1, "Test Bear")

        val dowsing = castDowsing(driver, p1, bearId)

        val projected = projector.project(driver.state)
        projected.getPower(bearId) shouldBe 3
        projected.hasKeyword(bearId, Keyword.HASTE) shouldBe true
        // Only one artifact (the device itself) → no transform.
        cardName(driver, dowsing) shouldBe "Dowsing Device"
    }
})
