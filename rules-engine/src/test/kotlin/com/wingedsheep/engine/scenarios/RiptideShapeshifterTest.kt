package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.RevealUntilCreatureTypeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

/**
 * Tests for Riptide Shapeshifter.
 *
 * Riptide Shapeshifter ({3}{U}{U}, Creature — Shapeshifter, 3/3):
 * {2}{U}{U}, Sacrifice Riptide Shapeshifter: Choose a creature type. Reveal cards from
 * the top of your library until you reveal a creature card of that type. Put that card
 * onto the battlefield and shuffle the rest into your library.
 */
class RiptideShapeshifterTest : FunSpec({

    val abilityId = AbilityId(UUID.randomUUID().toString())

    val RiptideShapeshifter = CardDefinition.creature(
        name = "Riptide Shapeshifter",
        manaCost = ManaCost.parse("{3}{U}{U}"),
        oracleText = "{2}{U}{U}, Sacrifice Riptide Shapeshifter: Choose a creature type. Reveal cards from the top of your library until you reveal a creature card of that type. Put that card onto the battlefield and shuffle the rest into your library.",
        power = 3,
        toughness = 3,
        subtypes = setOf(Subtype("Shapeshifter")),
        script = CardScript.permanent(
            ActivatedAbility(
                id = abilityId,
                cost = AbilityCost.Composite(listOf(
                    AbilityCost.Mana(ManaCost.parse("{2}{U}{U}")),
                    AbilityCost.SacrificeSelf
                )),
                effect = RevealUntilCreatureTypeEffect
            )
        )
    )

    val SilverKnight = CardDefinition.creature(
        name = "Silver Knight",
        manaCost = ManaCost.parse("{W}{W}"),
        oracleText = "",
        power = 2,
        toughness = 2,
        subtypes = setOf(Subtype("Human"), Subtype("Knight"))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RiptideShapeshifter)
        driver.registerCard(SilverKnight)
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            skipMulligans = true
        )
        return driver
    }

    test("finds creature of chosen type and puts it onto the battlefield") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Riptide Shapeshifter on battlefield
        val shapeshifter = driver.putCreatureOnBattlefield(activePlayer, "Riptide Shapeshifter")
        driver.removeSummoningSickness(shapeshifter)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Put a Knight in the library (not on top, some other cards first)
        // Library currently has Grizzly Bears. Place a Knight somewhere in it.
        val knight = driver.putCardOnTopOfLibrary(activePlayer, "Silver Knight")

        // Activate the ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = shapeshifter,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        // Shapeshifter should be sacrificed
        driver.state.getBattlefield().contains(shapeshifter) shouldBe false

        // Resolve the ability
        driver.bothPass()

        // Choose "Knight" creature type
        val decision = driver.pendingDecision as ChooseOptionDecision
        val knightIndex = decision.options.indexOf("Knight")
        knightIndex shouldNotBe -1
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, knightIndex))

        // Silver Knight should be on the battlefield
        val battlefieldZone = ZoneKey(activePlayer, Zone.BATTLEFIELD)
        val battlefield = driver.state.getZone(battlefieldZone)
        val knightOnBattlefield = battlefield.any { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Silver Knight"
        }
        knightOnBattlefield shouldBe true

        // Silver Knight should have summoning sickness
        driver.state.getEntity(knight)?.has<SummoningSicknessComponent>() shouldBe true
    }

    test("reveals past non-matching cards to find the right type") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val shapeshifter = driver.putCreatureOnBattlefield(activePlayer, "Riptide Shapeshifter")
        driver.removeSummoningSickness(shapeshifter)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Put Silver Knight deeper: put some non-Knight cards on top first
        val knight = driver.putCardOnTopOfLibrary(activePlayer, "Silver Knight")
        // Now put non-Knight creatures on top of the Knight
        driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = shapeshifter,
                abilityId = abilityId
            )
        )

        driver.bothPass()

        // Choose "Knight" creature type
        val decision = driver.pendingDecision as ChooseOptionDecision
        val knightIndex = decision.options.indexOf("Knight")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, knightIndex))

        // Silver Knight should be on the battlefield
        val battlefieldZone = ZoneKey(activePlayer, Zone.BATTLEFIELD)
        val battlefield = driver.state.getZone(battlefieldZone)
        val knightOnBattlefield = battlefield.any { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Silver Knight"
        }
        knightOnBattlefield shouldBe true

        // Knight should not be in library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.contains(knight) shouldBe false
    }

    test("no creature of chosen type - shuffles library and nothing enters battlefield") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val shapeshifter = driver.putCreatureOnBattlefield(activePlayer, "Riptide Shapeshifter")
        driver.removeSummoningSickness(shapeshifter)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Library has only Grizzly Bears (type: Bear) - no Knights

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = shapeshifter,
                abilityId = abilityId
            )
        )

        // Shapeshifter is sacrificed as cost - count battlefield after sacrifice
        val battlefieldAfterSacrifice = driver.state.getZone(ZoneKey(activePlayer, Zone.BATTLEFIELD)).size

        driver.bothPass()

        // Choose "Knight" - a type not present in the library
        val decision = driver.pendingDecision as ChooseOptionDecision
        val knightIndex = decision.options.indexOf("Knight")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, knightIndex))

        // No new creatures on battlefield (nothing of type Knight was found)
        val battlefieldAfter = driver.state.getZone(ZoneKey(activePlayer, Zone.BATTLEFIELD)).size
        battlefieldAfter shouldBe battlefieldAfterSacrifice
    }

    test("empty library - nothing happens") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val shapeshifter = driver.putCreatureOnBattlefield(activePlayer, "Riptide Shapeshifter")
        driver.removeSummoningSickness(shapeshifter)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Empty the library
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        driver.replaceState(driver.state.copy(
            zones = driver.state.zones + (libraryZone to emptyList())
        ))

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = shapeshifter,
                abilityId = abilityId
            )
        )

        driver.bothPass()

        // With empty library, ability resolves without asking for creature type
        // No pending decision should remain (it auto-completes)
        driver.pendingDecision shouldBe null
    }
})
