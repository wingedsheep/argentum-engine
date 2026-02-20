package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

/**
 * Tests for Callous Oppressor.
 *
 * Callous Oppressor: {1}{U}{U}
 * Creature — Cephalid
 * 1/2
 * You may choose not to untap Callous Oppressor during your untap step.
 * As Callous Oppressor enters the battlefield, an opponent chooses a creature type.
 * {T}: Gain control of target creature that isn't of the chosen type for as long as
 * Callous Oppressor remains tapped.
 */
class CallousOppressorTest : FunSpec({

    val oppressorAbilityId = AbilityId(UUID.randomUUID().toString())

    val CallousOppressor = CardDefinition.creature(
        name = "Callous Oppressor",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype("Cephalid")),
        power = 1,
        toughness = 2,
        keywords = setOf(Keyword.MAY_NOT_UNTAP),
        oracleText = "You may choose not to untap Callous Oppressor during your untap step.\nAs Callous Oppressor enters the battlefield, an opponent chooses a creature type.\n{T}: Gain control of target creature that isn't of the chosen type for as long as Callous Oppressor remains tapped.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = oppressorAbilityId,
                cost = AbilityCost.Tap,
                effect = GainControlEffect(EffectTarget.ContextTarget(0), Duration.WhileSourceTapped()),
                targetRequirement = TargetCreature(
                    filter = TargetFilter(GameObjectFilter.Creature.notOfSourceChosenType())
                )
            ),
            replacementEffects = listOf(EntersWithCreatureTypeChoice(opponentChooses = true))
        )
    )

    // Simple creatures for testing
    val GoblinPiker = CardDefinition.creature(
        name = "Goblin Piker",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Warrior")),
        power = 2,
        toughness = 1,
        oracleText = ""
    )

    val ElvishWarrior = CardDefinition.creature(
        name = "Elvish Warrior",
        manaCost = ManaCost.parse("{G}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 2,
        toughness = 3,
        oracleText = ""
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CallousOppressor, GoblinPiker, ElvishWarrior))
        return driver
    }

    test("Opponent chooses creature type on ETB") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana and cast Callous Oppressor from hand
        val cardId = driver.putCardInHand(activePlayer, "Callous Oppressor")
        driver.giveMana(activePlayer, Color.BLUE, 3)
        driver.castSpell(activePlayer, cardId)

        // Resolve the spell
        driver.bothPass()

        // The creature type choice decision should be pending for the opponent
        val decision = driver.pendingDecision as ChooseOptionDecision
        decision.playerId shouldBe opponent

        // Opponent chooses "Goblin"
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(opponent, OptionChosenResponse(decision.id, goblinIndex))

        // Verify the chosen creature type is stored on the permanent
        val oppressor = driver.state.getBattlefield().find { entityId ->
            driver.state.getEntity(entityId)?.get<ChosenCreatureTypeComponent>() != null
        }
        oppressor shouldNotBe null
        driver.state.getEntity(oppressor!!)?.get<ChosenCreatureTypeComponent>()?.creatureType shouldBe "Goblin"
    }

    test("Tap to steal creature not of chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Callous Oppressor on the battlefield with chosen type "Goblin"
        val oppressor = driver.putCreatureOnBattlefield(activePlayer, "Callous Oppressor")
        driver.removeSummoningSickness(oppressor)
        driver.replaceState(driver.state.updateEntity(oppressor) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Put an Elf (not a Goblin) on the opponent's battlefield
        val elf = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")

        // Activate the oppressor's ability targeting the Elf
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oppressor,
                abilityId = oppressorAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Elf should now be controlled by the active player
        val projected = projector.project(driver.state)
        projected.getController(elf) shouldBe activePlayer
    }

    test("Cannot target creature of chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Callous Oppressor on the battlefield with chosen type "Goblin"
        val oppressor = driver.putCreatureOnBattlefield(activePlayer, "Callous Oppressor")
        driver.removeSummoningSickness(oppressor)
        driver.replaceState(driver.state.updateEntity(oppressor) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Put a Goblin on the opponent's battlefield
        val goblin = driver.putCreatureOnBattlefield(opponent, "Goblin Piker")

        // Attempting to activate targeting the Goblin should fail validation
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oppressor,
                abilityId = oppressorAbilityId,
                targets = listOf(ChosenTarget.Permanent(goblin))
            )
        )
        result.isSuccess shouldBe false
    }

    test("Control persists while Callous Oppressor remains tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Callous Oppressor on the battlefield with chosen type "Goblin"
        val oppressor = driver.putCreatureOnBattlefield(activePlayer, "Callous Oppressor")
        driver.removeSummoningSickness(oppressor)
        driver.replaceState(driver.state.updateEntity(oppressor) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Put an Elf on the opponent's battlefield
        val elf = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oppressor,
                abilityId = oppressorAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Control should be ours
        var projected = projector.project(driver.state)
        projected.getController(elf) shouldBe activePlayer

        // Pass to next untap step and choose to keep oppressor tapped
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, listOf(oppressor))

        // Oppressor should still be tapped
        driver.state.getEntity(oppressor)?.has<TappedComponent>() shouldBe true

        // Control should still be ours
        projected = projector.project(driver.state)
        projected.getController(elf) shouldBe activePlayer
    }

    test("Control returns when Callous Oppressor is untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Callous Oppressor on the battlefield with chosen type "Goblin"
        val oppressor = driver.putCreatureOnBattlefield(activePlayer, "Callous Oppressor")
        driver.removeSummoningSickness(oppressor)
        driver.replaceState(driver.state.updateEntity(oppressor) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Put an Elf on the opponent's battlefield
        val elf = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = oppressor,
                abilityId = oppressorAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Control should be ours
        var projected = projector.project(driver.state)
        projected.getController(elf) shouldBe activePlayer

        // Pass to next untap step and choose to untap oppressor (select nothing)
        driver.passPriorityUntil(Step.UNTAP)
        driver.submitCardSelection(activePlayer, emptyList())

        // Oppressor should now be untapped
        driver.state.getEntity(oppressor)?.has<TappedComponent>() shouldBe false

        // Control should return to the opponent
        projected = projector.project(driver.state)
        projected.getController(elf) shouldBe opponent
    }

    test("MAY_NOT_UNTAP offers choice during untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put tapped Callous Oppressor on the battlefield
        val oppressor = driver.putCreatureOnBattlefield(activePlayer, "Callous Oppressor")
        driver.removeSummoningSickness(oppressor)
        driver.replaceState(driver.state.updateEntity(oppressor) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
                .with(TappedComponent)
        })

        // Pass to untap step
        driver.passPriorityUntil(Step.UNTAP)

        // MAY_NOT_UNTAP decision should be pending
        val decision = driver.pendingDecision as SelectCardsDecision

        // Choose to keep it tapped
        driver.submitCardSelection(activePlayer, listOf(oppressor))
        driver.state.getEntity(oppressor)?.has<TappedComponent>() shouldBe true
    }
})
