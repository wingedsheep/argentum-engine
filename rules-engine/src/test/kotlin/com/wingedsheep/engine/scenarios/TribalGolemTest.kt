package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Tribal Golem.
 *
 * Tribal Golem: {6}
 * Artifact Creature — Golem
 * 4/4
 * Tribal Golem has trample as long as you control a Beast, haste as long as you
 * control a Goblin, first strike as long as you control a Soldier, flying as long
 * as you control a Wizard, and "{B}: Regenerate Tribal Golem" as long as you
 * control a Zombie.
 */
class TribalGolemTest : FunSpec({

    val TribalGolem = card("Tribal Golem") {
        manaCost = "{6}"
        typeLine = "Artifact Creature — Golem"
        power = 4
        toughness = 4

        staticAbility {
            ability = GrantKeyword(Keyword.TRAMPLE, StaticTarget.SourceCreature)
            condition = Conditions.ControlCreatureOfType(Subtype("Beast"))
        }
        staticAbility {
            ability = GrantKeyword(Keyword.HASTE, StaticTarget.SourceCreature)
            condition = Conditions.ControlCreatureOfType(Subtype("Goblin"))
        }
        staticAbility {
            ability = GrantKeyword(Keyword.FIRST_STRIKE, StaticTarget.SourceCreature)
            condition = Conditions.ControlCreatureOfType(Subtype("Soldier"))
        }
        staticAbility {
            ability = GrantKeyword(Keyword.FLYING, StaticTarget.SourceCreature)
            condition = Conditions.ControlCreatureOfType(Subtype("Wizard"))
        }

        activatedAbility {
            cost = Costs.Mana("{B}")
            effect = RegenerateEffect(EffectTarget.Self)
            restrictions = listOf(
                ActivationRestriction.OnlyIfCondition(Conditions.ControlCreatureOfType(Subtype("Zombie")))
            )
        }
    }

    val regenerateAbilityId = TribalGolem.activatedAbilities.first().id

    val TestWizard = CardDefinition.creature(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TribalGolem, TestWizard))
        return driver
    }

    test("Tribal Golem is 4/4 with no keywords by default") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        val projected = projector.project(driver.state)

        projected.getPower(golem) shouldBe 4
        projected.getToughness(golem) shouldBe 4
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe false
        projected.hasKeyword(golem, Keyword.HASTE) shouldBe false
        projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe false
        projected.hasKeyword(golem, Keyword.FLYING) shouldBe false
    }

    test("gains trample when you control a Beast") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe true
    }

    test("gains haste when you control a Goblin") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.HASTE) shouldBe true
    }

    test("gains first strike when you control a Soldier") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("gains flying when you control a Wizard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.FLYING) shouldBe true
    }

    test("gains multiple keywords with multiple creature types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")
        driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(golem, Keyword.HASTE) shouldBe true
        projected.hasKeyword(golem, Keyword.FLYING) shouldBe true
        projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe false // No Soldier
    }

    test("does not gain keywords from opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(opponent, "Trample Beast")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe false
    }

    test("regenerate ability is available when you control a Zombie") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.removeSummoningSickness(golem)
        driver.putCreatureOnBattlefield(activePlayer, "Gravedigger")

        // Give black mana for regenerate cost
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Should be able to activate regenerate
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = golem,
                abilityId = regenerateAbilityId
            )
        )
        result.isSuccess shouldBe true
    }

    test("regenerate ability is NOT available without a Zombie") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.removeSummoningSickness(golem)

        // Give black mana
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Should NOT be able to activate regenerate (no Zombie)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = golem,
                abilityId = regenerateAbilityId
            )
        )
        result.isSuccess shouldBe false
    }
})
