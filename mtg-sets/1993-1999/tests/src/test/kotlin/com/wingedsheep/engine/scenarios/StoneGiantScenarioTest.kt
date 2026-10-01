package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class StoneGiantScenarioTest : ScenarioTestBase() {
    init {
        fun board(step: Step = Step.PRECOMBAT_MAIN) = scenario().withPlayers()
            .withCardOnBattlefield(1, "Stone Giant")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
            .inPhase(if (step == Step.END) Phase.ENDING else Phase.PRECOMBAT_MAIN, step).build()
        fun activate(game: TestGame, name: String = "Grizzly Bears") = game.execute(ActivateAbility(
            game.player1Id, game.findPermanent("Stone Giant")!!,
            cardRegistry.getCard("Stone Giant")!!.script.activatedAbilities.single().id,
            listOf(ChosenTarget.Permanent(game.findPermanent(name)!!))))
        fun counters(game: TestGame, name: String, type: CounterType, count: Int) {
            game.state = game.state.updateEntity(game.findPermanent(name)!!) {
                it.with(CountersComponent(mapOf(type to count)))
            }
        }
        test("flying lasts for the turn and the next end step destroys the watched creature") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            activate(game).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(bear, Keyword.FLYING) shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.delayedTriggers.size shouldBe 0
        }
        test("equal toughness opponent creatures and an unmodified Giant itself are illegal") {
            for (name in listOf("Hill Giant", "Wall of Wood", "Stone Giant")) {
                val game = board()
                if (name == "Wall of Wood") counters(game, "Stone Giant", CounterType.PLUS_ONE_PLUS_ZERO, 1)
                activate(game, name).error shouldNotBe null
                game.state.stack.size shouldBe 0
            }
        }
        test("server target enumeration uses projected power and toughness") {
            val game = board()
            counters(game, "Stone Giant", CounterType.PLUS_ONE_PLUS_ZERO, 1)
            val giant = game.findPermanent("Stone Giant")!!
            val hill = game.findPermanent("Hill Giant")!!
            val requirement = cardRegistry.getCard("Stone Giant")!!.script.activatedAbilities.single().targetRequirements.single()
            val finder = TargetFinder(PredicateEvaluator(cardRegistry))
            finder.findLegalTargets(game.state, requirement, game.player1Id, giant).contains(hill) shouldBe true
            counters(game, "Hill Giant", CounterType.PLUS_ONE_PLUS_ONE, 1)
            finder.findLegalTargets(game.state, requirement, game.player1Id, giant).contains(hill) shouldBe false
        }
        test("increasing target toughness to equal source power fizzles before any effect") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            activate(game).error shouldBe null
            counters(game, "Grizzly Bears", CounterType.PLUS_ONE_PLUS_ONE, 1)
            game.resolveStack()
            game.state.projectedState.hasKeyword(bear, Keyword.FLYING) shouldBe false
            game.state.delayedTriggers.size shouldBe 0
        }
        test("decreasing source power also invalidates the target on resolution") {
            val game = board()
            activate(game).error shouldBe null
            counters(game, "Stone Giant", CounterType.MINUS_ONE_MINUS_ONE, 1)
            game.resolveStack()
            game.state.delayedTriggers.size shouldBe 0
        }
        for (departed in listOf(false, true)) {
            test("a source made noncreature has no power when ${if (departed) "departed" else "still on the battlefield"}") {
                val game = board()
                val source = game.findPermanent("Stone Giant")!!
                val bear = game.findPermanent("Grizzly Bears")!!
                activate(game).error shouldBe null
                game.state = game.state.addFloatingEffect(
                    layer = Layer.TYPE,
                    modification = SerializableModification.SetCardTypes(setOf("LAND")),
                    affectedEntities = setOf(source),
                    duration = Duration.EndOfTurn,
                    context = EffectContext(sourceId = source, controllerId = game.player1Id),
                )
                game.state.projectedState.isCreature(source) shouldBe false
                if (departed) {
                    game.state = game.zones.moveToZone(game.state, source, Zone.GRAVEYARD).state
                }
                game.resolveStack()
                game.state.projectedState.hasKeyword(bear, Keyword.FLYING) shouldBe false
                game.state.delayedTriggers.size shouldBe 0
            }
        }
        test("a Giant with increased power can throw itself") {
            val game = board()
            counters(game, "Stone Giant", CounterType.PLUS_ONE_PLUS_ZERO, 2)
            val giant = game.findPermanent("Stone Giant")!!
            activate(game, "Stone Giant").error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInGraveyard(1, "Stone Giant") shouldBe true
        }
        test("source departure uses power at departure rather than activation or printed power") {
            val game = board()
            counters(game, "Stone Giant", CounterType.PLUS_ONE_PLUS_ZERO, 1)
            activate(game, "Hill Giant").error shouldBe null
            counters(game, "Stone Giant", CounterType.MINUS_ONE_MINUS_ONE, 1)
            val giant = game.findPermanent("Stone Giant")!!
            game.state = game.zones.moveToZone(game.state, giant, Zone.GRAVEYARD).state
            game.resolveStack()
            game.state.delayedTriggers.size shouldBe 0
        }
        test("pumped departed source still resolves and a blink cannot substitute its new power") {
            val game = board()
            counters(game, "Stone Giant", CounterType.PLUS_ONE_PLUS_ZERO, 1)
            activate(game, "Hill Giant").error shouldBe null
            val giant = game.findPermanent("Stone Giant")!!
            game.state = game.zones.moveToZone(game.state, giant, Zone.EXILE).state
            game.state = game.zones.moveToZone(game.state, giant, Zone.BATTLEFIELD).state
            game.resolveStack()
            game.state.projectedState.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.FLYING) shouldBe true
            game.state.delayedTriggers.size shouldBe 1
        }
        test("delayed destruction ignores subsequent control and toughness changes") {
            val game = board()
            activate(game).error shouldBe null
            game.resolveStack()
            counters(game, "Grizzly Bears", CounterType.PLUS_ONE_PLUS_ONE, 5)
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bear) { it.with(ControllerComponent(game.player2Id)) }
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
        test("delayed destruction still affects a permanent that stopped being a creature") {
            val game = board()
            activate(game).error shouldBe null
            game.resolveStack()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bear) {
                val card = it.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!
                it.with(card.copy(typeLine = com.wingedsheep.sdk.core.TypeLine.artifact(), baseStats = null))
            }
            game.state.projectedState.isCreature(bear) shouldBe false
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
        test("changing target control before resolution invalidates it") {
            val game = board()
            activate(game).error shouldBe null
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bear) { it.with(ControllerComponent(game.player2Id)) }
            game.resolveStack()
            game.state.delayedTriggers.size shouldBe 0
        }
        test("a blinked target is a different object and survives the delayed trigger") {
            val game = board()
            activate(game).error shouldBe null
            game.resolveStack()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.zones.moveToZone(game.state, bear, Zone.EXILE).state
            game.state = game.zones.moveToZone(game.state, bear, Zone.BATTLEFIELD).state
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
        test("an end-step activation waits for the next turn's end step while flying expires") {
            val game = board(Step.END)
            val bear = game.findPermanent("Grizzly Bears")!!
            val turn = game.state.turnNumber
            activate(game).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.turnNumber shouldBe turn + 1
            game.state.projectedState.hasKeyword(bear, Keyword.FLYING) shouldBe false
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
