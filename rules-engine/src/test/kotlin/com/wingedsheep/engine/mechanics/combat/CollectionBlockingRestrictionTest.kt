package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.core.BlockerDeclarationPolicyChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.combat.GrantCantBeBlockedExceptByCollectionExecutor
import com.wingedsheep.engine.mechanics.combat.rules.BlockCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.CantBeBlockedExceptByCollectionRule
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CollectionBlockingRestrictionTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardOnBattlefield(2, "Giant Spider")
            .withCardOnBattlefield(2, "Air Elemental")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).inPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS).build()
        fun grant(game: TestGame, names: List<String>, filter: GameObjectFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)) {
            val attacker = game.findPermanent("Grizzly Bears")!!
            val before = game.state
            val result = GrantCantBeBlockedExceptByCollectionExecutor().execute(
                before,
                GrantCantBeBlockedExceptByCollectionEffect(EffectTarget.SpecificEntity(attacker), "pile", filter, Duration.EndOfCombat),
                EffectContext(controllerId = game.player1Id, sourceId = attacker,
                    pipeline = PipelineState(storedCollections = mapOf("pile" to names.map { game.findPermanent(it)!! })))
            )
            result.events.filterIsInstance<BlockerDeclarationPolicyChangedEvent>().size shouldBe 1
            before.floatingEffects.size + 1 shouldBe result.state.floatingEffects.size
            game.state = result.state
        }
        fun check(game: TestGame, blocker: String): String? {
            val rule = CantBeBlockedExceptByCollectionRule(PredicateEvaluator(cardRegistry = cardRegistry))
            return rule.check(BlockCheckContext(game.state, game.state.projectedState,
                game.findPermanent("Grizzly Bears")!!, game.findPermanent(blocker)!!, game.player2Id, cardRegistry))
        }
        test("collection members and current flying blockers pass, reach alone does not") {
            val game = board()
            grant(game, listOf("Wall of Wood"))
            check(game, "Wall of Wood") shouldBe null
            check(game, "Air Elemental") shouldBe null
            check(game, "Giant Spider").shouldNotBeNull()
        }
        test("empty collection still restricts ground creatures and permits the alternative") {
            val game = board()
            grant(game, emptyList())
            check(game, "Wall of Wood").shouldNotBeNull()
            check(game, "Air Elemental") shouldBe null
        }
        test("multiple restrictions intersect rather than replace each other") {
            val game = board()
            grant(game, listOf("Wall of Wood"))
            grant(game, listOf("Giant Spider"))
            check(game, "Wall of Wood").shouldNotBeNull()
            check(game, "Giant Spider").shouldNotBeNull()
            check(game, "Air Elemental") shouldBe null
        }
        test("collection membership does not survive a different battlefield visit") {
            val game = board()
            grant(game, listOf("Wall of Wood"))
            val wall = game.findPermanent("Wall of Wood")!!
            val identity = game.state.objectIdentities.getValue(wall)
            game.state = game.state.copy(objectIdentities = game.state.objectIdentities + (wall to identity.copy(generation = identity.generation + 100)))
            check(game, "Wall of Wood").shouldNotBeNull()
        }
        test("restriction does not follow the attacker to a different battlefield visit") {
            val game = board()
            grant(game, emptyList())
            val attacker = game.findPermanent("Grizzly Bears")!!
            val identity = game.state.objectIdentities.getValue(attacker)
            game.state = game.state.copy(objectIdentities = game.state.objectIdentities + (attacker to identity.copy(generation = identity.generation + 100)))
            check(game, "Wall of Wood") shouldBe null
        }
        test("state round trip preserves membership and object references") {
            val game = board()
            grant(game, listOf("Wall of Wood"))
            val json = Json { allowStructuredMapKeys = true; serializersModule = engineSerializersModule }
            game.state = json.decodeFromString<GameState>(json.encodeToString(GameState.serializer(), game.state))
            check(game, "Wall of Wood") shouldBe null
            check(game, "Giant Spider").shouldNotBeNull()
            val restriction = game.state.floatingEffects.last().effect.modification as SerializableModification.CantBeBlockedExceptByCollection
            restriction.blockers.single().entityId shouldBe game.findPermanent("Wall of Wood")!!
        }
        test("an evasion badge explains the collection alternative and duration") {
            val game = board()
            grant(game, listOf("Wall of Wood"))
            val view = ClientStateTransformer(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = cardRegistry))
                .transform(game.state, game.player1Id)
            val effects = view.cards.getValue(game.findPermanent("Grizzly Bears")!!).activeEffects
            val badge = effects.single { it.effectId == "cant_be_blocked_except_by_collection" }
            badge.duration shouldBe "until end of combat"
            badge.description!!.contains("chosen creatures") shouldBe true
        }
    }
}
