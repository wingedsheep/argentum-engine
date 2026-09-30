package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.engineSerializersModule
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.event.GrantedStateTriggeredAbility
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetTriggeredAbilityExecutor
import com.wingedsheep.engine.mechanics.stack.StackPlacement
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/** State triggers stay suppressed until the original ability leaves the stack (CR 603.8). */
class StateTriggerLifecycleScenarioTest : ScenarioTestBase() {
    private val beast = card("Lifecycle Test Beast") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        stateTriggeredAbility {
            condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Island"), negate = true)
            effect = Effects.GainLife(1, EffectTarget.Controller)
        }
    }

    init {
        cardRegistry.register(beast)
        val services = EngineServices(cardRegistry)
        val poller = services.stateTriggerPoller
        fun board(island: Boolean = false) = scenario().withPlayers()
            .withCardOnBattlefield(1, "Lifecycle Test Beast")
            .withLandsOnBattlefield(1, if (island) "Island" else "Mountain", 1)
            .withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Mountain")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun arm(game: TestGame) {
            val polled = poller.poll(game.state)
            polled.pendingTriggers.size shouldBe 1
            game.state = game.state.copy(pendingTriggers = polled.pendingTriggers)
            game.passPriority().error shouldBe null
            game.state.stack.size shouldBe 1
        }

        test("a still-true condition retriggers after each resolution") {
            val game = board()
            arm(game)
            val first = game.state.stack.single()
            repeat(2) { game.passPriority().error shouldBe null }
            game.getLifeTotal(1) shouldBe 21
            game.state.stack.size shouldBe 1
            game.state.stack.single() shouldNotBe first
            repeat(2) { game.passPriority().error shouldBe null }
            game.getLifeTotal(1) shouldBe 22
            game.state.stack.size shouldBe 1
        }

        test("pending triggers suppress another firing even through a condition toggle") {
            val game = board(island = true)
            val island = game.findPermanent("Island")!!
            val original = game.state
            val absent = game.zones.moveToZone(original, island, Zone.GRAVEYARD).state
            val waiting = poller.poll(absent).pendingTriggers
            waiting.size shouldBe 1
            poller.poll(original.copy(pendingTriggers = waiting)).pendingTriggers.shouldBeEmpty()
            poller.poll(absent.copy(pendingTriggers = waiting)).pendingTriggers.shouldBeEmpty()
        }

        test("a condition toggle while the original is on the stack never creates a duplicate") {
            val game = board(island = true)
            val island = game.findPermanent("Island")!!
            game.state = game.zones.moveToZone(game.state, island, Zone.GRAVEYARD).state
            arm(game)
            game.state = game.zones.moveToZone(game.state, island, Zone.BATTLEFIELD).state
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
            game.state = game.zones.moveToZone(game.state, island, Zone.GRAVEYARD).state
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
        }

        test("countering the original releases suppression immediately") {
            val game = board()
            arm(game)
            game.state = services.stackResolver.counterAbility(game.state, game.state.stack.single()).state
            poller.poll(game.state).pendingTriggers.size shouldBe 1
        }

        test("removal by a rule releases suppression even if the entity remains") {
            val game = board()
            arm(game)
            val removed = game.state.removeFromStack(game.state.stack.single())
            poller.poll(removed).pendingTriggers.size shouldBe 1
        }

        test("a false condition after resolution waits until it becomes true again") {
            val game = board(island = true)
            val island = game.findPermanent("Island")!!
            game.state = game.zones.moveToZone(game.state, island, Zone.GRAVEYARD).state
            arm(game)
            game.state = game.zones.moveToZone(game.state, island, Zone.BATTLEFIELD).state
            repeat(2) { game.passPriority().error shouldBe null }
            game.getLifeTotal(1) shouldBe 21
            game.state.stack.shouldBeEmpty()
            game.state = game.zones.moveToZone(game.state, island, Zone.GRAVEYARD).state
            poller.poll(game.state).pendingTriggers.size shouldBe 1
        }

        test("a copy does not prolong the original trigger's lifecycle") {
            val game = board()
            arm(game)
            val originalId = game.state.stack.single()
            val original = game.state.getEntity(originalId)!!.get<TriggeredAbilityOnStackComponent>()!!
            val copy = CopyTargetTriggeredAbilityExecutor.cloneAbility(original, game.player1Id)
            copy.stateTriggerAbilityId shouldBe null
            game.state = StackPlacement.putTriggeredAbility(game.state, copy).state
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
            game.state = services.stackResolver.counterAbility(game.state, originalId).state
            game.state.stack.size shouldBe 1
            poller.poll(game.state).pendingTriggers.size shouldBe 1
        }

        test("a battlefield round trip creates a fresh source even while the old trigger remains") {
            val game = board()
            arm(game)
            val source = game.findPermanent("Lifecycle Test Beast")!!
            game.state = game.zones.moveToZone(game.state, source, Zone.EXILE).state
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
            game.state = game.zones.moveToZone(game.state, source, Zone.BATTLEFIELD).state
            poller.poll(game.state).pendingTriggers.size shouldBe 1
        }

        test("phasing keeps object identity and does not release an outstanding trigger") {
            val game = board()
            arm(game)
            val source = game.findPermanent("Lifecycle Test Beast")!!
            game.state = game.state.updateEntity(source) { it.with(PhasedOutComponent(game.player1Id)) }
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
            game.state = game.state.updateEntity(source) { it.without<PhasedOutComponent>() }
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
        }

        test("printed and granted abilities have independent lifecycle identities") {
            val game = board()
            val granted = beast.script.stateTriggeredAbilities.single().copy(id = com.wingedsheep.sdk.scripting.AbilityId("granted-lifecycle-test"))
            game.state = game.state.copy(grantedStateTriggeredAbilities = listOf(
                GrantedStateTriggeredAbility(game.findPermanent("Lifecycle Test Beast")!!, granted, Duration.Permanent)
            ))
            val polled = poller.poll(game.state).pendingTriggers
            polled.size shouldBe 2
            poller.poll(game.state.copy(pendingTriggers = polled)).pendingTriggers.shouldBeEmpty()
        }

        test("resolution choices finish before a still-true condition can retrigger") {
            val optional = card("Optional Lifecycle Beast") {
                typeLine = "Creature — Beast"
                power = 2
                toughness = 2
                stateTriggeredAbility {
                    condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Island"), negate = true)
                    effect = Effects.May(Effects.GainLife(1))
                }
            }
            cardRegistry.register(optional)
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Optional Lifecycle Beast")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            arm(game)
            repeat(2) { game.passPriority().error shouldBe null }
            game.state.pendingDecision shouldNotBe null
            game.state.pendingTriggers.shouldBeEmpty()
            game.answerYesNo(false).error shouldBe null
            game.getLifeTotal(1) shouldBe 20
            game.state.pendingDecision shouldBe null
            game.state.stack.size shouldBe 1
        }

        test("a state trigger with no legal modal mode ends its mandatory loop in a draw") {
            val modal = card("Modal Lifecycle Enchantment") {
                typeLine = "Enchantment"
                stateTriggeredAbility {
                    condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Island"), negate = true)
                    effect = Effects.Modal(listOf(
                        Mode.withTarget(Effects.Destroy(EffectTarget.ContextTarget(0)), TargetObject(filter = TargetFilter.Creature)),
                        Mode.withTarget(Effects.Exile(EffectTarget.ContextTarget(0)), TargetObject(filter = TargetFilter.Creature))
                    ))
                }
            }
            cardRegistry.register(modal)
            val game = scenario().withPlayers().withCardOnBattlefield(1, modal.name)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state = game.state.copy(pendingTriggers = poller.poll(game.state).pendingTriggers)
            val result = game.passPriority()
            result.error shouldBe null
            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe null
            game.state.pendingTriggers.shouldBeEmpty()
            game.state.stack.shouldBeEmpty()
            result.events.filterIsInstance<GameEndedEvent>().single().reason shouldBe GameEndReason.INFINITE_LOOP

            // A trigger captured before the condition changed is removed only once, not a loop.
            val recovered = scenario().withPlayers().withCardOnBattlefield(1, modal.name)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val island = recovered.findPermanent("Island")!!
            val withoutIsland = recovered.zones.moveToZone(recovered.state, island, Zone.GRAVEYARD).state
            recovered.state = recovered.state.copy(pendingTriggers = poller.poll(withoutIsland).pendingTriggers)
            recovered.passPriority().error shouldBe null
            recovered.state.gameOver shouldBe false
            recovered.state.pendingTriggers.shouldBeEmpty()
            recovered.state.stack.shouldBeEmpty()
        }

        test("ability removal suppresses printed state triggers") {
            val game = board()
            val source = game.findPermanent("Lifecycle Test Beast")!!
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.RemoveAllAbilities(EffectTarget.Self),
                com.wingedsheep.engine.handlers.EffectContext(source, game.player1Id)).state
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
        }

        test("printed state triggers read effective text rather than the original Island word") {
            val game = board()
            val source = game.findPermanent("Lifecycle Test Beast")!!
            game.state = game.state.updateEntity(source) { it.with(
                com.wingedsheep.engine.state.components.identity.TextReplacementComponent(listOf(
                    com.wingedsheep.engine.state.components.identity.TextReplacement(
                        "Island", "Mountain", com.wingedsheep.engine.state.components.identity.TextReplacementCategory.BASIC_LAND_TYPE
                    )
                ))
            ) }
            poller.poll(game.state).pendingTriggers.shouldBeEmpty()
            val mountain = game.findPermanent("Mountain")!!
            game.state = game.zones.moveToZone(game.state, mountain, Zone.GRAVEYARD).state
            poller.poll(game.state).pendingTriggers.size shouldBe 1
        }

        test("lifecycle identity survives a serialized game state") {
            val game = board()
            arm(game)
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            val encoded = json.encodeToString(game.state)
            val decoded = json.decodeFromString<GameState>(encoded)
            poller.poll(decoded).pendingTriggers.shouldBeEmpty()
            val removed = services.stackResolver.counterAbility(decoded, decoded.stack.single()).state
            poller.poll(removed).pendingTriggers.size shouldBe 1
        }
    }
}
