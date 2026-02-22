package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.SeasideHaven
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Seaside Haven.
 *
 * Seaside Haven
 * Land
 * {T}: Add {C}.
 * {W}{U}, {T}, Sacrifice a Bird: Draw a card.
 */
class SeasideHavenTest : FunSpec({

    val drawAbilityId = SeasideHaven.activatedAbilities[1].id

    // Non-Bird creature for negative tests
    val GoblinScout = CardDefinition.creature(
        name = "Goblin Scout",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinScout))
        return driver
    }

    test("sacrifice a Bird to draw a card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Seaside Haven on the battlefield
        val haven = driver.putPermanentOnBattlefield(activePlayer, "Seaside Haven")

        // Put a Bird (Birds of Paradise) on the battlefield
        val bird = driver.putCreatureOnBattlefield(activePlayer, "Birds of Paradise")

        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate the draw ability: {W}{U}, {T}, Sacrifice a Bird
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLUE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = haven,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bird))
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Bird should be sacrificed (gone from battlefield)
        driver.findPermanent(activePlayer, "Birds of Paradise") shouldBe null

        // Should have drawn a card
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }

    test("cannot activate without a Bird to sacrifice") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val haven = driver.putPermanentOnBattlefield(activePlayer, "Seaside Haven")

        // Put a non-Bird creature on the battlefield
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Scout")

        val initialHandSize = driver.getHandSize(activePlayer)

        // Try to activate with a non-Bird creature as sacrifice
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLUE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = haven,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goblin))
            )
        )
        result.isSuccess shouldBe false

        // Hand size should not change
        driver.getHandSize(activePlayer) shouldBe initialHandSize
    }

    test("cannot activate without enough mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val haven = driver.putPermanentOnBattlefield(activePlayer, "Seaside Haven")
        val bird = driver.putCreatureOnBattlefield(activePlayer, "Birds of Paradise")

        val initialHandSize = driver.getHandSize(activePlayer)

        // Only give white mana (need both white and blue)
        driver.giveMana(activePlayer, Color.WHITE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = haven,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bird))
            )
        )
        result.isSuccess shouldBe false

        // Hand size should not change
        driver.getHandSize(activePlayer) shouldBe initialHandSize
    }

    test("Seaside Haven remains on battlefield after activation") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val haven = driver.putPermanentOnBattlefield(activePlayer, "Seaside Haven")
        val bird = driver.putCreatureOnBattlefield(activePlayer, "Birds of Paradise")

        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = haven,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bird))
            )
        )
        driver.bothPass()

        // Haven should still be on the battlefield (only the Bird is sacrificed)
        driver.findPermanent(activePlayer, "Seaside Haven") shouldNotBe null
    }
})
