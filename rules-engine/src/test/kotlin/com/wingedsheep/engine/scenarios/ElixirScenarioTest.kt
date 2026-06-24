package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Elixir (FIN #256) — {1} Artifact.
 *
 * "This artifact enters tapped.
 *  {5}, {T}, Exile this artifact: Shuffle all nonland cards from your graveyard into your library.
 *  You gain life equal to the number of cards shuffled into your library this way."
 *
 * Verifies the filtered shuffle (lands stay in the graveyard) and that the life gained equals the
 * number of nonland cards moved.
 */
class ElixirScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("shuffles only nonland cards into library and gains life equal to the count") {
        val driver = newDriver()
        val me = driver.player1

        // Three nonland cards + two lands in the graveyard.
        driver.putCardInGraveyard(me, "Grizzly Bears")
        driver.putCardInGraveyard(me, "Hill Giant")
        driver.putCardInGraveyard(me, "Shock")
        driver.putCardInGraveyard(me, "Forest")
        driver.putCardInGraveyard(me, "Forest")

        val elixir = driver.putPermanentOnBattlefield(me, "Elixir")
        driver.untapPermanent(elixir)

        val startingLife = driver.getLifeTotal(me)
        val abilityId = driver.cardRegistry.requireCard("Elixir").activatedAbilities[0].id

        driver.giveColorlessMana(me, 5)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = elixir,
                abilityId = abilityId
            )
        )
        driver.bothPass()

        // Gained life equal to the three nonland cards shuffled in.
        driver.getLifeTotal(me) shouldBe startingLife + 3

        // The two lands remain in the graveyard.
        val graveyard = driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD))
            .mapNotNull { driver.state.getEntity(it)?.get<CardComponent>()?.name }
        graveyard.count { it == "Forest" } shouldBe 2
        graveyard.contains("Grizzly Bears") shouldBe false
        graveyard.contains("Hill Giant") shouldBe false
        graveyard.contains("Shock") shouldBe false
    }
})
