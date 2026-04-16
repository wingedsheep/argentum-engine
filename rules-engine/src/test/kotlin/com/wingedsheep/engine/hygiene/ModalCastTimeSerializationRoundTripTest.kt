package com.wingedsheep.engine.hygiene

import com.wingedsheep.engine.core.CastModalModeSelectionContinuation
import com.wingedsheep.engine.core.CastModalTargetSelectionContinuation
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * Tests L2 / L3 from [`backlog/modal-cast-time-choices-plan.md`]:
 *
 * - L2: [CastSpell] actions carrying `chosenModes` and `modeTargetsOrdered` must
 *   survive a JSON round-trip via [engineSerializersModule]. The cast-time
 *   continuation stores the original action verbatim; if it didn't round-trip
 *   cleanly, pausing and resuming a modal cast (across a server restart or
 *   spectator rejoin) would drop the pre-selected modes or targets.
 *
 * - L3: [CastModalModeSelectionContinuation] and [CastModalTargetSelectionContinuation]
 *   must be polymorphically registered and round-trip, including the
 *   `List<Mode>` payload (with its nested `Effect` hierarchy).
 */
class ModalCastTimeSerializationRoundTripTest : FunSpec({

    val json = Json {
        serializersModule = engineSerializersModule
        encodeDefaults = true
    }

    test("L2 — CastSpell with chosenModes, modeTargetsOrdered, modeDamageDistribution round-trips") {
        val player = EntityId.generate()
        val card = EntityId.generate()
        val creature = EntityId.generate()
        val goblin = EntityId.generate()
        val opponentPlayer = EntityId.generate()

        val original: GameAction = CastSpell(
            playerId = player,
            cardId = card,
            // Flat `targets` holds the union — this is what StackResolver populates
            // TargetsComponent with for arrow rendering. Per-mode breakdown lives below.
            targets = listOf(
                ChosenTarget.Permanent(creature),
                ChosenTarget.Permanent(goblin),
                ChosenTarget.Player(opponentPlayer)
            ),
            chosenModes = listOf(2, 3, 0),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Permanent(creature)),
                listOf(ChosenTarget.Permanent(creature), ChosenTarget.Permanent(goblin)),
                listOf(ChosenTarget.Player(opponentPlayer))
            ),
            modeDamageDistribution = mapOf(
                2 to mapOf(goblin to 2, creature to 1)
            )
        )

        val encoded = json.encodeToString(GameAction.serializer(), original)
        val decoded = json.decodeFromString(GameAction.serializer(), encoded) as CastSpell

        decoded.playerId shouldBe player
        decoded.cardId shouldBe card
        decoded.chosenModes shouldBe listOf(2, 3, 0)
        decoded.modeTargetsOrdered shouldBe listOf(
            listOf(ChosenTarget.Permanent(creature)),
            listOf(ChosenTarget.Permanent(creature), ChosenTarget.Permanent(goblin)),
            listOf(ChosenTarget.Player(opponentPlayer))
        )
        decoded.modeDamageDistribution shouldBe mapOf(2 to mapOf(goblin to 2, creature to 1))
        decoded.targets.size shouldBe 3
    }

    test("L2 — legacy CastSpell without chosenModes still round-trips with empty defaults") {
        val original: GameAction = CastSpell(
            playerId = EntityId.generate(),
            cardId = EntityId.generate()
        )

        val decoded = json.decodeFromString(
            GameAction.serializer(),
            json.encodeToString(GameAction.serializer(), original)
        ) as CastSpell

        decoded.chosenModes shouldBe emptyList()
        decoded.modeTargetsOrdered shouldBe emptyList()
        decoded.modeDamageDistribution shouldBe emptyMap()
    }

    test("L3 — CastModalModeSelectionContinuation round-trips with Mode payload") {
        // Build two concrete modes with distinct shapes: one no-target draw, one
        // targeted keyword grant. Both must serialize through the Effect polymorphic
        // hierarchy carried by ModalEffect.
        val modes = listOf(
            Mode.noTarget(
                DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                "Draw a card"
            ),
            Mode.noTarget(
                GrantKeywordEffect(Keyword.TRAMPLE, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                "Target creature gains trample until end of turn"
            )
        )

        val baseAction = CastSpell(
            playerId = EntityId.generate(),
            cardId = EntityId.generate()
        )

        val original: ContinuationFrame = CastModalModeSelectionContinuation(
            decisionId = "decision-42",
            cardId = baseAction.cardId,
            casterId = baseAction.playerId,
            baseCastAction = baseAction,
            modes = modes,
            chooseCount = 2,
            minChooseCount = 1,
            allowRepeat = true,
            offeredIndices = listOf(0, 1),
            availableIndices = null,                       // allowRepeat=true → null
            selectedModeIndices = listOf(0),                // one mode picked already
            doneOptionOffered = true                        // minChooseCount=1 + 1 picked → Done offered
        )

        val encoded = json.encodeToString(ContinuationFrame.serializer(), original)
        val decoded = json.decodeFromString(
            ContinuationFrame.serializer(),
            encoded
        ) as CastModalModeSelectionContinuation

        decoded.decisionId shouldBe "decision-42"
        decoded.chooseCount shouldBe 2
        decoded.minChooseCount shouldBe 1
        decoded.allowRepeat shouldBe true
        decoded.offeredIndices shouldBe listOf(0, 1)
        decoded.availableIndices shouldBe null
        decoded.selectedModeIndices shouldBe listOf(0)
        decoded.doneOptionOffered shouldBe true
        decoded.modes.size shouldBe 2
        decoded.modes[0].description shouldBe "Draw a card"
        decoded.modes[1].description shouldBe "Target creature gains trample until end of turn"
        decoded.baseCastAction.cardId shouldBe baseAction.cardId
        decoded.baseCastAction.playerId shouldBe baseAction.playerId
    }

    test("L3 — CastModalTargetSelectionContinuation round-trips with per-mode ChosenTargets") {
        val modes = listOf(
            Mode.noTarget(
                DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                "Draw a card"
            ),
            Mode.noTarget(
                GrantKeywordEffect(Keyword.FLYING, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                "Target creature gains flying"
            )
        )

        val creatureA = EntityId.generate()
        val creatureB = EntityId.generate()
        val playerTarget = EntityId.generate()
        val baseAction = CastSpell(
            playerId = EntityId.generate(),
            cardId = EntityId.generate()
        )

        val original: ContinuationFrame = CastModalTargetSelectionContinuation(
            decisionId = "target-decision-7",
            cardId = baseAction.cardId,
            casterId = baseAction.playerId,
            baseCastAction = baseAction,
            modes = modes,
            chosenModeIndices = listOf(0, 1, 1),                 // allowRepeat style: mode 1 twice
            resolvedModeTargets = listOf(
                emptyList(),                                      // mode 0: no targets
                listOf(ChosenTarget.Permanent(creatureA)),        // first mode 1
                listOf(ChosenTarget.Player(playerTarget), ChosenTarget.Permanent(creatureB))
            ),
            currentOrdinal = 3
        )

        val decoded = json.decodeFromString(
            ContinuationFrame.serializer(),
            json.encodeToString(ContinuationFrame.serializer(), original)
        ) as CastModalTargetSelectionContinuation

        decoded.decisionId shouldBe "target-decision-7"
        decoded.chosenModeIndices shouldBe listOf(0, 1, 1)
        decoded.currentOrdinal shouldBe 3
        decoded.resolvedModeTargets.size shouldBe 3
        decoded.resolvedModeTargets[0] shouldBe emptyList()
        decoded.resolvedModeTargets[1] shouldBe listOf(ChosenTarget.Permanent(creatureA))
        decoded.resolvedModeTargets[2] shouldBe listOf(
            ChosenTarget.Player(playerTarget),
            ChosenTarget.Permanent(creatureB)
        )
        decoded.modes.size shouldBe 2
    }
})
