package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.MayAbilityContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Actual import/resume boundary; missing historical refs cannot be recovered from current visits. */
class LegacyPausedObjectIdentityTest : FunSpec({
    // Same options as persistenceJson. GameState uses the engine module only; the server module
    // contributes client log serializers and therefore is not a rules-engine test dependency.
    val persistenceJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        allowStructuredMapKeys = true
        serializersModule = engineSerializersModule
    }

    fun stripHistoricalIdentity(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.filterKeys {
            it !in setOf("objectReferences", "objectIdentities", "nextObjectGeneration")
        }.mapValues { stripHistoricalIdentity(it.value) })
        is JsonArray -> JsonArray(value.map(::stripHistoricalIdentity))
        else -> value
    }

    for (sourceInstructionFrame in listOf("may", "effect")) {
        for (legacy in listOf(false, true)) {
            for (interveningVisit in listOf(false, true)) {
                test("$sourceInstructionFrame resume legacy=$legacy interveningVisit=$interveningVisit keeps independent effects") {
                    val driver = GameTestDriver().apply {
                        registerCards(TestCards.all)
                        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
                        passPriorityUntil(Step.PRECOMBAT_MAIN)
                    }
                    val player = driver.player1
                    val source = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
                    val sourceObject = driver.state.objectRef(source)!!
                    val lifeBefore = driver.getLifeTotal(player)
                    val context = EffectContext(
                        sourceId = source,
                        controllerId = player,
                        objectReferences = ObjectReferenceEnvironment(
                            captured = true, origin = sourceObject, source = sourceObject,
                            resolutionKey = "paused-import:$source:${sourceObject.generation}"
                        )
                    )
                    val bounce = Effects.ReturnToHand(EffectTarget.Self)
                    val yesEffects = if (sourceInstructionFrame == "may")
                        listOf(bounce, Effects.GainLife(2)) else listOf(Effects.GainLife(2))
                    val remainingEffects = if (sourceInstructionFrame == "effect")
                        listOf(bounce, Effects.GainLife(3)) else listOf(Effects.GainLife(3))
                    val decisionId = "historical-entry-decision"
                    var paused = driver.state
                        .pushContinuation(EffectContinuation(
                            decisionId = "remaining-import-effects",
                            remainingEffects = remainingEffects,
                            effectContext = context
                        ))
                        .pushContinuation(MayAbilityContinuation(
                            decisionId = decisionId,
                            playerId = player,
                            sourceName = "Grizzly Bears",
                            effectIfYes = CompositeEffect(yesEffects),
                            effectIfNo = null,
                            effectContext = context
                        ))
                        .withPendingDecision(YesNoDecision(
                            id = decisionId,
                            playerId = player,
                            prompt = "Continue the saved effect?",
                            context = DecisionContext(sourceId = source)
                        ))
                    if (interveningVisit) {
                        paused = paused.moveToZone(source, ZoneKey(player, Zone.BATTLEFIELD), ZoneKey(player, Zone.EXILE))
                            .moveToZone(source, ZoneKey(player, Zone.EXILE), ZoneKey(player, Zone.BATTLEFIELD))
                    }

                    val serialized = persistenceJson.encodeToJsonElement(GameState.serializer(), paused)
                    val imported = persistenceJson.decodeFromJsonElement(GameState.serializer(),
                        if (legacy) stripHistoricalIdentity(serialized) else serialized)
                        .initializeObjectIdentities()
                    // The importer can establish a current object; this is not proof of history.
                    imported.objectRef(source) shouldNotBe null
                    val importedMay = imported.continuationStack.last() as MayAbilityContinuation
                    importedMay.effectContext.objectReferences.captured shouldBe !legacy
                    driver.replaceState(imported)

                    // Goes through SubmitDecision -> ContinuationHandler -> registered may resumer
                    // and automatic EffectContinuation resume, including the actual executor registry.
                    driver.submitYesNo(player, true).error shouldBe null
                    driver.pendingDecision shouldBe null
                    driver.state.continuationStack shouldBe emptyList()
                    driver.getLifeTotal(player) shouldBe lifeBefore + 5
                    val shouldReturn = !legacy && !interveningVisit
                    (source in driver.state.getHand(player)) shouldBe shouldReturn
                    (source in driver.state.getBattlefield()) shouldBe !shouldReturn
                }
            }
        }
    }
})
