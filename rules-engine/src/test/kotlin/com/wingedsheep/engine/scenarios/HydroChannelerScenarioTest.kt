package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.HydroChanneler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hydro-Channeler — {1}{U} 1/3 Creature — Merfolk Wizard
 *
 * {T}: Add {U}. Spend this mana only to cast an instant or sorcery spell.
 * {1}, {T}: Add one mana of any color. Spend this mana only to cast an instant or sorcery spell.
 */
class HydroChannelerScenarioTest : FunSpec({

    val blueAbilityId = HydroChanneler.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HydroChanneler)
        return driver
    }

    test("{T}: Add {U} produces blue mana restricted to instants/sorceries") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val channeler = driver.putCreatureOnBattlefield(player, "Hydro-Channeler")
        driver.removeSummoningSickness(channeler)

        val result = driver.submit(
            ActivateAbility(playerId = player, sourceId = channeler, abilityId = blueAbilityId)
        )
        result.error shouldBe null
        driver.isTapped(channeler) shouldBe true

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()!!
        // The mana is restricted, so it lands in the restricted pool, not the plain blue count.
        pool.blue shouldBe 0
        pool.restrictedMana.size shouldBe 1
        val entry = pool.restrictedMana.single()
        entry.color shouldBe Color.BLUE
        entry.restriction shouldBe ManaRestriction.InstantOrSorceryOnly
    }
})
