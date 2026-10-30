package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

class FangDruidSummonerScenarioTest : FunSpec({
    test("enters and finds only a creature card with no abilities from library or graveyard") {
        val driver = GameTestDriver().also { it.registerCards(TestCards.all) }
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vanilla = driver.putCardInGraveyard(active, "Grizzly Bears")
        val creatureWithAbilities = driver.putCardOnTopOfLibrary(active, "Llanowar Elves")
        val summoner = driver.putCardInHand(active, "Fang-Druid Summoner")
        driver.giveMana(active, Color.GREEN, 1)
        driver.giveColorlessMana(active, 3)

        driver.castSpell(active, summoner).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.bothPass()

        // "Your library and/or graveyard": choose which zones to search.
        driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>().let {
            driver.submitDecision(active, OptionChosenResponse(it.id, it.options.indexOf("Search your library and graveyard")))
        }
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain vanilla
        decision.options shouldNotContain creatureWithAbilities
        driver.submitDecision(active, CardsSelectedResponse(decision.id, listOf(vanilla)))

        driver.findCardInHand(active, "Grizzly Bears") shouldBe vanilla
    }
})
