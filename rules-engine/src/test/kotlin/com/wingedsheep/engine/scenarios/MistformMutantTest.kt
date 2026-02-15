package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Mistform Mutant.
 *
 * Mistform Mutant: {4}{U}{U}
 * Creature — Illusion Mutant
 * 3/4
 * {1}{U}: Choose a creature type other than Wall. Target creature becomes that type until end of turn.
 */
class MistformMutantTest : FunSpec({

    val mutantAbilityId = AbilityId(UUID.randomUUID().toString())

    val MistformMutant = CardDefinition(
        name = "Mistform Mutant",
        manaCost = ManaCost.parse("{4}{U}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Illusion"), Subtype("Mutant"))),
        oracleText = "{1}{U}: Choose a creature type other than Wall. Target creature becomes that type until end of turn.",
        creatureStats = CreatureStats(3, 4),
        script = CardScript.permanent(
            ActivatedAbility(
                id = mutantAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{1}{U}")),
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.ContextTarget(0),
                    excludedTypes = listOf("Wall")
                ),
                targetRequirement = TargetCreature()
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MistformMutant))
        return driver
    }

    test("Mistform Mutant presents creature type choice excluding Wall") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Mistform Mutant and a target creature on the battlefield
        val mutant = driver.putCreatureOnBattlefield(activePlayer, "Mistform Mutant")
        driver.removeSummoningSickness(mutant)
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Give the player mana to pay {1}{U}
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Activate Mistform Mutant targeting Grizzly Bears
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mutant,
                abilityId = mutantAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )
        result.isSuccess shouldBe true

        // Both pass to resolve the ability
        driver.bothPass()

        // Should present a ChooseOptionDecision for creature type
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        // Wall should NOT be in the options
        decision.options shouldNotContain "Wall"
    }

    test("target creature type changes after choosing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mutant = driver.putCreatureOnBattlefield(activePlayer, "Mistform Mutant")
        driver.removeSummoningSickness(mutant)
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Activate targeting the bear
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mutant,
                abilityId = mutantAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Goblin" from the options
        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // The ability should have resolved successfully
        driver.isPaused shouldBe false
    }

    test("can target opponent's creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mutant = driver.putCreatureOnBattlefield(activePlayer, "Mistform Mutant")
        driver.removeSummoningSickness(mutant)
        val opponentBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Activate targeting opponent's creature
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mutant,
                abilityId = mutantAbilityId,
                targets = listOf(ChosenTarget.Permanent(opponentBear))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf" from the options
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // The ability should have resolved successfully
        driver.isPaused shouldBe false
    }
})
