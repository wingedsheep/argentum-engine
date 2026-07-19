package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.RavenousAmulet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ravenous Amulet (FDN #131).
 *
 * {1}, {T}, Sacrifice a creature: Draw a card and put a soul counter on this artifact.
 *   Activate only as a sorcery.
 * {4}, {T}, Sacrifice this artifact: Each opponent loses life equal to the number of soul
 *   counters on this artifact.
 *
 * Proves the new [com.wingedsheep.sdk.core.Counters.SOUL] passive counter: the first ability
 * accrues one per activation (and draws), and the second reads the accumulated count to size the
 * life each opponent loses.
 */
class RavenousAmuletScenarioTest : FunSpec({

    val soulAbilityId = RavenousAmulet.activatedAbilities[0].id
    val drainAbilityId = RavenousAmulet.activatedAbilities[1].id

    val fodder = CardDefinition.creature(
        name = "Soul Fodder",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RavenousAmulet, fodder))
        return driver
    }

    fun soulCounters(driver: GameTestDriver, amulet: EntityId): Int =
        driver.state.getEntity(amulet)?.get<CountersComponent>()?.getCount(CounterType.SOUL) ?: 0

    test("first ability: sacrifice a creature draws a card and adds a soul counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val amulet = driver.putPermanentOnBattlefield(you, "Ravenous Amulet")
        val creature = driver.putCreatureOnBattlefield(you, "Soul Fodder")
        val handBefore = driver.getHand(you).size
        driver.giveColorlessMana(you, 1)

        driver.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = amulet,
                abilityId = soulAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(creature))
            )
        )
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        soulCounters(driver, amulet) shouldBe 1
        driver.getHand(you).size shouldBe handBefore + 1
    }

    test("second ability: each opponent loses life equal to the soul counters on the amulet") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val amulet = driver.putPermanentOnBattlefield(you, "Ravenous Amulet")

        // Accrue two soul counters by activating the first ability twice, untapping between.
        repeat(2) {
            val creature = driver.putCreatureOnBattlefield(you, "Soul Fodder")
            driver.untapPermanent(amulet)
            driver.giveColorlessMana(you, 1)
            driver.submitSuccess(
                ActivateAbility(
                    playerId = you,
                    sourceId = amulet,
                    abilityId = soulAbilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(creature))
                )
            )
            while (driver.state.stack.isNotEmpty()) driver.bothPass()
        }
        soulCounters(driver, amulet) shouldBe 2

        // Now drain: sacrifice the amulet for {4}.
        driver.untapPermanent(amulet)
        driver.giveColorlessMana(you, 4)
        val opponentLifeBefore = driver.getLifeTotal(opponent)

        driver.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = amulet,
                abilityId = drainAbilityId,
            )
        )
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        (opponentLifeBefore - driver.getLifeTotal(opponent)) shouldBe 2
    }
})
