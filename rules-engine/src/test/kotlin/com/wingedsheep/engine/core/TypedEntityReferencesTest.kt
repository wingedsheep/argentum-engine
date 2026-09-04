package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TypedEntityReferencesTest : FunSpec({

    test("action traversal retains repeated nested references and entity map-key locations") {
        val player = EntityId.of("player")
        val card = EntityId.of("card")
        val target = EntityId.of("target")
        val owner = EntityId.of("owner")
        val manaSource = EntityId.of("mana-source")
        val delved = EntityId.of("delved")
        val convoked = EntityId.of("convoked")
        val sacrificed = EntityId.of("sacrificed")
        val counterRemoval = EntityId.of("counter-removal")
        val damageTarget = EntityId.of("damage-target")
        val modeDamageTarget = EntityId.of("mode-damage-target")
        val repeated = EntityId.of("repeated")
        val ordinaryText = EntityId.of("ordinary-text-that-looks-like-an-id")
        val action = CastSpell(
            playerId = player,
            cardId = card,
            targets = listOf(ChosenTarget.Card(target, owner, com.wingedsheep.sdk.core.Zone.HAND)),
            paymentStrategy = PaymentStrategy.Explicit(listOf(manaSource)),
            alternativePayment = AlternativePaymentChoice(
                delvedCards = listOf(delved),
                convokedCreatures = linkedMapOf(convoked to ConvokePayment()),
                harmonizeCreature = repeated,
                tapForGenericPermanents = linkedSetOf(repeated),
            ),
            additionalCostPayment = AdditionalCostPayment(
                sacrificedPermanents = listOf(sacrificed),
                distributedCounterRemovals = listOf(
                    DistributedCounterRemoval(counterRemoval, ordinaryText.value, 1),
                ),
            ),
            giftRecipient = repeated,
            splicedCardIds = listOf(repeated),
            damageDistribution = linkedMapOf(damageTarget to 2),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(repeated))),
            modeDamageDistribution = linkedMapOf(0 to linkedMapOf(modeDamageTarget to 1)),
            conspiredCreatures = listOf(repeated),
            casualtyCreature = repeated,
        )

        val projection = TypedEntityReferences.action(action)
            .shouldBeInstanceOf<TypedEntityReferences.Projection.Complete>()

        projection.occurrences.count { it.entityId == repeated } shouldBe 7
        projection.entityIds shouldBe setOf(
            player,
            card,
            target,
            owner,
            manaSource,
            delved,
            convoked,
            sacrificed,
            counterRemoval,
            damageTarget,
            modeDamageTarget,
            repeated,
        )
        projection.occurrences shouldContain TypedEntityReferences.Occurrence(
            damageTarget,
            listOf(
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("damageDistribution"),
                TypedEntityReferences.PathSegment.MapEntry(0, TypedEntityReferences.PathSegment.MapEntry.Role.KEY),
            ),
        )
        projection.occurrences shouldContain TypedEntityReferences.Occurrence(
            modeDamageTarget,
            listOf(
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("modeDamageDistribution"),
                TypedEntityReferences.PathSegment.MapEntry(0, TypedEntityReferences.PathSegment.MapEntry.Role.VALUE),
                TypedEntityReferences.PathSegment.MapEntry(0, TypedEntityReferences.PathSegment.MapEntry.Role.KEY),
            ),
        )
        projection.occurrences shouldContain TypedEntityReferences.Occurrence(
            target,
            listOf(
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("targets"),
                TypedEntityReferences.PathSegment.Element(0),
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("cardId"),
            ),
        )
        projection.shouldResolveAgainst(
            normalEngineJson.encodeToJsonElement(GameAction.serializer(), action),
        )
    }

    test("nested submit-decision traversal keeps map keys values and ordinary strings distinct") {
        val player = EntityId.of("player")
        val blocker = EntityId.of("blocker")
        val attacker = EntityId.of("attacker")
        val otherBlocker = EntityId.of("other-blocker")
        val ordinaryText = EntityId.of("ordinary-text-that-looks-like-an-id")
        val response = CombatResolutionResponse(
            decisionId = ordinaryText.value,
            edges = listOf(DamageEdgeAmount(ordinaryText.value, 2)),
            orderedBlockers = linkedMapOf(blocker to listOf(attacker, attacker)),
            orderedAttackers = linkedMapOf(attacker to listOf(otherBlocker)),
        )
        val action = SubmitDecision(player, response)

        val projection = TypedEntityReferences.action(action)
            .shouldBeInstanceOf<TypedEntityReferences.Projection.Complete>()

        projection.occurrences.count { it.entityId == attacker } shouldBe 3
        projection.entityIds shouldNotContain ordinaryText
        projection.occurrences shouldContain TypedEntityReferences.Occurrence(
            blocker,
            listOf(
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("response"),
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("orderedBlockers"),
                TypedEntityReferences.PathSegment.MapEntry(0, TypedEntityReferences.PathSegment.MapEntry.Role.KEY),
            ),
        )
        projection.occurrences shouldContain TypedEntityReferences.Occurrence(
            attacker,
            listOf(
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("response"),
                TypedEntityReferences.PathSegment.PolymorphicPayload,
                TypedEntityReferences.PathSegment.Field("orderedBlockers"),
                TypedEntityReferences.PathSegment.MapEntry(0, TypedEntityReferences.PathSegment.MapEntry.Role.VALUE),
                TypedEntityReferences.PathSegment.Element(0),
            ),
        )
        projection.shouldResolveAgainst(
            normalEngineJson.encodeToJsonElement(GameAction.serializer(), action),
        )
        val responseProjection = TypedEntityReferences.response(response)
            .shouldBeInstanceOf<TypedEntityReferences.Projection.Complete>()
        responseProjection.occurrences.count { it.entityId == attacker } shouldBe 3
        responseProjection.shouldResolveAgainst(
            normalEngineJson.encodeToJsonElement(DecisionResponse.serializer(), response),
        )
    }

    test("action traversal does not mistake an equal AbilityId for an EntityId") {
        val player = EntityId.of("player")
        val sharedBytes = "same-entity-and-ability-bytes"
        val source = EntityId.of(sharedBytes)
        val action = ActivateAbility(
            playerId = player,
            sourceId = source,
            abilityId = AbilityId(sharedBytes),
        )

        val projection = TypedEntityReferences.action(action)
            .shouldBeInstanceOf<TypedEntityReferences.Projection.Complete>()

        projection.entityIds shouldBe setOf(player, source)
        projection.occurrences.count { it.entityId == source } shouldBe 1
    }

    test("action traversal reports serializer failure rather than an empty reference list") {
        val player = EntityId.of("player")
        val throwingSerializer = object : SerializationStrategy<GameAction> {
            override val descriptor = GameAction.serializer().descriptor

            override fun serialize(encoder: Encoder, value: GameAction): Nothing =
                throw IllegalStateException("forced traversal failure")
        }

        val projection = TypedEntityReferences.project(throwingSerializer, PassPriority(player))
            .shouldBeInstanceOf<TypedEntityReferences.Projection.Incomplete>()

        projection.rootType shouldBe PassPriority::class.java.name
        projection.failure shouldBe IllegalStateException::class.java.name
    }

    test("an unknown JSON-only serializer fails closed") {
        val player = EntityId.of("player")
        val jsonOnlySerializer = object : SerializationStrategy<GameAction> {
            override val descriptor = GameAction.serializer().descriptor

            override fun serialize(encoder: Encoder, value: GameAction) {
                (encoder as kotlinx.serialization.json.JsonEncoder)
                    .encodeJsonElement(JsonPrimitive("opaque"))
            }
        }

        TypedEntityReferences.project(jsonOnlySerializer, PassPriority(player))
            .shouldBeInstanceOf<TypedEntityReferences.Projection.Incomplete>()
            .failure shouldBe ClassCastException::class.java.name
    }
})

