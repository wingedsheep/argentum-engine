package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RagingRiverScenarioTest : ScenarioTestBase() {
    init {
        fun board(twoRivers: Boolean = false): TestGame {
            val builder = scenario().withPlayers()
                .withCardOnBattlefield(1, "Raging River")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wall of Wood")
                .withCardOnBattlefield(2, "Giant Spider")
                .withCardOnBattlefield(2, "Flying Men")
                .withCardInHand(2, "Squire")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            if (twoRivers) builder.withCardOnBattlefield(1, "Raging River")
            return builder.build()
        }
        fun partition(game: TestGame, leftNames: List<String>) {
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.playerId shouldBe game.player2Id
            decision.options.contains(game.findPermanent("Flying Men")!!) shouldBe false
            game.selectCards(leftNames.map { game.findPermanent(it)!! }).error shouldBe null
        }
        fun side(game: TestGame, label: String) {
            val decision = game.state.pendingDecision as ChooseOptionDecision
            decision.playerId shouldBe game.player1Id
            decision.options shouldBe listOf("left", "right")
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(label))).error shouldBe null
        }
        fun attack(game: TestGame) {
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.resolveStack()
        }
        fun finish(game: TestGame) {
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }
        fun flying(game: TestGame, name: String, grant: Boolean) {
            game.state = game.state.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = if (grant) SerializableModification.GrantKeyword("FLYING")
                    else SerializableModification.RemoveKeyword("FLYING"),
                affectedEntities = setOf(game.findPermanent(name)!!),
                duration = Duration.EndOfTurn,
                context = EffectContext(sourceId = null, controllerId = game.player2Id)
            )
        }
        test("the attacker chooses a labelled pile and reach is not the flying exception") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            val choice = game.state.pendingDecision as ChooseOptionDecision
            choice.context.sourceId shouldBe game.findPermanent("Hill Giant")
            choice.context.sourceName shouldBe "Hill Giant"
            side(game, "left")
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Giant Spider" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        }
        test("a paused pile choice round trips with its attacker and both collections") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            game.state = json.decodeFromString<GameState>(json.encodeToString(GameState.serializer(), game.state))
            (game.state.pendingDecision as ChooseOptionDecision).context.sourceId shouldBe game.findPermanent("Hill Giant")
            side(game, "left")
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        }
        test("a right-side choice allows the right pile") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "right")
            finish(game)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldBe null
        }
        test("wrong-pile creatures gaining flying can block and initial fliers losing flying cannot") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            flying(game, "Wall of Wood", true)
            flying(game, "Flying Men", false)
            finish(game)
            game.state.projectedState.hasKeyword(game.findPermanent("Wall of Wood")!!, Keyword.FLYING) shouldBe true
            game.declareBlockers(mapOf("Flying Men" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldBe null
        }
        test("initial fliers can block either pile") {
            val game = board()
            attack(game)
            partition(game, emptyList())
            side(game, "left")
            finish(game)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Flying Men" to listOf("Hill Giant"))).error shouldBe null
        }
        test("a new arrival without flying belongs to neither pile") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            val arrival = game.state.getHand(game.player2Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Squire"
            }
            game.state = game.state.removeFromZone(ZoneKey(game.player2Id, Zone.HAND), arrival)
                .addToZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), arrival)
                .updateEntity(arrival) { it.with(ControllerComponent(game.player2Id)) }
            finish(game)
            game.declareBlockers(mapOf("Squire" to listOf("Hill Giant"))).error shouldNotBe null
        }
        test("a blinked chosen blocker is a new object and loses pile membership") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            val bear = game.findPermanent("Grizzly Bears")!!
            val battlefield = ZoneKey(game.player2Id, Zone.BATTLEFIELD)
            val exile = ZoneKey(game.player2Id, Zone.EXILE)
            game.state = game.state.removeFromZone(battlefield, bear).addToZone(exile, bear)
                .removeFromZone(exile, bear).addToZone(battlefield, bear)
            finish(game)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
        }
        test("source departure after resolution does not remove the combat restriction") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            val river = game.findPermanent("Raging River")!!
            game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD), river)
                .addToZone(ZoneKey(game.player1Id, Zone.GRAVEYARD), river)
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        }
        test("source departure before resolution preserves the triggering player's choices") {
            val game = board()
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            val river = game.findPermanent("Raging River")!!
            game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD), river)
                .addToZone(ZoneKey(game.player1Id, Zone.GRAVEYARD), river)
            game.resolveStack()
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        }
        test("the restriction expires when combat ends") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            game.state.floatingEffects.count {
                it.effect.modification is SerializableModification.CantBeBlockedExceptByCollection
            } shouldBe 1
            finish(game)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.state.floatingEffects.count {
                it.effect.modification is SerializableModification.CantBeBlockedExceptByCollection
            } shouldBe 0
        }
        test("one attack batch partitions once then chooses once per attacker") {
            val game = board()
            game.declareAttackers(mapOf("Hill Giant" to 2, "Savannah Lions" to 2)).error shouldBe null
            game.resolveStack()
            partition(game, listOf("Grizzly Bears"))
            val first = (game.state.pendingDecision as ChooseOptionDecision).context.sourceId
            side(game, "left")
            val second = (game.state.pendingDecision as ChooseOptionDecision).context.sourceId
            first shouldNotBe second
            side(game, "right")
            game.state.pendingDecision shouldBe null
            finish(game)
            val leftAttacker = game.state.getEntity(first!!)?.get<CardComponent>()!!.name
            val rightAttacker = game.state.getEntity(second!!)?.get<CardComponent>()!!.name
            game.declareBlockers(mapOf("Grizzly Bears" to listOf(rightAttacker))).error shouldNotBe null
            game.declareBlockers(mapOf("Wall of Wood" to listOf(leftAttacker))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf(leftAttacker), "Wall of Wood" to listOf(rightAttacker))).error shouldBe null
        }
        test("all nonflying piles may be empty but every attacker still chooses a label") {
            val game = board()
            for (name in listOf("Grizzly Bears", "Wall of Wood", "Giant Spider")) {
                flying(game, name, true)
            }
            attack(game)
            (game.state.pendingDecision is ChooseOptionDecision) shouldBe true
            side(game, "left")
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldBe null
        }
        test("a creature entering attacking after resolution receives no River restriction") {
            val game = board()
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            val lateAttacker = game.findPermanent("Savannah Lions")!!
            game.state = game.state.updateEntity(lateAttacker) {
                it.with(AttackingComponent(game.player2Id))
            }
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Savannah Lions"))).error shouldBe null
        }
        test("an unattacked opponent partitions before the attacker chooses a label") {
            val game = board()
            val third = EntityId.of("player-3")
            val spider = game.findPermanent("Giant Spider")!!
            game.state = game.state.withEntity(third, ComponentContainer.of(
                PlayerComponent("Player3"), LifeTotalComponent(20)
            )).copy(turnOrder = listOf(game.player1Id, game.player2Id, third))
                .removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), spider)
                .addToZone(ZoneKey(third, Zone.BATTLEFIELD), spider)
                .updateEntity(spider) { it.with(ControllerComponent(third)) }
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            val thirdDecision = game.state.pendingDecision as SelectCardsDecision
            thirdDecision.playerId shouldBe third
            thirdDecision.options shouldBe listOf(spider)
            game.selectCards(listOf(spider)).error shouldBe null
            (game.state.pendingDecision is ChooseOptionDecision) shouldBe true
            side(game, "left")
            finish(game)
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        }
        test("two Rivers impose both restrictions even when the attacker chooses different piles") {
            val game = board(twoRivers = true)
            attack(game)
            partition(game, listOf("Grizzly Bears"))
            side(game, "left")
            game.resolveStack()
            partition(game, listOf("Grizzly Bears"))
            side(game, "right")
            finish(game)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Wall of Wood" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Flying Men" to listOf("Hill Giant"))).error shouldBe null
        }
    }
}
