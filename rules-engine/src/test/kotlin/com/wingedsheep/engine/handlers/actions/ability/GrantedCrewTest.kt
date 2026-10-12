package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Weatherlight
import com.wingedsheep.mtg.sets.definitions.otj.cards.LuxuriousLocomotive
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Granted crew — "Vehicles you control have crew N" (Astor, Bearer of Blades), the projected
 * `CREW_<n>` keyword ([KeywordAbility.grantedCrew]).
 *
 * Each crew instance is its own activated ability (CR 702.122a), so a granted crew sits *beside* a
 * printed one rather than adding to it: the cheapest available instance is what the crew action
 * asks for, repeated grants of the same N don't sum, and a once-each-turn printed crew that's been
 * used leaves the granted one still open. A printed crew rides the projected bare `CREW` keyword,
 * so it disappears with a lose-all-abilities effect.
 */
class GrantedCrewTest : FunSpec({

    fun crewLord(name: String, n: Int) = card(name) {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Vehicles you control have crew $n."
        staticAbility {
            ability = GrantKeyword(
                KeywordAbility.grantedCrew(n),
                GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE).youControl()),
            )
        }
    }

    val vehicleSuppressor = card("Test Vehicle Suppressor") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Vehicles lose all abilities."
        staticAbility {
            ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE)))
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(
            listOf(
                Weatherlight,
                LuxuriousLocomotive,
                crewLord("Test Crew One Lord", 1),
                crewLord("Test Crew One Lord Again", 1),
                crewLord("Test Crew Two Lord", 2),
                vehicleSuppressor,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.crewRequirement(playerId: EntityId, vehicle: EntityId): Int? =
        legalActions(playerId)
            .firstOrNull { it.action is CrewVehicle && (it.action as CrewVehicle).vehicleId == vehicle }
            ?.tapForPowerRequired

    test("a granted crew 1 lets a Vehicle with printed crew 3 be crewed for 1") {
        val driver = setup()
        val me = driver.activePlayer!!
        val weatherlight = driver.putPermanentOnBattlefield(me, "Weatherlight")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        driver.crewRequirement(me, weatherlight) shouldBe 3
        driver.submitExpectFailure(CrewVehicle(me, weatherlight, listOf(lions)))

        driver.putPermanentOnBattlefield(me, "Test Crew One Lord")
        driver.crewRequirement(me, weatherlight) shouldBe 1
        driver.submitSuccess(CrewVehicle(me, weatherlight, listOf(lions)))
    }

    test("two grants of crew 1 are two crew 1 abilities, not crew 2") {
        val driver = setup()
        val me = driver.activePlayer!!
        val weatherlight = driver.putPermanentOnBattlefield(me, "Weatherlight")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.putPermanentOnBattlefield(me, "Test Crew One Lord")
        driver.putPermanentOnBattlefield(me, "Test Crew One Lord Again")

        driver.state.projectedState.getKeywords(weatherlight).filter { it.startsWith("CREW_") } shouldBe listOf("CREW_1")
        driver.crewRequirement(me, weatherlight) shouldBe 1
        driver.submitSuccess(CrewVehicle(me, weatherlight, listOf(lions)))
    }

    test("granted crew 2 and crew 1 sit side by side; the cheaper one is offered") {
        val driver = setup()
        val me = driver.activePlayer!!
        val weatherlight = driver.putPermanentOnBattlefield(me, "Weatherlight")
        driver.putPermanentOnBattlefield(me, "Test Crew Two Lord")
        driver.crewRequirement(me, weatherlight) shouldBe 2

        driver.putPermanentOnBattlefield(me, "Test Crew One Lord")
        driver.state.projectedState.getKeywords(weatherlight).filter { it.startsWith("CREW_") }.toSet() shouldBe
            setOf("CREW_1", "CREW_2")
        driver.crewRequirement(me, weatherlight) shouldBe 1
    }

    test("the grant reaches a permanent with no printed crew only if it is a Vehicle you control") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val theirs = driver.putPermanentOnBattlefield(opponent, "Weatherlight")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.putPermanentOnBattlefield(me, "Test Crew One Lord")

        driver.state.projectedState.getKeywords(theirs).none { it.startsWith("CREW_") } shouldBe true
        driver.state.projectedState.getKeywords(lions).none { it.startsWith("CREW_") } shouldBe true
    }

    test("a used once-each-turn printed crew leaves the granted crew available") {
        val driver = setup()
        val me = driver.activePlayer!!
        val loco = driver.putPermanentOnBattlefield(me, "Luxurious Locomotive")
        val bears1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bears2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.submitSuccess(CrewVehicle(me, loco, listOf(bears1)))
        driver.bothPass()
        driver.crewRequirement(me, loco).shouldBeNull()
        driver.submitExpectFailure(CrewVehicle(me, loco, listOf(bears2))).error.shouldNotBeNull()
            .shouldContain("once each turn")

        driver.putPermanentOnBattlefield(me, "Test Crew Two Lord")
        driver.crewRequirement(me, loco) shouldBe 2
        driver.submitSuccess(CrewVehicle(me, loco, listOf(bears2)))
    }

    test("a Vehicle that lost all abilities has no printed crew") {
        val driver = setup()
        val me = driver.activePlayer!!
        val weatherlight = driver.putPermanentOnBattlefield(me, "Weatherlight")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.putPermanentOnBattlefield(me, "Test Vehicle Suppressor")

        driver.crewRequirement(me, weatherlight).shouldBeNull()
        driver.submitExpectFailure(CrewVehicle(me, weatherlight, listOf(lions))).error.shouldNotBeNull()
            .shouldContain("doesn't have crew")
    }

    test("the granted crew ends when its source leaves the battlefield") {
        val driver = setup()
        val me = driver.activePlayer!!
        val weatherlight = driver.putPermanentOnBattlefield(me, "Weatherlight")
        val lord = driver.putPermanentOnBattlefield(me, "Test Crew One Lord")
        driver.crewRequirement(me, weatherlight) shouldBe 1

        driver.moveToGraveyard(lord)
        driver.crewRequirement(me, weatherlight) shouldBe 3
    }
})
