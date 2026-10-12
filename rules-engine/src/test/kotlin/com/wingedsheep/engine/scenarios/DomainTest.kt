package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Tests for the Domain ability word — read end-to-end via [DynamicAmounts.domain].
 *
 * Uses a synthetic artifact whose activated ability draws cards equal to domain,
 * so each test asserts a concrete integer count without standing up a damage stack.
 *
 * Relevant rules: 207.2c (basic land types), and the Domain reminder text:
 * "for each basic land type among lands you control" — capped at 5, dual-typed
 * lands contribute each of their basic subtypes.
 */
class DomainTest : FunSpec({

    val DomainDrawer = card("Domain Drawer") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{T}: Domain — Draw a card for each basic land type among lands you control."

        activatedAbility {
            cost = Costs.Tap
            effect = DrawCardsEffect(
                count = DynamicAmounts.domain(),
                target = EffectTarget.Controller
            )
        }
    }

    // Dual land carrying both Plains and Island subtypes — matches Hallowed Fountain
    // / Tundra. Used to verify a single land contributes two basic subtypes to domain.
    val PlainsIslandDual = card("Plains Island Dual") {
        typeLine = "Land — Plains Island"
        oracleText = "({T}: Add {W} or {U}.)"
    }

    // "Target land loses all land types" — the shape of Ultima, Origin of Oblivion's blight. The
    // land keeps its card type, so it still matches the domain filter but projects no subtypes.
    val LandTypeEraser = card("Land Type Eraser") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{T}: Target land loses all land types."

        activatedAbility {
            cost = Costs.Tap
            val land = target(TargetFilter.Land)
            effect = Effects.BecomeArtifact(
                target = land,
                cardTypes = null,
                subtypes = emptySet(),
                colors = null,
                loseAllAbilities = false
            )
        }
    }

    val abilityId = DomainDrawer.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DomainDrawer)
        driver.registerCard(PlainsIslandDual)
        driver.registerCard(LandTypeEraser)
        return driver
    }

    fun setUpDrawer(driver: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Mountain" to 30),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drawer = driver.putPermanentOnBattlefield(activePlayer, "Domain Drawer")
        return activePlayer to drawer
    }

    fun activateAndCount(driver: GameTestDriver, activePlayer: com.wingedsheep.sdk.model.EntityId, drawer: com.wingedsheep.sdk.model.EntityId): Int {
        val handBefore = driver.getHandSize(activePlayer)
        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = drawer, abilityId = abilityId)
        )
        result.outcome shouldBe Outcome.Done
        driver.bothPass()
        return driver.getHandSize(activePlayer) - handBefore
    }

    test("zero basic land types — draws zero") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        // No lands on battlefield (mulligans dropped initial draw)
        activateAndCount(driver, activePlayer, drawer) shouldBe 0
    }

    test("one Forest — draws one") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        driver.putLandOnBattlefield(activePlayer, "Forest")
        activateAndCount(driver, activePlayer, drawer) shouldBe 1
    }

    test("two Forests — duplicates don't increase domain") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Forest")
        activateAndCount(driver, activePlayer, drawer) shouldBe 1
    }

    test("Forest + Island — two distinct types") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Island")
        activateAndCount(driver, activePlayer, drawer) shouldBe 2
    }

    test("all five basic types — caps at five") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putLandOnBattlefield(activePlayer, "Island")
        driver.putLandOnBattlefield(activePlayer, "Swamp")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Forest")
        // Extra basics shouldn't push past 5
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Plains")
        activateAndCount(driver, activePlayer, drawer) shouldBe 5
    }

    test("dual-typed land contributes both subtypes") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        // One dual (Plains+Island) plus a Forest = 3 distinct basic types
        driver.putLandOnBattlefield(activePlayer, "Plains Island Dual")
        driver.putLandOnBattlefield(activePlayer, "Forest")
        activateAndCount(driver, activePlayer, drawer) shouldBe 3
    }

    test("two dual-typed lands of same subtypes don't double-count") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        // Two Plains+Island duals = still only 2 distinct basic types
        driver.putLandOnBattlefield(activePlayer, "Plains Island Dual")
        driver.putLandOnBattlefield(activePlayer, "Plains Island Dual")
        activateAndCount(driver, activePlayer, drawer) shouldBe 2
    }

    test("dual + matching basic — basic adds nothing new") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        // Plains+Island dual already covers Plains, so an extra Plains is redundant
        driver.putLandOnBattlefield(activePlayer, "Plains Island Dual")
        driver.putLandOnBattlefield(activePlayer, "Plains")
        activateAndCount(driver, activePlayer, drawer) shouldBe 2
    }

    test("opponent's lands don't count toward your domain") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        val opponent = driver.state.turnOrder.first { it != activePlayer }
        driver.putLandOnBattlefield(opponent, "Plains")
        driver.putLandOnBattlefield(opponent, "Island")
        driver.putLandOnBattlefield(opponent, "Swamp")
        driver.putLandOnBattlefield(activePlayer, "Forest")
        activateAndCount(driver, activePlayer, drawer) shouldBe 1
    }

    test("a land that lost all land types contributes nothing") {
        val driver = createDriver()
        val (activePlayer, drawer) = setUpDrawer(driver)
        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Island")
        val eraser = driver.putPermanentOnBattlefield(activePlayer, "Land Type Eraser")
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = eraser,
                abilityId = LandTypeEraser.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(forest))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.projectedState.hasType(forest, "LAND") shouldBe true
        driver.state.projectedState.getSubtypes(forest) shouldBe emptySet()
        // The printed Forest subtype must not leak back in through a base-state fallback.
        activateAndCount(driver, activePlayer, drawer) shouldBe 1
    }
})
