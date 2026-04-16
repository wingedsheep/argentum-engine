package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.StormCopyEffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.BrigidsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests G1 / G2 from [`backlog/modal-cast-time-choices-plan.md`]: rule 700.2g —
 * copies of a choose-N modal spell must retain the original's chosen modes.
 * The copy controller may (per paper-Magic) re-choose targets, but modes stay
 * locked. Current implementation inherits both modes *and* per-mode targets,
 * which is a conservative subset of 700.2g that matches the engine's comment
 * in [StormCopyEffectExecutor.executePreChosen...].
 *
 * - G1: [com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellExecutor]
 *   (e.g., Mischievous Quanar / Twincast style) must populate the
 *   [TriggeredAbilityOnStackComponent]'s `chosenModes` / `modeTargetsOrdered`
 *   from the source spell on the stack.
 * - G2: [StormCopyEffectExecutor] must do the same when creating Storm copies.
 *
 * G1 is an end-to-end integration test via a synthetic "Copy Target Spell"
 * instant. G2 is a direct executor-level unit test because Storm's full flow
 * (cast-count trigger) requires significant additional scaffolding that
 * doesn't make the propagation logic more trustworthy to exercise.
 */
class ModalCopyPreservationTest : FunSpec({

    val TestCopySpell = CardDefinition(
        name = "Test Copy Spell",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.instant(),
        oracleText = "Copy target instant or sorcery spell.",
        script = CardScript.spell(
            effect = Effects.CopyTargetSpell(),
            Targets.InstantOrSorcerySpell
        )
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(BrigidsCommand)
        d.registerCard(TestCopySpell)
        return d
    }

    fun nameOf(d: GameTestDriver, id: EntityId): String? =
        d.state.getEntity(id)?.get<CardComponent>()?.name

    test("G1 — CopyTargetSpell inherits chosenModes and modeTargetsOrdered onto the triggered ability copy") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        // P1: cast Brigid's Command ({1}{G}{W}) with pre-chosen modes [2, 3].
        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")
        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(centaur)),
                    listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
                )
            )
        ).isSuccess shouldBe true

        val brigidsOnStackId = d.state.stack.first()

        // P1 passes priority to P2.
        d.passPriority(p1)

        // P2: cast Test Copy Spell ({U}) targeting Brigid's Command.
        d.giveMana(p2, Color.BLUE, 1)
        val copySpell = d.putCardInHand(p2, "Test Copy Spell")
        d.submit(
            CastSpell(
                playerId = p2,
                cardId = copySpell,
                targets = listOf(ChosenTarget.Spell(brigidsOnStackId))
            )
        ).isSuccess shouldBe true

        // Priority passes to P1. Pass twice so Copy Spell resolves first (LIFO).
        d.bothPass()

        // After Copy Spell resolves, a TriggeredAbilityOnStackComponent is pushed onto
        // the stack above Brigid's Command with the same chosenModes/modeTargetsOrdered.
        val copyTriggerId = d.state.stack.last { id ->
            d.state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>() != null
        }
        val copyTrigger = d.state.getEntity(copyTriggerId)?.get<TriggeredAbilityOnStackComponent>()
        copyTrigger.shouldNotBeNull()
        copyTrigger.chosenModes shouldBe listOf(2, 3)
        copyTrigger.modeTargetsOrdered shouldHaveSize 2
        copyTrigger.modeTargetsOrdered[0] shouldBe listOf(ChosenTarget.Permanent(centaur))
        copyTrigger.modeTargetsOrdered[1] shouldBe listOf(
            ChosenTarget.Permanent(centaur),
            ChosenTarget.Permanent(goblin)
        )
    }

    test("G2 — StormCopyEffectExecutor inherits chosenModes/modeTargetsOrdered from the source spell") {
        // Unit-level test: fabricate a minimal state with a modal SpellOnStackComponent
        // already populated, invoke the executor, assert the resulting copy carries the
        // same per-mode data. Bypasses the full Storm trigger flow because the propagation
        // logic (not the trigger) is the subject under test.
        val p1 = EntityId.generate()
        val spellEntity = EntityId.generate()
        val centaur = EntityId.generate()
        val goblin = EntityId.generate()

        // SpellOnStackComponent carrying the pre-chosen modes/targets, as if the modal
        // spell had been cast through the normal cast-time flow.
        val spellComponent = SpellOnStackComponent(
            casterId = p1,
            chosenModes = listOf(2, 3),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Permanent(centaur)),
                listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
            )
        )
        val spellCardComponent = CardComponent(
            cardDefinitionId = "Brigid's Command",
            name = "Brigid's Command",
            manaCost = ManaCost.parse("{1}{G}{W}"),
            typeLine = TypeLine.sorcery(),
            oracleText = "",
            ownerId = p1,
            spellEffect = null
        )

        val state = GameState(
            activePlayerId = p1,
            priorityPlayerId = p1,
            turnOrder = listOf(p1)
        )
            .withEntity(spellEntity, ComponentContainer.of(
                spellCardComponent,
                OwnerComponent(p1),
                ControllerComponent(p1),
                spellComponent
            ))
            .copy(stack = listOf(spellEntity))

        // Storm effect creating a single copy. Under executor rules, `spellTargetRequirements`
        // is ignored when the source has chosenModes (modal source path).
        val stormEffect = StormCopyEffect(
            copyCount = 1,
            spellEffect = com.wingedsheep.sdk.scripting.effects.DrawCardsEffect(
                com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(1),
                com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller
            ),
            spellName = "Brigid's Command"
        )
        val context = EffectContext(
            sourceId = spellEntity,
            controllerId = p1,
            opponentId = null
        )

        val executor = StormCopyEffectExecutor(cardRegistry = CardRegistry(), targetFinder = TargetFinder())
        val result = executor.execute(state, stormEffect, context)
        result.isSuccess shouldBe true

        // Per 707.12, the copy is a spell on the stack — a SpellOnStackComponent-backed
        // entity with a CopyOfComponent marker, not a triggered ability.
        val copyEntityId = result.state.stack.single { id ->
            val container = result.state.getEntity(id)
            container?.get<SpellOnStackComponent>() != null && container.has<CopyOfComponent>()
        }
        val copy = result.state.getEntity(copyEntityId)!!.get<SpellOnStackComponent>()!!

        copy.chosenModes shouldBe listOf(2, 3)
        copy.modeTargetsOrdered shouldHaveSize 2
        copy.modeTargetsOrdered[0] shouldBe listOf(ChosenTarget.Permanent(centaur))
        copy.modeTargetsOrdered[1] shouldBe listOf(
            ChosenTarget.Permanent(centaur),
            ChosenTarget.Permanent(goblin)
        )
    }
})
