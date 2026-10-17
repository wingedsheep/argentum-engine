package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.IntercessorsArrest
import com.wingedsheep.mtg.sets.definitions.xln.cards.SleekSchooner
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class IntercessorsArrestScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(IntercessorsArrest, SleekSchooner))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.enchant(target: com.wingedsheep.sdk.model.EntityId) {
        val arrest = putCardInHand(player1, "Intercessor's Arrest")
        giveMana(player1, Color.WHITE, 3)
        castSpell(player1, arrest, listOf(target)).error shouldBe null
        bothPass()
    }

    test("the enchanted creature can't crew Vehicles; other creatures still can") {
        val game = driver()
        val arrested = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        val free = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        val schooner = game.putPermanentOnBattlefield(game.player1, "Sleek Schooner")
        game.enchant(arrested)

        val crew = game.legalActions(game.player1).single { it.actionType == "CrewVehicle" }
        val candidates = crew.tapForPowerCreatures!!.map { it.entityId }
        candidates shouldNotContain arrested
        candidates shouldContain free

        game.submit(CrewVehicle(game.player1, schooner, listOf(arrested))).error shouldNotBe null
        game.isTapped(arrested) shouldBe false
        game.submitSuccess(CrewVehicle(game.player1, schooner, listOf(free)))
        game.isTapped(free) shouldBe true
    }

    test("an enchanted Vehicle's crew ability can't be activated") {
        val game = driver()
        val bears = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        val schooner = game.putPermanentOnBattlefield(game.player1, "Sleek Schooner")
        game.enchant(schooner)

        game.legalActions(game.player1).none { it.actionType == "CrewVehicle" } shouldBe true
        game.submit(CrewVehicle(game.player1, schooner, listOf(bears))).error shouldNotBe null
        game.isTapped(bears) shouldBe false
    }
})
