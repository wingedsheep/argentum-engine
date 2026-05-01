package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominariaunited.cards.AdarkarWastes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Adarkar Wastes (DMU #243).
 *
 * Adarkar Wastes:
 * Land
 * {T}: Add {C}.
 * {T}: Add {W} or {U}. This land deals 1 damage to you.
 *
 * The pain damage is part of the *effect* of the second mana ability — not a cost.
 * It must be dealt every time the colored ability resolves, including when the engine
 * auto-taps the land to pay for a spell.
 */
class AdarkarWastesTest : FunSpec({

    val colorlessAbilityId = AdarkarWastes.activatedAbilities[0].id
    val whiteAbilityId = AdarkarWastes.activatedAbilities[1].id
    val blueAbilityId = AdarkarWastes.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AdarkarWastes)
        return driver
    }

    test("activating the {W} ability adds white mana and deals 1 damage to controller") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val wastes = driver.putPermanentOnBattlefield(activePlayer, "Adarkar Wastes")

        val before = driver.getLifeTotal(activePlayer)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wastes,
                abilityId = whiteAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Land is tapped
        driver.isTapped(wastes) shouldBe true

        // White mana was added
        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()
        pool?.white shouldBe 1

        // Controller took 1 pain damage
        driver.getLifeTotal(activePlayer) shouldBe (before - 1)
    }

    test("activating the {U} ability adds blue mana and deals 1 damage to controller") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val wastes = driver.putPermanentOnBattlefield(activePlayer, "Adarkar Wastes")

        val before = driver.getLifeTotal(activePlayer)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wastes,
                abilityId = blueAbilityId
            )
        )
        result.isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()
        pool?.blue shouldBe 1

        driver.getLifeTotal(activePlayer) shouldBe (before - 1)
    }

    test("activating the {C} ability does NOT deal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val wastes = driver.putPermanentOnBattlefield(activePlayer, "Adarkar Wastes")

        val before = driver.getLifeTotal(activePlayer)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wastes,
                abilityId = colorlessAbilityId
            )
        )
        result.isSuccess shouldBe true

        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()
        pool?.colorless shouldBe 1

        // No pain — colorless ability has no damage effect
        driver.getLifeTotal(activePlayer) shouldBe before
    }

    test("auto-paying a {W} spell with Adarkar Wastes deals 1 damage to controller") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val wastes = driver.putPermanentOnBattlefield(activePlayer, "Adarkar Wastes")
        val lions = driver.putCardInHand(activePlayer, "Savannah Lions")

        val before = driver.getLifeTotal(activePlayer)

        // Cast Savannah Lions ({W}). The only available source for white mana is
        // Adarkar Wastes, so the auto-pay solver must tap it. Tapping the
        // {T}: Add {W} ability resolves its full effect, which includes
        // "this land deals 1 damage to you".
        val result = driver.castSpell(activePlayer, lions)
        result.isSuccess shouldBe true

        driver.isTapped(wastes) shouldBe true
        driver.getLifeTotal(activePlayer) shouldBe (before - 1)
    }
})
