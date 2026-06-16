package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TombTrawler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tomb Trawler — {2} 0/4 Artifact Creature — Golem
 *
 * "{2}: Put target card from your graveyard on the bottom of your library."
 */
class TombTrawlerScenarioTest : FunSpec({

    val trawlerAbilityId = TombTrawler.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TombTrawler)
        return driver
    }

    test("puts a target card from your graveyard on the bottom of your library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Tomb Trawler")
        val trawler = driver.findPermanent(player, "Tomb Trawler")!!

        // A Grizzly Bears card sits in the graveyard.
        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.getGraveyardCardNames(player).contains("Grizzly Bears") shouldBe true

        driver.giveColorlessMana(player, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = trawler,
                abilityId = trawlerAbilityId,
                targets = listOf(ChosenTarget.Card(bears, player, Zone.GRAVEYARD)),
            ),
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve the ability

        // The card left the graveyard and is now the bottom card of the library.
        driver.getGraveyardCardNames(player).contains("Grizzly Bears") shouldBe false
        val library = driver.state.getLibrary(player)
        val bottomId = library.last()
        driver.state.getEntity(bottomId)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
        driver.state.getEntity(bottomId) shouldNotBe null
    }
})
