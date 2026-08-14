package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.LifecraftEngine
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val projector = StateProjector()

/**
 * Lifecraft Engine (DFT) — {3} 4/4 Artifact — Vehicle
 *
 * "As this Vehicle enters, choose a creature type.
 *  Vehicle creatures you control are the chosen creature type in addition to their other types.
 *  Each creature you control of the chosen type other than this Vehicle gets +1/+1.
 *  Crew 3"
 *
 * The interaction worth pinning is the CR 613.8a dependency: the type grant applies to *Vehicle
 * creatures*, so crew's animation changes which permanents the grant affects and must be applied
 * first within Layer 4 — even though the Engine's static has the earlier timestamp.
 */
class LifecraftEngineScenarioTest : FunSpec({

    val pilot = CardDefinition.creature(
        name = "Test Pilot",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Pilot")),
        power = 2,
        toughness = 2
    )

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    // A second Vehicle, cheap to crew, so the grant has a non-source Vehicle to act on.
    val scooter = card("Test Scooter") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.crew(1))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LifecraftEngine, pilot, bear))
        driver.registerCard(scooter)
        return driver
    }

    /** Put the Engine out with [type] already locked into its creature-type choice slot. */
    fun GameTestDriver.putEngine(playerId: EntityId, type: String): EntityId {
        val engine = putPermanentOnBattlefield(playerId, "Lifecraft Engine")
        replaceState(state.updateEntity(engine) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        })
        return engine
    }

    test("creatures you control of the chosen type get +1/+1; others and opponents' do not") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opponent = driver.getOpponent(you)

        val engine = driver.putEngine(you, "Pilot")
        val yourPilot = driver.putCreatureOnBattlefield(you, "Test Pilot")
        val yourBear = driver.putCreatureOnBattlefield(you, "Test Bear")
        val theirPilot = driver.putCreatureOnBattlefield(opponent, "Test Pilot")

        val projected = projector.project(driver.state)

        projected.getPower(yourPilot) shouldBe 3
        projected.getToughness(yourPilot) shouldBe 3

        // Not the chosen type.
        projected.getPower(yourBear) shouldBe 2
        projected.getToughness(yourBear) shouldBe 2

        // "creature you control" — an opponent's Pilot is untouched.
        projected.getPower(theirPilot) shouldBe 2
        projected.getToughness(theirPilot) shouldBe 2

        // Uncrewed, the Engine is not a creature, so its own type grant doesn't reach it.
        projected.hasSubtype(engine, "Pilot") shouldBe false
    }

    test("a crewed Vehicle you control becomes the chosen type and gets +1/+1 (CR 613.8a dependency)") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1

        // The Engine's static is on the battlefield first, so it holds the earlier timestamp.
        driver.putEngine(you, "Pilot")
        val scooterId = driver.putPermanentOnBattlefield(you, "Test Scooter")

        // Before crewing: not a creature, so no chosen type and no buff.
        projector.project(driver.state).hasSubtype(scooterId, "Pilot") shouldBe false

        val crewer = driver.putCreatureOnBattlefield(you, "Test Bear")
        driver.submitSuccess(CrewVehicle(you, scooterId, listOf(crewer)))
        driver.bothPass() // resolve the crew ability

        val projected = projector.project(driver.state)

        // Crew's Layer 4 animation is applied before the Engine's Layer 4 grant even though it has
        // the later timestamp, because the grant's affected set depends on it.
        projected.isCreature(scooterId) shouldBe true
        projected.hasSubtype(scooterId, "Pilot") shouldBe true
        projected.hasSubtype(scooterId, "Vehicle") shouldBe true

        // Layer 7c then sees a Pilot creature you control: 3/3 base becomes 4/4.
        projected.getPower(scooterId) shouldBe 4
        projected.getToughness(scooterId) shouldBe 4
    }

    test("the crewed Engine is the chosen type but never buffs itself") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1

        val engine = driver.putEngine(you, "Pilot")
        // Crew 3 — one 2/2 isn't enough, so bring two.
        val crewA = driver.putCreatureOnBattlefield(you, "Test Bear")
        val crewB = driver.putCreatureOnBattlefield(you, "Test Bear")
        driver.submitSuccess(CrewVehicle(you, engine, listOf(crewA, crewB)))
        driver.bothPass() // resolve the crew ability

        val projected = projector.project(driver.state)

        projected.isCreature(engine) shouldBe true
        projected.hasSubtype(engine, "Pilot") shouldBe true

        // "other than this Vehicle" — the lord skips the Engine, which stays a base 4/4.
        projected.getPower(engine) shouldBe 4
        projected.getToughness(engine) shouldBe 4
    }
})
