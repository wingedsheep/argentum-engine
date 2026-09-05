package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PendingTriggersContinuation
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.mechanics.sba.permanent.BattleDefenseCheck
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class SiegePendingObjectIdentityTest : FunSpec({
    val siegeCard = card("Identity Siege") { manaCost = "{1}"; typeLine = "Battle — Siege"; startingDefense = 3 }
    val legend = card("Identity Legendary Witness") { manaCost = "{1}"; typeLine = "Legendary Creature — Human"; power = 1; toughness = 1 }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(siegeCard, legend))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun putSiege(d: GameTestDriver): EntityId {
        val id = d.putPermanentOnBattlefield(d.player1, siegeCard.name)
        d.replaceState(d.state.updateEntity(id) {
            it.with(CountersComponent(mapOf(com.wingedsheep.sdk.core.CounterType.DEFENSE to 3)))
                .with(com.wingedsheep.engine.state.components.battlefield.ProtectorComponent(d.player2))
        })
        return id
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    fun roundTrip(d: GameTestDriver) = d.replaceState(json.decodeFromString(GameState.serializer(),
        json.encodeToString(GameState.serializer(), d.state)))

    for (pause in listOf("none", "resolution", "sba")) {
        test("ordinary counter removal keeps the exact Siege pending through $pause pause") {
            val d = driver()
            val siege = putSiege(d)
            val original = d.state.objectRef(siege)!!
            val removal = card("Identity Remove Defense") {
                manaCost = "{0}"; typeLine = "Sorcery"
                spell {
                    effect = Effects.Composite(
                        Effects.RemoveCounters(Counters.DEFENSE, 3, EffectTarget.SpecificEntity(siege)),
                        if (pause == "resolution") GatedEffect(Gate.MayDecide("Continue?"), Effects.GainLife(1))
                        else Effects.GainLife(1))
                }
            }
            d.registerCards(listOf(removal))
            val spell = d.putCardInHand(d.player1, removal.name)
            d.castSpell(d.player1, spell).error shouldBe null
            if (pause == "sba") {
                d.putCreatureOnBattlefield(d.player1, legend.name)
                d.putCreatureOnBattlefield(d.player1, legend.name)
            }
            d.bothPass().error shouldBe null
            if (pause != "none") {
                (d.pendingDecision != null) shouldBe true
                roundTrip(d)
                if (pause == "resolution") d.submitYesNo(d.player1, true).error shouldBe null
                else d.autoResolveDecision()
            }
            d.state.isCurrentObject(original) shouldBe true
            (siege in d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))) shouldBe true
            d.stackSize shouldBe 1
            roundTrip(d)
            d.bothPass().error shouldBe null
            (siege in d.state.getZone(ZoneKey(d.player1, Zone.EXILE))) shouldBe true
            (siege in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe false
        }
    }

    for (storage in listOf("staged", "paused", "stack")) {
        for (stale in listOf(false, true)) {
            test("any $storage trigger protects only its current Siege origin; stale=$stale") {
                val d = driver()
                val siege = putSiege(d)
                val original = d.state.objectRef(siege)!!
                val refs = ObjectReferenceEnvironment(captured = true, origin = original, source = original)
                var state = d.state.updateEntity(siege) { it.with(CountersComponent()) }
                val trigger = PendingTrigger(TriggeredAbility(id = com.wingedsheep.sdk.scripting.AbilityId.generate(), trigger = Triggers.EntersBattlefield.event,
                    effect = Effects.GainLife(1)), siege, siegeCard.name,
                    objectReferences = refs, controllerId = d.player1, triggerContext = TriggerContext())
                val stackId = EntityId.generate()
                if (storage == "paused") state = state.pushContinuation(PendingTriggersContinuation("waiting", listOf(trigger)))
                if (storage == "stack") state = state.withEntity(stackId, ComponentContainer.EMPTY.with(
                    TriggeredAbilityOnStackComponent(sourceId = siege, sourceName = siegeCard.name,
                        controllerId = d.player1, effect = Effects.GainLife(1), description = "unrelated trigger",
                        objectReferences = refs))).pushToStack(stackId)
                if (stale) state = state.moveToZone(siege, ZoneKey(d.player1, Zone.BATTLEFIELD), ZoneKey(d.player1, Zone.EXILE))
                    .moveToZone(siege, ZoneKey(d.player1, Zone.EXILE), ZoneKey(d.player1, Zone.BATTLEFIELD))
                state = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), state))
                val check = BattleDefenseCheck()
                val result = check.check(state, state, if (storage == "staged") setOf(original) else emptySet())
                (siege in result.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))) shouldBe !stale
                // Once the trigger has left the stack (or a pending trigger is declined), its
                // source gets no lingering reprieve on the next state-based check.
                if (!stale) {
                    val cleared = result.state.copy(stack = emptyList(), continuationStack = emptyList())
                    val after = check.check(cleared).state
                    (siege in after.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true
                }
            }
        }
    }
})
