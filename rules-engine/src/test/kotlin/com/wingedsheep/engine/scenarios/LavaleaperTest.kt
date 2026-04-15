package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.Lavaleaper
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Lavaleaper ({3}{R}, 4/4 Elemental).
 * - All creatures have haste.
 * - Whenever a player taps a basic land for mana, that player adds one mana of any
 *   type that land produced.
 *
 * Exercises the new AdditionalManaOnLandTap static ability and GrantKeywordToCreatureGroup
 * with GroupFilter.AllCreatures granting haste globally.
 */
class LavaleaperTest : FunSpec({

    val TestForest = basicLand("Forest") {}
    val TestMountain = basicLand("Mountain") {}

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestForest, TestMountain, Lavaleaper))
        return driver
    }

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all + listOf(TestForest, TestMountain, Lavaleaper))
        return registry
    }

    test("Tapping a basic Mountain with Lavaleaper on the battlefield adds 2 red mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Lavaleaper")
        val mountain = driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        val manaAbilityId = TestMountain.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = mountain, abilityId = manaAbilityId)
        )
        result.isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        pool.red shouldBe 2
    }

    test("Bonus triggers for the tapping player regardless of who controls Lavaleaper") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Lavaleaper controlled by opponent
        driver.putCreatureOnBattlefield(opponent, "Lavaleaper")
        // Active player taps their own basic Forest
        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")

        val manaAbilityId = TestForest.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = forest, abilityId = manaAbilityId)
        )
        result.isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        pool.green shouldBe 2
    }

    test("All creatures have haste while Lavaleaper is on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Lavaleaper")
        val yourBears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val projected = projector.project(driver.state)
        projected.hasKeyword(yourBears, Keyword.HASTE) shouldBe true
        projected.hasKeyword(theirBears, Keyword.HASTE) shouldBe true
    }

    test("ManaSolver sees extra mana when Lavaleaper is in play") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Lavaleaper")
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        val solver = ManaSolver(createRegistry())
        // 1 Mountain + bonus = 2R total
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{R}{R}")) shouldBe true
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{R}{R}{R}")) shouldBe false
    }
})
