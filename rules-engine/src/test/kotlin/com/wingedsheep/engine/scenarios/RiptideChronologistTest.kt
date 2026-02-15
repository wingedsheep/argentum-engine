package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeUntapEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Riptide Chronologist.
 *
 * Riptide Chronologist ({3}{U}{U}, Creature — Human Wizard, 1/3):
 * {U}, Sacrifice Riptide Chronologist: Untap all creatures of the creature type of your choice.
 */
class RiptideChronologistTest : FunSpec({

    val abilityId = AbilityId(UUID.randomUUID().toString())

    val ElvishWarrior = CardDefinition.creature(
        name = "Elvish Warrior",
        manaCost = ManaCost.parse("{G}{G}"),
        oracleText = "",
        power = 2,
        toughness = 3,
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior"))
    )

    val RiptideChronologist = CardDefinition.creature(
        name = "Riptide Chronologist",
        manaCost = ManaCost.parse("{3}{U}{U}"),
        oracleText = "{U}, Sacrifice Riptide Chronologist: Untap all creatures of the creature type of your choice.",
        power = 1,
        toughness = 3,
        subtypes = setOf(Subtype("Human"), Subtype("Wizard")),
        script = CardScript.permanent(
            ActivatedAbility(
                id = abilityId,
                cost = AbilityCost.Composite(listOf(
                    AbilityCost.Mana(ManaCost.parse("{U}")),
                    AbilityCost.SacrificeSelf
                )),
                effect = ChooseCreatureTypeUntapEffect
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RiptideChronologist)
        driver.registerCard(ElvishWarrior)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20,
                "Grizzly Bears" to 10,
                "Elvish Warrior" to 10
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Riptide Chronologist ability untaps all creatures of chosen type") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Riptide Chronologist on battlefield
        val chronologist = driver.putCreatureOnBattlefield(activePlayer, "Riptide Chronologist")
        driver.removeSummoningSickness(chronologist)
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Put some tapped Elves on the battlefield
        val elf1 = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(elf1)
        driver.tapPermanent(elf1)

        val elf2 = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")
        driver.removeSummoningSickness(elf2)
        driver.tapPermanent(elf2)

        // Put a tapped non-Elf creature (should not be affected)
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(bear)
        driver.tapPermanent(bear)

        // Verify all are tapped
        driver.state.getEntity(elf1)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(elf2)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(bear)?.has<TappedComponent>() shouldBe true

        // Activate the ability (sacrifices Riptide Chronologist)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = chronologist,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf" creature type
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Verify Elves are untapped (both yours and opponent's)
        driver.state.getEntity(elf1)?.has<TappedComponent>() shouldBe false
        driver.state.getEntity(elf2)?.has<TappedComponent>() shouldBe false

        // Verify non-Elf creature is still tapped
        driver.state.getEntity(bear)?.has<TappedComponent>() shouldBe true
    }

    test("Riptide Chronologist is sacrificed as part of cost") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chronologist = driver.putCreatureOnBattlefield(activePlayer, "Riptide Chronologist")
        driver.removeSummoningSickness(chronologist)
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = chronologist,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        // Chronologist should be sacrificed (no longer on battlefield)
        driver.state.getBattlefield().contains(chronologist) shouldBe false
    }

    test("Already untapped creatures are not affected") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chronologist = driver.putCreatureOnBattlefield(activePlayer, "Riptide Chronologist")
        driver.removeSummoningSickness(chronologist)
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Put an untapped Elf
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(elf)
        // Don't tap it

        driver.state.getEntity(elf)?.has<TappedComponent>() shouldBe false

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = chronologist,
                abilityId = abilityId
            )
        )

        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Elf should still be untapped
        driver.state.getEntity(elf)?.has<TappedComponent>() shouldBe false
    }
})
