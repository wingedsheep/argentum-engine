package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.DreamHarvest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.core.spec.style.FunSpec

/**
 * Tests for Dream Harvest.
 *
 * Dream Harvest {5}{U/B}{U/B}
 * Sorcery
 * Each opponent exiles cards from the top of their library until they have
 * exiled cards with total mana value 5 or greater this way. Until end of turn,
 * you may cast cards exiled this way without paying their mana costs.
 *
 * ## Covered Scenarios
 * - Exile cards from opponent's library until cumulative MV >= 5, stops on first
 *   card that pushes the sum past the threshold
 * - Lands on top of opponent's library count 0 toward mana value (rule 107.3b)
 * - Exiled cards get MayPlayFromExile + PlayWithoutPayingCost permissions
 *   granted to Dream Harvest's controller (not to each opponent)
 * - Caster's own library is untouched
 * - Opponent's library ends up smaller by the number of cards exiled
 */
class DreamHarvestTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DreamHarvest)
        return driver
    }

    test("exiles cards from opponent's library until cumulative mana value reaches 5") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent's library top: Savannah Lions (1), Centaur Courser (3), Savannah Lions (1)
        // Cumulative MV after exiling each: 1, 4, 5 → stop after third
        driver.putCardOnTopOfLibrary(opponent, "Savannah Lions")
        driver.putCardOnTopOfLibrary(opponent, "Centaur Courser")
        driver.putCardOnTopOfLibrary(opponent, "Savannah Lions")
        // Top order after these pushes: Savannah Lions, Centaur Courser, Savannah Lions, ...

        val dreamHarvest = driver.putCardInHand(caster, "Dream Harvest")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.giveColorlessMana(caster, 5)

        driver.castSpell(caster, dreamHarvest)
        driver.bothPass()

        val exiled = driver.getExile(opponent)
        exiled.size shouldBe 3

        val exiledNames = exiled.map { driver.state.getEntity(it)?.get<CardComponent>()?.name }
        exiledNames shouldContain "Centaur Courser"
        exiledNames.count { it == "Savannah Lions" } shouldBe 2
    }

    test("lands on top count 0 mana value and keep exiling past them") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent's library top: Swamp (0), Swamp (0), Force of Nature (5)
        // Cumulative: 0, 0, 5 → stop after Force of Nature (3 cards exiled)
        driver.putCardOnTopOfLibrary(opponent, "Force of Nature")
        driver.putCardOnTopOfLibrary(opponent, "Swamp")
        driver.putCardOnTopOfLibrary(opponent, "Swamp")

        val dreamHarvest = driver.putCardInHand(caster, "Dream Harvest")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.giveColorlessMana(caster, 5)

        driver.castSpell(caster, dreamHarvest)
        driver.bothPass()

        val exiled = driver.getExile(opponent)
        exiled.size shouldBe 3

        val exiledNames = exiled.map { driver.state.getEntity(it)?.get<CardComponent>()?.name }
        exiledNames shouldContain "Force of Nature"
        exiledNames.count { it == "Swamp" } shouldBe 2
    }

    test("grants MayPlayFromExile and PlayWithoutPayingCost to caster on every exiled card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(opponent, "Force of Nature") // MV 5 — one card is enough

        val dreamHarvest = driver.putCardInHand(caster, "Dream Harvest")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.giveColorlessMana(caster, 5)

        driver.castSpell(caster, dreamHarvest)
        driver.bothPass()

        val exiled = driver.getExile(opponent)
        exiled.size shouldBe 1

        val exiledCard = exiled.first()
        val mayPlay = driver.state.getEntity(exiledCard)?.get<MayPlayFromExileComponent>()
        val freeCast = driver.state.getEntity(exiledCard)?.get<PlayWithoutPayingCostComponent>()

        mayPlay shouldNotBe null
        freeCast shouldNotBe null
        // Controller of the impulse-play permission must be the Dream Harvest caster,
        // not the opponent whose library was exiled.
        mayPlay!!.controllerId shouldBe caster
        freeCast!!.controllerId shouldBe caster
    }

    test("does not touch the caster's own library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(opponent, "Force of Nature")

        val casterLibraryBefore = driver.state.getZone(
            com.wingedsheep.engine.state.ZoneKey(caster, com.wingedsheep.sdk.core.Zone.LIBRARY)
        ).size

        val dreamHarvest = driver.putCardInHand(caster, "Dream Harvest")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.giveColorlessMana(caster, 5)

        driver.castSpell(caster, dreamHarvest)
        driver.bothPass()

        driver.getExile(caster).size shouldBe 0
        val casterLibraryAfter = driver.state.getZone(
            com.wingedsheep.engine.state.ZoneKey(caster, com.wingedsheep.sdk.core.Zone.LIBRARY)
        ).size
        casterLibraryAfter shouldBe casterLibraryBefore
    }

    test("empties opponent's library if threshold is never reached") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 5, "Swamp" to 5),
            startingLife = 20
        )

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opponentLibraryBefore = driver.state.getZone(
            com.wingedsheep.engine.state.ZoneKey(opponent, com.wingedsheep.sdk.core.Zone.LIBRARY)
        ).size

        val dreamHarvest = driver.putCardInHand(caster, "Dream Harvest")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.giveColorlessMana(caster, 5)

        driver.castSpell(caster, dreamHarvest)
        driver.bothPass()

        val opponentLibraryAfter = driver.state.getZone(
            com.wingedsheep.engine.state.ZoneKey(opponent, com.wingedsheep.sdk.core.Zone.LIBRARY)
        ).size
        val exileSize = driver.getExile(opponent).size

        opponentLibraryAfter shouldBe 0
        exileSize shouldBe opponentLibraryBefore
    }
})
