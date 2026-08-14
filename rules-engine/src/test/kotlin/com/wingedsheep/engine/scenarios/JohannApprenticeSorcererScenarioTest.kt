package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ons.cards.FutureSight
import com.wingedsheep.mtg.sets.definitions.woe.cards.JohannApprenticeSorcerer
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JohannApprenticeSorcererScenarioTest : FunSpec({

    val testInstant = CardDefinition.instant(
        name = "Johann Test Instant",
        manaCost = ManaCost.ZERO,
        oracleText = "You gain 1 life.",
        script = CardScript.spell(Effects.GainLife(1))
    )
    val secondInstant = CardDefinition.instant(
        name = "Johann Second Test Instant",
        manaCost = ManaCost.ZERO,
        oracleText = "You gain 1 life.",
        script = CardScript.spell(Effects.GainLife(1))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JohannApprenticeSorcerer, FutureSight, testInstant, secondInstant))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.canCastFromLibrary(cardName: String): Boolean = legalActions(activePlayer!!).any {
        it.actionType == "CastSpell" && it.sourceZone == "LIBRARY" && it.description.contains(cardName)
    }

    test("casts only one instant or sorcery from the top each turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Johann, Apprentice Sorcerer")

        val first = driver.putCardOnTopOfLibrary(you, "Johann Test Instant")
        driver.canCastFromLibrary("Johann Test Instant") shouldBe true
        driver.castSpell(you, first).error shouldBe null
        driver.bothPass()

        val second = driver.putCardOnTopOfLibrary(you, "Johann Second Test Instant")
        driver.canCastFromLibrary("Johann Second Test Instant") shouldBe false
        driver.castSpell(you, second).error shouldBe "Card is not in your hand"
    }

    test("a new Johann object has a fresh permission in the same turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val oldJohann = driver.putCreatureOnBattlefield(you, "Johann, Apprentice Sorcerer")

        val first = driver.putCardOnTopOfLibrary(you, "Johann Test Instant")
        driver.castSpell(you, first).error shouldBe null
        driver.bothPass()

        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(you, Zone.BATTLEFIELD), oldJohann)
                .withoutEntity(oldJohann)
        )
        driver.putCreatureOnBattlefield(you, "Johann, Apprentice Sorcerer")

        val second = driver.putCardOnTopOfLibrary(you, "Johann Second Test Instant")
        driver.canCastFromLibrary("Johann Second Test Instant") shouldBe true
        driver.castSpell(you, second).error shouldBe null
    }

    test("an unlimited permission does not consume Johann's limited permission") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Johann, Apprentice Sorcerer")
        val futureSight = driver.putPermanentOnBattlefield(you, "Future Sight")

        val first = driver.putCardOnTopOfLibrary(you, "Johann Test Instant")
        driver.castSpell(you, first).error shouldBe null
        driver.bothPass()

        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(you, Zone.BATTLEFIELD), futureSight)
                .withoutEntity(futureSight)
        )
        val second = driver.putCardOnTopOfLibrary(you, "Johann Second Test Instant")
        driver.canCastFromLibrary("Johann Second Test Instant") shouldBe true
    }

    test("does not allow a creature spell from the top") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Johann, Apprentice Sorcerer")
        driver.putCardOnTopOfLibrary(you, "Grizzly Bears")

        driver.canCastFromLibrary("Grizzly Bears") shouldBe false
    }
})
