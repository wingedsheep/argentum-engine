package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.GroundchuckAndDirtbag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Groundchuck & Dirtbag ({4}{G}{G}, 8/8 Ox Mole Mutant).
 *
 *   Trample
 *   Whenever you tap a land for mana, add {G}.
 *
 * This is a triggered MANA ability (CR 605.1b): it adds mana and triggers off a mana ability,
 * so it must resolve immediately without using the stack — the {G} has to be in the pool for the
 * same payment. The card is built on [com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap]
 * (the engine's off-stack resolver, shared with Badgermole Cub's "...tap a creature for mana..."),
 * not a generic `landTappedForMana` triggered ability — which is not wired to off-stack mana
 * resolution and so adds no mana (the reported bug).
 */
class GroundchuckAndDirtbagTest : FunSpec({

    // A nonbasic Land with an explicit "{T}: Add {G}" mana ability, so the test can drive the
    // manual mana-ability activation path directly (no intrinsic-ability id plumbing).
    val tapForGreenLand = card("Tap-for-Green Land") {
        typeLine = "Land"
        oracleText = "{T}: Add {G}."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN, 1)
            manaAbility = true
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GroundchuckAndDirtbag, tapForGreenLand))
        return driver
    }

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all + listOf(GroundchuckAndDirtbag, tapForGreenLand))
        return registry
    }

    test("tapping a land for mana adds the extra {G} to the pool immediately") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Groundchuck & Dirtbag")
        val land = driver.putLandOnBattlefield(activePlayer, "Tap-for-Green Land")

        val manaAbilityId = tapForGreenLand.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = land, abilityId = manaAbilityId)
        )
        result.isSuccess shouldBe true

        // 1 from the land + 1 bonus from Groundchuck = 2 green, available right now.
        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        pool.green shouldBe 2
    }

    test("ManaSolver counts the Groundchuck bonus when costing land mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Groundchuck & Dirtbag")
        driver.putLandOnBattlefield(activePlayer, "Tap-for-Green Land")

        val solver = ManaSolver(createRegistry())
        // 1 from the land + 1 bonus from Groundchuck = 2 green available
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{G}{G}")) shouldBe true
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{G}{G}{G}")) shouldBe false
    }

    test("bonus does not fire when an opponent controls Groundchuck") {
        // "Whenever you tap a land for mana" — the bonus belongs to Groundchuck's controller. If P2
        // controls Groundchuck but P1 taps their own land, neither player gets the extra {G}.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Groundchuck & Dirtbag")
        val land = driver.putLandOnBattlefield(activePlayer, "Tap-for-Green Land")

        val manaAbilityId = tapForGreenLand.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = land, abilityId = manaAbilityId)
        )
        result.isSuccess shouldBe true

        val activePool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        val opponentPool = driver.state.getEntity(opponent)?.get<ManaPoolComponent>()!!
        activePool.green shouldBe 1   // just the land — no Groundchuck bonus
        opponentPool.green shouldBe 0 // opponent controls Groundchuck but didn't tap anything
    }
})
