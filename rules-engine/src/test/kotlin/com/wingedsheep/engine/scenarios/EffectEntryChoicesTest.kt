package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * "As this enters, choose …" (CR 614.12) for a permanent an *effect* puts onto the battlefield —
 * reanimation, a blink's return, a library search — and for a land played as a copy. The choice is
 * made before the permanent enters, of the card it enters as (the copied card when it copies), and
 * its enters triggers see the answer.
 */
class EffectEntryChoicesTest : FunSpec({
    val typeChooser = card("Entry Type Chooser") {
        manaCost = "{G}"; typeLine = "Creature — Elf"; power = 2; toughness = 2
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
        replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE, allowedCreatureTypes = listOf("Elf", "Goblin")))
        triggeredAbility { trigger = Triggers.self.enters(); effect = Effects.GainLife(1) }
    }
    val bodyguard = card("Entry Creature Chooser") {
        manaCost = "{W}"; typeLine = "Creature — Human"; power = 1; toughness = 1
        replacementEffect(EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD))
    }
    val colorLand = card("Entry Color Land") {
        typeLine = "Land"
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
    }
    val landCopier = card("Entry Land Copier") {
        typeLine = "Land"
        replacementEffect(EntersAsCopy(optional = true, copyFilter = GameObjectFilter.Land))
    }
    val reanimate = card("Entry Test Reanimate") {
        manaCost = "{B}"; typeLine = "Sorcery"
        spell { val t = target(TargetFilter.CreatureInYourGraveyard); effect = Effects.PutOntoBattlefieldFromGraveyard(t) }
    }
    val search = card("Entry Test Search") {
        manaCost = "{G}"; typeLine = "Sorcery"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land, count = 2, destination = SearchDestination.BATTLEFIELD
            )
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(typeChooser, bodyguard, colorLand, landCopier, reanimate, search))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.roundTrip() {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        replaceState(json.decodeFromString<GameState>(json.encodeToString(state)))
    }
    fun GameTestDriver.chooseColor(color: Color) =
        submitDecision(player1, ColorChosenResponse(state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>().id, color))
    fun GameTestDriver.chooseOption(label: String): ExecutionResult {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        return submitDecision(player1, OptionChosenResponse(decision.id, decision.options.indexOf(label)))
    }
    fun GameTestDriver.castReanimate(target: EntityId) {
        val spell = putCardInHand(player1, reanimate.name)
        giveMana(player1, Color.BLACK, 1)
        castSpellWithTargets(player1, spell, listOf(ChosenTarget.Card(target, player1, Zone.GRAVEYARD))).error shouldBe null
        bothPass()
    }

    test("a reanimated permanent makes its as-enters choices before it enters, then its enters trigger fires once") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, typeChooser.name)
        d.castReanimate(id)

        (id in d.state.getGraveyard(d.player1)) shouldBe true
        d.roundTrip()
        d.chooseColor(Color.RED).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe false
        val entered = d.chooseOption("Goblin")
        entered.error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.chosenColor() shouldBe Color.RED
        d.state.getEntity(id)!!.chosenCreatureType() shouldBe "Goblin"
        entered.events.filterIsInstance<ZoneChangeEvent>()
            .count { it.entityId == id && it.toZone == Zone.BATTLEFIELD } shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("a choice with nothing to choose from is skipped and the permanent still enters") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, bodyguard.name)
        d.castReanimate(id)
        d.state.pendingDecision shouldBe null
        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.chosenCreatureRef() shouldBe null
    }

    test("the creature choice is made from the pre-entry battlefield") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = d.putCardInGraveyard(d.player1, bodyguard.name)
        d.castReanimate(id)
        val choice = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.options shouldBe listOf(bear)
        d.submitCardSelection(d.player1, listOf(bear)).error shouldBe null
        d.state.getEntity(id)!!.chosenCreatureRef() shouldBe bear
    }

    test("every card a search puts onto the battlefield makes its own choice") {
        val d = driver()
        val first = d.putCardOnTopOfLibrary(d.player1, colorLand.name)
        val second = d.putCardOnTopOfLibrary(d.player1, colorLand.name)
        val spell = d.putCardInHand(d.player1, search.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, listOf(first, second)).error shouldBe null

        d.chooseColor(Color.WHITE).error shouldBe null
        (first in d.state.getBattlefield()) shouldBe false
        d.roundTrip()
        d.chooseColor(Color.BLACK).error shouldBe null

        (first in d.state.getBattlefield()) shouldBe true
        (second in d.state.getBattlefield()) shouldBe true
        setOf(d.state.getEntity(first)!!.chosenColor(), d.state.getEntity(second)!!.chosenColor()) shouldBe
            setOf(Color.WHITE, Color.BLACK)
    }

    test("a land played as a copy makes the copied land's choice, and its entry names the copy") {
        val d = driver()
        val original = d.putPermanentOnBattlefield(d.player2, colorLand.name)
        d.replaceState(d.state.updateEntity(original) { it.withCastChoice(ChoiceSlot.COLOR, ChoiceValue.ColorChoice(Color.GREEN)) })
        val id = d.putCardInHand(d.player1, landCopier.name)
        d.playLand(d.player1, id).error shouldBe null
        d.submitCardSelection(d.player1, listOf(original)).error shouldBe null

        d.roundTrip()
        val entered = d.chooseColor(Color.BLUE)
        entered.error shouldBe null
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe colorLand.name
        d.state.getEntity(id)!!.chosenColor() shouldBe Color.BLUE
        d.state.getEntity(original)!!.chosenColor() shouldBe Color.GREEN
        entered.events.filterIsInstance<ZoneChangeEvent>()
            .single { it.entityId == id && it.toZone == Zone.BATTLEFIELD }.copyOfOriginalName shouldBe landCopier.name
    }
})
