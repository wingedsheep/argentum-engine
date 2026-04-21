package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.ThornvaultForager
import com.wingedsheep.mtg.sets.definitions.portal.cards.ScorchingSpear
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ThornvaultForagerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThornvaultForager, ScorchingSpear))
        return driver
    }

    test("Forage mana ability is unaffordable with empty graveyard and no Food") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, active, EnumerationMode.FULL)
        val foragerManaAbilities = actions.filter {
            val a = it.action
            a is ActivateAbility && a.sourceId == forager && it.isManaAbility
        }

        // Two mana abilities: {T}: Add {G}  and  {T}, Forage: Add two mana.
        // The Forage one must be unaffordable — empty graveyard, no Food.
        foragerManaAbilities.size shouldBe 2
        foragerManaAbilities.count { it.affordable } shouldBe 1
    }

    test("Forage mana ability is affordable when graveyard has 3+ cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )
        repeat(3) { driver.putCardInGraveyard(active, "Forest") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, active, EnumerationMode.FULL)
        val foragerManaAbilities = actions.filter {
            val a = it.action
            a is ActivateAbility && a.sourceId == forager && it.isManaAbility
        }

        foragerManaAbilities.size shouldBe 2
        foragerManaAbilities.all { it.affordable } shouldBe true
    }

    test("ManaSolver exposes Thornvault Forager as a green-only source with empty graveyard and no Food") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )

        val solver = ManaSolver(driver.cardRegistry)
        val source = solver.findAvailableManaSources(driver.state, active)
            .find { it.entityId == forager }

        source shouldNotBe null
        source!!.producesColors shouldContain Color.GREEN
        // Forage cost can't be paid (empty graveyard, no Food) — any-color production
        // must not leak through via the second ability.
        source.producesColors shouldNotContain Color.RED
        source.producesColors shouldNotContain Color.BLUE
        source.producesColors shouldNotContain Color.BLACK
        source.producesColors shouldNotContain Color.WHITE
    }

    test("ManaSolver does not auto-tap Thornvault Forager for any-color even when Forage is payable") {
        // Forage is a choice cost (exile 3 graveyard cards OR sacrifice a Food). The solver
        // must never auto-pay it as a side effect of covering a spell's mana — the player
        // must explicitly activate the ability. So even with a full graveyard, the solver
        // should only see Thornvault as a {G} source.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )
        repeat(3) { driver.putCardInGraveyard(active, "Forest") }

        val solver = ManaSolver(driver.cardRegistry)
        val source = solver.findAvailableManaSources(driver.state, active)
            .find { it.entityId == forager }

        source shouldNotBe null
        source!!.producesColors shouldContain Color.GREEN
        source.producesColors shouldNotContain Color.RED
        source.producesColors shouldNotContain Color.BLUE
        source.producesColors shouldNotContain Color.BLACK
        source.producesColors shouldNotContain Color.WHITE
    }

    test("Auto-tap cannot cast Scorching Spear through Thornvault Forager when Forage is unpayable") {
        // End-to-end regression: the real bug surfaced during spell auto-pay. Thornvault
        // Forager should not be treated as a red source because its any-color ability
        // requires Forage, which the player can't pay (empty graveyard, no Food).
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != active }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )
        val spear = driver.putCardInHand(active, "Scorching Spear")

        val result = driver.castSpell(active, spear, targets = listOf(opponent))

        result.isSuccess shouldBe false
    }
})
