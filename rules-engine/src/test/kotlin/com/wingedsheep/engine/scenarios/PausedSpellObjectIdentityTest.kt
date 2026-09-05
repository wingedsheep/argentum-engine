package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class PausedSpellObjectIdentityTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    for (destination in listOf("graveyard", "flashback", "replacement")) {
        test("nested serialized spell choices retain stack object and finalize once: $destination") {
            val spell = card("Identity Paused Spell $destination") {
                manaCost = "{0}"; typeLine = "Sorcery"
                spell {
                    effect = Effects.Composite(
                        GatedEffect(Gate.MayDecide("First?"), Effects.Composite(
                            Effects.GainLife(1),
                            GatedEffect(Gate.MayDecide("Second?"), Effects.GainLife(2)))),
                        Effects.GainLife(4))
                }
                if (destination == "flashback") keywordAbility(KeywordAbility.flashback("{0}"))
                if (destination == "replacement") replacementEffect(RedirectZoneChange(
                    newDestination = Zone.EXILE,
                    appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD), selfOnly = true))
            }
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + spell)
                initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val id = if (destination == "flashback") d.putCardInGraveyard(d.player1, spell.name)
                else d.putCardInHand(d.player1, spell.name)
            val lifeBefore = d.getLifeTotal(d.player1)
            d.submit(CastSpell(d.player1, id, paymentStrategy = PaymentStrategy.FromPool,
                useAlternativeCost = destination == "flashback")).error shouldBe null
            val stackRef = d.state.objectRef(id)!!
            d.bothPass().error shouldBe null
            repeat(2) {
                (d.pendingDecision != null) shouldBe true
                d.state.stack.count { it == id } shouldBe 1
                d.state.objectRef(id) shouldBe stackRef
                d.state.logicalZone(id)?.zoneType shouldBe Zone.STACK
                d.events.filterIsInstance<ZoneChangeEvent>().count { it.entityId == id && it.fromZone == Zone.STACK } shouldBe 0
                d.replaceState(json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state)))
                d.submitYesNo(d.player1, true).error shouldBe null
            }
            d.pendingDecision shouldBe null
            d.state.stack.count { it == id } shouldBe 0
            d.getLifeTotal(d.player1) shouldBe lifeBefore + 7
            val expectedZone = if (destination == "graveyard") Zone.GRAVEYARD else Zone.EXILE
            (id in d.state.getZone(ZoneKey(d.player1, expectedZone))) shouldBe true
            val move = d.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == id && it.fromZone == Zone.STACK }
            move.oldObject shouldBe stackRef
            move.newObject shouldBe d.state.objectRef(id)
            move.toZone shouldBe expectedZone
            d.state.isCurrentObject(stackRef) shouldBe false
        }
    }
})
