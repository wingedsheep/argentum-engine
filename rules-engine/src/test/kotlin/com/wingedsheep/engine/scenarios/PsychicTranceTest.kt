package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.CounterSpellEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityToGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Psychic Trance (ONS).
 *
 * {2}{U}{U} Instant
 * Until end of turn, Wizards you control gain "{T}: Counter target spell."
 */
class PsychicTranceTest : FunSpec({

    val PsychicTrance = CardDefinition.instant(
        name = "Psychic Trance",
        manaCost = ManaCost.parse("{2}{U}{U}"),
        oracleText = "Until end of turn, Wizards you control gain \"{T}: Counter target spell.\"",
        script = CardScript.spell(
            effect = GrantActivatedAbilityToGroupEffect(
                ability = ActivatedAbility(
                    id = AbilityId.generate(),
                    cost = AbilityCost.Tap,
                    effect = CounterSpellEffect,
                    targetRequirement = Targets.Spell
                ),
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Wizard").youControl()),
                duration = Duration.EndOfTurn
            )
        )
    )

    val TestWizard = CardDefinition(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
        creatureStats = CreatureStats(1, 1),
        script = CardScript.permanent()
    )

    val TestWarrior = CardDefinition(
        name = "Test Warrior",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Warrior"))),
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent()
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PsychicTrance, TestWizard, TestWarrior))
        return driver
    }

    test("grants counter ability to Wizards you control") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Wizard on the battlefield
        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")
        driver.removeSummoningSickness(wizard)

        // Cast Psychic Trance
        val trance = driver.putCardInHand(activePlayer, "Psychic Trance")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, trance)
        driver.bothPass()

        // Wizard should have a granted activated ability
        val grants = driver.state.grantedActivatedAbilities.filter { it.entityId == wizard }
        grants.size shouldBe 1

        // Opponent casts a spell
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(activePlayer))

        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        // Activate the granted counter ability on the Wizard
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard,
                abilityId = grants[0].ability.id,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )

        // Resolve the counter ability
        driver.bothPass()

        // Lightning Bolt should be countered
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
    }

    test("does not grant counter ability to non-Wizards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Warrior (non-Wizard) on the battlefield
        val warrior = driver.putCreatureOnBattlefield(activePlayer, "Test Warrior")
        driver.removeSummoningSickness(warrior)

        // Cast Psychic Trance
        val trance = driver.putCardInHand(activePlayer, "Psychic Trance")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, trance)
        driver.bothPass()

        // Warrior should NOT have a granted activated ability
        val grants = driver.state.grantedActivatedAbilities.filter { it.entityId == warrior }
        grants.size shouldBe 0
    }

    test("does not grant counter ability to opponent's Wizards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Wizards on both sides
        val myWizard = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")
        val theirWizard = driver.putCreatureOnBattlefield(opponent, "Test Wizard")

        // Cast Psychic Trance
        val trance = driver.putCardInHand(activePlayer, "Psychic Trance")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, trance)
        driver.bothPass()

        // My Wizard should have the granted ability
        val myGrants = driver.state.grantedActivatedAbilities.filter { it.entityId == myWizard }
        myGrants.size shouldBe 1

        // Opponent's Wizard should NOT have the granted ability
        val theirGrants = driver.state.grantedActivatedAbilities.filter { it.entityId == theirWizard }
        theirGrants.size shouldBe 0
    }

    test("multiple Wizards can each counter a spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two Wizards on the battlefield
        val wizard1 = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")
        val wizard2 = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")
        driver.removeSummoningSickness(wizard1)
        driver.removeSummoningSickness(wizard2)

        // Cast Psychic Trance
        val trance = driver.putCardInHand(activePlayer, "Psychic Trance")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, trance)
        driver.bothPass()

        // Both Wizards should have the granted ability
        val grants1 = driver.state.grantedActivatedAbilities.filter { it.entityId == wizard1 }
        val grants2 = driver.state.grantedActivatedAbilities.filter { it.entityId == wizard2 }
        grants1.size shouldBe 1
        grants2.size shouldBe 1

        // Opponent casts first spell
        val bolt1 = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt1, listOf(activePlayer))
        val spell1 = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        // Counter with first Wizard
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard1,
                abilityId = grants1[0].ability.id,
                targets = listOf(ChosenTarget.Spell(spell1))
            )
        )
        driver.bothPass()

        // First spell countered
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"

        // Opponent casts second spell
        val bolt2 = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt2, listOf(activePlayer))
        val spell2 = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        // Counter with second Wizard
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard2,
                abilityId = grants2[0].ability.id,
                targets = listOf(ChosenTarget.Spell(spell2))
            )
        )
        driver.bothPass()

        // Both spells should be in graveyard (countered)
        driver.getGraveyardCardNames(opponent).count { it == "Lightning Bolt" } shouldBe 2
    }

    test("Wizard taps when using the granted counter ability") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")
        driver.removeSummoningSickness(wizard)

        // Cast Psychic Trance
        val trance = driver.putCardInHand(activePlayer, "Psychic Trance")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, trance)
        driver.bothPass()

        val grants = driver.state.grantedActivatedAbilities.filter { it.entityId == wizard }

        // Opponent casts a spell
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(activePlayer))
        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        // Activate counter ability
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizard,
                abilityId = grants[0].ability.id,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )

        // Wizard should be tapped
        driver.isTapped(wizard) shouldBe true
    }
})