private val normalEngineJson = Json {
    serializersModule = engineSerializersModule
    encodeDefaults = true
    classDiscriminator = "type"
}

private fun TypedEntityReferences.Projection.Complete.shouldResolveAgainst(encoded: JsonElement) {
    occurrences.forEach { occurrence ->
        val objectJsonPath = occurrence.path.filterNot {
            it is TypedEntityReferences.PathSegment.PolymorphicPayload
        }
        encoded.resolve(objectJsonPath).jsonPrimitive.content shouldBe occurrence.entityId.value
    }
}

private fun JsonElement.resolve(path: List<TypedEntityReferences.PathSegment>): JsonElement =
    path.fold(this) { current, segment ->
        when (segment) {
            is TypedEntityReferences.PathSegment.Field -> current.jsonObject.getValue(segment.name)
            is TypedEntityReferences.PathSegment.Element -> current.jsonArray[segment.index]
            is TypedEntityReferences.PathSegment.MapEntry -> {
                val entry = current.jsonObject.entries.elementAt(segment.index)
                when (segment.role) {
                    TypedEntityReferences.PathSegment.MapEntry.Role.KEY -> JsonPrimitive(entry.key)
                    TypedEntityReferences.PathSegment.MapEntry.Role.VALUE -> entry.value
                }
            }
            TypedEntityReferences.PathSegment.PolymorphicPayload ->
                error("Object-polymorphic JSON paths must drop PolymorphicPayload")
        }
    }
