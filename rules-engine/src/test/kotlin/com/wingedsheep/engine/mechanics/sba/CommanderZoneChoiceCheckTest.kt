package com.wingedsheep.engine.mechanics.sba

import com.wingedsheep.engine.core.CommanderZoneChoiceContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.sba.permanent.CommanderZoneChoiceCheck
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderZoneChoiceAskedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 903.9a state-based action — when the format is Commander (and the
 * `alwaysDivertToCommand` shortcut is off), seeing a commander outside the command zone must
 * pause for the owner's "put it in the command zone?" decision. The marker component prevents
 * re-prompting on subsequent SBA iterations until the commander changes zones.
 */
class CommanderZoneChoiceCheckTest : FunSpec({

    val ownerId = EntityId.generate()
    val cmdrId = EntityId.generate()

    fun commanderCard(name: String = "Test Commander"): ComponentContainer =
        ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost.parse("{2}{R}"),
                typeLine = TypeLine(
                    supertypes = setOf(Supertype.LEGENDARY),
                    cardTypes = setOf(CardType.CREATURE),
                    subtypes = setOf(Subtype("Human")),
                ),
                oracleText = "",
                baseStats = CreatureStats(2, 2),
                colors = setOf(Color.RED),
                ownerId = ownerId,
                spellEffect = null,
            ),
            OwnerComponent(ownerId),
            CommanderComponent(ownerId = ownerId),
        )

    fun stateWithCommanderIn(zone: Zone, format: Format = Format.Commander()): GameState {
        return GameState(format = format)
            .withEntity(ownerId, ComponentContainer.EMPTY)
            .withEntity(cmdrId, commanderCard())
            .addToZone(ZoneKey(ownerId, zone), cmdrId)
            .copy(turnOrder = listOf(ownerId))
    }

    val check = CommanderZoneChoiceCheck(DecisionHandler())

    test("pauses with a YesNoDecision when a commander is in the graveyard") {
        val state = stateWithCommanderIn(Zone.GRAVEYARD)
        val result = check.check(state)

        result.isPaused shouldBe true
        val decision = result.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe ownerId

        val frame = result.state.continuationStack.last()
        frame.shouldBeInstanceOf<CommanderZoneChoiceContinuation>()
        frame.commanderId shouldBe cmdrId
        frame.ownerId shouldBe ownerId
        frame.currentZone shouldBe Zone.GRAVEYARD
    }

    test("pauses for a commander in exile, hand, or library too") {
        for (zone in listOf(Zone.EXILE, Zone.HAND, Zone.LIBRARY)) {
            val result = check.check(stateWithCommanderIn(zone))
            result.isPaused shouldBe true
            (result.state.continuationStack.last() as CommanderZoneChoiceContinuation)
                .currentZone shouldBe zone
        }
    }

    test("does not pause when the commander is in the command zone") {
        val result = check.check(stateWithCommanderIn(Zone.COMMAND))
        result.isPaused shouldBe false
    }

    test("does not pause when the commander is on the battlefield") {
        val result = check.check(stateWithCommanderIn(Zone.BATTLEFIELD))
        result.isPaused shouldBe false
    }

    test("does not pause when the asked marker is already attached") {
        val state = stateWithCommanderIn(Zone.GRAVEYARD)
            .updateEntity(cmdrId) { c -> c.with(CommanderZoneChoiceAskedComponent) }
        val result = check.check(state)
        result.isPaused shouldBe false
    }

    test("does not pause when alwaysDivertToCommand is enabled") {
        // The synchronous replacement-time redirect handles diversion; the SBA must stay out
        // of the way so it doesn't double-prompt for shortcut tooling / AI.
        val state = stateWithCommanderIn(
            Zone.GRAVEYARD,
            format = Format.Commander(alwaysDivertToCommand = true),
        )
        val result = check.check(state)
        result.isPaused shouldBe false
    }

    test("does not pause when the format is not Commander") {
        val state = stateWithCommanderIn(Zone.GRAVEYARD, format = Format.Standard)
        val result = check.check(state)
        result.isPaused shouldBe false
    }

    test("Team vs Team prompts when Commander rules are enabled") {
        val format = Format.TeamVsTeam(
            startingLife = 40,
            commanderDamageThreshold = 21,
            deckSize = 100,
        )
        val result = check.check(stateWithCommanderIn(Zone.GRAVEYARD, format))

        result.isPaused shouldBe true
        result.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    test("prompt text names both the commander and the source zone") {
        val state = stateWithCommanderIn(Zone.EXILE)
        val result = check.check(state)
        val decision = result.pendingDecision as YesNoDecision
        decision.prompt shouldBe "Put Test Commander into the command zone instead of leaving it in exile?"
    }

    test("multiple eligible commanders only produce one prompt per SBA pass") {
        // Two commanders for the same player, both in the graveyard. The check returns after
        // the first paused result; the SBA loop in StateBasedActionChecker re-enters after the
        // decision resolves and will pick up the second commander on a later iteration.
        val cmdrB = EntityId.generate()
        val state = stateWithCommanderIn(Zone.GRAVEYARD)
            .withEntity(cmdrB, commanderCard("Other Commander"))
            .addToZone(ZoneKey(ownerId, Zone.GRAVEYARD), cmdrB)

        val result = check.check(state)
        result.isPaused shouldBe true
        result.state.continuationStack.size shouldBe 1
    }

    test("non-owner commander not asked about by this player's turn slot") {
        // A second player exists but their commander is in their own graveyard.
        // The check still pauses for the right player — the owner of the commander,
        // not the active turn player.
        val otherPlayer = EntityId.generate()
        val state = GameState(format = Format.Commander())
            .withEntity(otherPlayer, ComponentContainer.EMPTY)
            .withEntity(ownerId, ComponentContainer.EMPTY)
            .withEntity(cmdrId, commanderCard())
            .addToZone(ZoneKey(ownerId, Zone.GRAVEYARD), cmdrId)
            .copy(turnOrder = listOf(otherPlayer, ownerId))

        val result = check.check(state)
        result.isPaused shouldBe true
        (result.pendingDecision as YesNoDecision).playerId shouldBe ownerId
    }

    test("commander with the asked marker is invisible to the SBA — no events, no pause") {
        // Once the owner has declined, the check should produce zero side effects on every
        // subsequent pass until the commander changes zones.
        val state = stateWithCommanderIn(Zone.EXILE)
            .updateEntity(cmdrId) { c -> c.with(CommanderZoneChoiceAskedComponent) }
        val result = check.check(state)
        result.isPaused shouldBe false
        result.events shouldBe emptyList()
        result.newState.getEntity(cmdrId)!!.has<CommanderZoneChoiceAskedComponent>() shouldBe true
    }

    test("a non-commander legendary with the same name does not trigger the prompt") {
        // The SBA must key off CommanderComponent, not legendary supertype or name.
        val nonCommander = EntityId.generate()
        val nonCommanderContainer = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "Other Legendary",
                name = "Other Legendary",
                manaCost = ManaCost.parse("{1}{G}"),
                typeLine = TypeLine(
                    supertypes = setOf(Supertype.LEGENDARY),
                    cardTypes = setOf(CardType.CREATURE),
                    subtypes = setOf(Subtype("Elf")),
                ),
                oracleText = "",
                baseStats = CreatureStats(2, 2),
                colors = setOf(Color.GREEN),
                ownerId = ownerId,
                spellEffect = null,
            ),
            OwnerComponent(ownerId),
        )

        val state = GameState(format = Format.Commander())
            .withEntity(ownerId, ComponentContainer.EMPTY)
            .withEntity(nonCommander, nonCommanderContainer)
            .addToZone(ZoneKey(ownerId, Zone.GRAVEYARD), nonCommander)
            .copy(turnOrder = listOf(ownerId))

        val result = check.check(state)
        result.isPaused shouldBe false
    }

    test("commander already on the stack is not prompted (CR 903.9 entry zones only)") {
        // Stack is for resolving spells; the SBA only fires on graveyard/exile/hand/library.
        val state = stateWithCommanderIn(Zone.STACK)
        val result = check.check(state)
        result.isPaused shouldBe false
    }

    test("commander entry is later 'forgotten' once the marker is stripped") {
        // The asked marker prevents re-prompts. Once it's stripped (which ZoneTransitionService
        // does on every commander zone change), the SBA prompts again from scratch.
        val asked = stateWithCommanderIn(Zone.GRAVEYARD)
            .updateEntity(cmdrId) { c -> c.with(CommanderZoneChoiceAskedComponent) }
        check.check(asked).isPaused shouldBe false

        val stripped = asked.updateEntity(cmdrId) { c ->
            c.without<CommanderZoneChoiceAskedComponent>()
        }
        check.check(stripped).isPaused shouldBe true
    }

    test("a commander that's been registered but has no zone slot is skipped silently") {
        // Defensive: a commander entity exists but isn't in any zone yet (e.g. mid-construction).
        // The check should not throw or wrongly pause.
        val state = GameState(format = Format.Commander())
            .withEntity(ownerId, ComponentContainer.EMPTY)
            .withEntity(cmdrId, commanderCard())
            .copy(turnOrder = listOf(ownerId))

        val result = check.check(state)
        result.isPaused shouldBe false
        result.newState shouldBe state
    }

    test("owner with zero commanders does nothing") {
        val state = GameState(format = Format.Commander())
            .withEntity(ownerId, ComponentContainer.EMPTY)
            .copy(turnOrder = listOf(ownerId))

        val result = check.check(state)
        result.isPaused shouldBe false
        result.newState.continuationStack shouldBe state.continuationStack
        result.newState.getEntity(cmdrId) shouldBe null
        // Sanity — turn order survives unchanged (we don't mutate state on the no-op path)
        result.newState.turnOrder shouldNotBe emptyList<EntityId>()
    }
})
