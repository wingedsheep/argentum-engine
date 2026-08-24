package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `ActivatedAbility.xDefinedAs` — an activation cost's `{X}` whose value the ability's own text
 * defines (CR 107.3c) instead of the controller announcing it (CR 107.3a).
 *
 * The engine models it as a *substitution*: the amount is evaluated against the source and folded
 * into the cost's `{X}` symbols before any of the three mana paths (enumeration, validation,
 * payment) reads the cost, so all three necessarily agree on the number. These tests pin the four
 * observable consequences — the offered cost, affordability, the absent X prompt, and the value
 * reaching the effect — plus the interaction with a cost-increasing static.
 *
 * Soul Foundry is the shipped card; the inline artifact below uses "the number of creatures you
 * control" instead of an imprinted card's mana value so the amount is trivial to vary.
 */
class DefinedXAbilityCostTest : FunSpec({

    val creaturesYouControl = DynamicAmount.Count(
        player = Player.You,
        zone = Zone.BATTLEFIELD,
        filter = GameObjectFilter.Creature
    )

    // "{X}: Draw X cards. X is the number of creatures you control."
    val definedXEngine = card("Test Defined X Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{X}: Draw X cards. X is the number of creatures you control."
        activatedAbility {
            cost = Costs.Mana("{X}")
            xDefinedAs = creaturesYouControl
            effect = Effects.DrawCards(DynamicAmount.XValue)
        }
    }

    // "{X}: Draw X cards." — the ordinary, player-announced X, as the control case.
    val chosenXEngine = card("Test Chosen X Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{X}: Draw X cards."
        activatedAbility {
            cost = Costs.Mana("{X}")
            effect = Effects.DrawCards(DynamicAmount.XValue)
        }
    }

    // "Artifacts' activated abilities cost {2} more to activate."
    val taxer = card("Test Ability Taxer") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "Artifacts' activated abilities cost {2} more to activate."
        staticAbility {
            ability = IncreaseActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Artifact),
                amount = DynamicAmount.Fixed(2)
            )
        }
    }

    fun driverInMainPhase(): EnumerationTestDriver {
        val driver = EnumerationTestDriver()
        driver.registerCards(TestCards.all + listOf(definedXEngine, chosenXEngine, taxer))
        driver.game.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun EnumerationTestDriver.abilityAction(sourceId: EntityId) =
        enumerateFor(player1).activatedAbilityActionsFor(sourceId).single()

    /** Colorless mana left unspent — `giveColorlessMana` is the only source these tests use. */
    fun EnumerationTestDriver.manaLeft(playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<ManaPoolComponent>()?.colorless ?: 0

    test("the ability is offered at its resolved price, with no X picker") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Defined X Engine")
        repeat(2) { driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        driver.game.giveColorlessMana(driver.player1, 5)

        val action = driver.abilityAction(engine)

        withClue("two creatures make the printed {X} an ordinary {2}") {
            action.manaCostString shouldBe "{2}"
        }
        withClue("a defined X is not the player's to choose, so it is not an X-picker cost") {
            action.hasXCost shouldBe false
            action.maxAffordableX shouldBe null
        }
    }

    test("the control case still asks for X — this test's card is the only thing that changed") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Chosen X Engine")
        repeat(2) { driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        driver.game.giveColorlessMana(driver.player1, 5)

        val action = driver.abilityAction(engine)

        action.hasXCost shouldBe true
        action.maxAffordableX shouldBe 5
    }

    test("affordability tracks the resolved X, not the printed {X}") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Defined X Engine")
        repeat(3) { driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }

        driver.game.giveColorlessMana(driver.player1, 2)
        withClue("{3} can't be paid out of two mana") {
            driver.abilityAction(engine).affordable shouldBe false
        }

        driver.game.giveColorlessMana(driver.player1, 1)
        withClue("a third mana covers it") {
            driver.abilityAction(engine).affordable shouldBe true
        }
    }

    test("activating pays the resolved X without a prompt, and the effect sees the same X") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Defined X Engine")
        repeat(2) { driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        driver.game.giveColorlessMana(driver.player1, 5)
        val handBefore = driver.game.getHandSize(driver.player1)

        val abilityId = definedXEngine.activatedAbilities.single().id
        driver.game.submit(ActivateAbility(driver.player1, engine, abilityId)).error shouldBe null

        withClue("no ChooseNumber decision: the ability defined X itself") {
            driver.game.state.pendingDecision shouldBe null
        }
        driver.game.bothPass()

        withClue("X reached the effect through EffectContext.xValue (CR 107.3i)") {
            driver.game.getHandSize(driver.player1) shouldBe handBefore + 2
        }
        withClue("exactly {2} was spent of the five available") {
            driver.manaLeft(driver.player1) shouldBe 3
        }
    }

    test("an amount that resolves to nothing is X = 0 — a legal, pointless activation") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Defined X Engine")
        val handBefore = driver.game.getHandSize(driver.player1)

        val action = driver.abilityAction(engine)
        withClue("no creatures, so the cost is {0} and the ability is still offered") {
            action.manaCostString shouldBe "{0}"
            action.affordable shouldBe true
        }

        val abilityId = definedXEngine.activatedAbilities.single().id
        driver.game.submit(ActivateAbility(driver.player1, engine, abilityId)).error shouldBe null
        driver.game.state.pendingDecision shouldBe null
        driver.game.bothPass()

        driver.game.getHandSize(driver.player1) shouldBe handBefore
    }

    test("a cost-increasing static taxes the resolved X, not the {X} symbol") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Defined X Engine")
        driver.game.putPermanentOnBattlefield(driver.player1, "Test Ability Taxer")
        repeat(2) { driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        driver.game.giveColorlessMana(driver.player1, 6)

        val action = driver.abilityAction(engine)
        withClue("X resolves to 2, then the static adds {2}") {
            action.manaCostString shouldBe "{4}"
            action.hasXCost shouldBe false
        }

        val abilityId = definedXEngine.activatedAbilities.single().id
        driver.game.submit(ActivateAbility(driver.player1, engine, abilityId)).error shouldBe null
        driver.game.state.pendingDecision shouldBe null
        driver.game.bothPass()

        withClue("the tax is paid too, but the *effect* still uses the defined X of 2") {
            driver.manaLeft(driver.player1) shouldBe 2
        }
    }

    test("the value is re-read on every activation, so the board decides the price") {
        val driver = driverInMainPhase()
        val engine = driver.game.putPermanentOnBattlefield(driver.player1, "Test Defined X Engine")
        driver.game.giveColorlessMana(driver.player1, 9)

        driver.abilityAction(engine).manaCostString shouldBe "{0}"
        driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.abilityAction(engine).manaCostString shouldBe "{1}"
        driver.game.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.abilityAction(engine).manaCostString shouldBe "{2}"

        withClue("the printed cost never changed — only what the player is charged for it") {
            definedXEngine.activatedAbilities.single().cost.description shouldBe "{X}"
        }
    }
})
