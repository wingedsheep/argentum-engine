package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
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
import com.wingedsheep.sdk.scripting.filters.unified.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A permanent spell that enters as a copy makes the copied card's own "as this enters" choices and
 * applies its entry replacements before it enters (CR 614.12 reads the permanent as it would exist
 * on the battlefield, after the copy replacement). The original's choices are not copiable values.
 */
class CopiedEntryChoicesTest : FunSpec({
    val clone = card("Entry Choice Clone") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy(optional = true))
    }
    val enchantmentCopier = card("Entry Choice Enchantment Copier") {
        manaCost = "{U}"; typeLine = "Enchantment"
        replacementEffect(EntersAsCopy(copyFilter = GameObjectFilter.Enchantment))
    }
    val colorAndType = card("Color And Type Chooser") {
        manaCost = "{G}"; typeLine = "Creature — Elf"; power = 2; toughness = 2
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
        replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE, allowedCreatureTypes = listOf("Elf", "Goblin")))
        triggeredAbility { trigger = Triggers.self.enters(); effect = Effects.GainLife(1) }
    }
    val lifeOnEntry = card("Life On Entry") {
        manaCost = "{W}"; typeLine = "Creature — Cleric"; power = 1; toughness = 1
        replacementEffect(OnEnterRun(Effects.GainLife(3)))
    }
    val devourer = card("Entry Devourer") {
        manaCost = "{R}"; typeLine = "Creature — Dragon"; power = 1; toughness = 1
        replacementEffect(EntersWithDevour(multiplier = 2))
    }
    val landTypeAura = card("Land Type Entry Aura") {
        manaCost = "{U}"; typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(clone, enchantmentCopier, colorAndType, lifeOnEntry, devourer, landTypeAura))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.cast(name: String): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, id).error shouldBe null
        bothPass()
        return id
    }
    fun GameTestDriver.roundTrip() {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        replaceState(json.decodeFromString<GameState>(json.encodeToString(state)))
    }
    fun GameTestDriver.chooseColor(color: Color) =
        submitDecision(player1, ColorChosenResponse(state.pendingDecision!!.id, color))
    fun GameTestDriver.chooseOption(label: String): ExecutionResult {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        return submitDecision(player1, OptionChosenResponse(decision.id, decision.options.indexOf(label)))
    }

    test("copying a permanent with as-enters choices makes fresh choices of its own before entering") {
        val d = driver()
        val original = d.putPermanentOnBattlefield(d.player2, colorAndType.name)
        d.replaceState(d.state.updateEntity(original) {
            it.withCastChoice(ChoiceSlot.COLOR, ChoiceValue.ColorChoice(Color.GREEN))
        })
        val id = d.cast(clone.name)
        d.submitCardSelection(d.player1, listOf(original)).error shouldBe null

        d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        (id in d.state.getBattlefield()) shouldBe false
        d.roundTrip()
        d.chooseColor(Color.RED).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe false
        val entered = d.chooseOption("Goblin")
        entered.error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        val permanent = d.state.getEntity(id)!!
        permanent.chosenColor() shouldBe Color.RED
        permanent.chosenCreatureType() shouldBe "Goblin"
        d.state.getEntity(original)!!.chosenColor() shouldBe Color.GREEN
        val entry = entered.events.filterIsInstance<ZoneChangeEvent>()
            .single { it.entityId == id && it.toZone == Zone.BATTLEFIELD }
        entry.copyOfOriginalName shouldBe clone.name
        entered.events.filterIsInstance<ResolvedEvent>().count { it.entityId == id } shouldBe 1

        // The copied enters trigger fires once, after the choices are made.
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("declining the copy enters as itself without the copied choices") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, colorAndType.name)
        val id = d.cast(clone.name)
        val result = d.submitCardSelection(d.player1, emptyList())
        result.error shouldBe null
        d.state.pendingDecision shouldBe null
        // The 0/0 enters and dies to state-based actions; it never asked for a color.
        result.events.filterIsInstance<ZoneChangeEvent>()
            .single { it.entityId == id && it.toZone == Zone.BATTLEFIELD }.copyOfOriginalName shouldBe null
    }

    test("a copied as-this-enters effect runs on the copy") {
        val d = driver()
        val original = d.putPermanentOnBattlefield(d.player2, lifeOnEntry.name)
        val id = d.cast(clone.name)
        d.submitCardSelection(d.player1, listOf(original)).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe true
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("a copied devour asks what to sacrifice before the copy enters") {
        val d = driver()
        val original = d.putPermanentOnBattlefield(d.player2, devourer.name)
        val food = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val id = d.cast(clone.name)
        d.submitCardSelection(d.player1, listOf(original)).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe false
        d.submitCardSelection(d.player1, listOf(food)).error shouldBe null
        (food in d.state.getGraveyard(d.player1)) shouldBe true
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.state.projectedState.getPower(id) shouldBe 3
    }

    test("a copied Aura chooses its host, then the copied as-enters choice, then enters attached") {
        val d = driver()
        val host = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.putPermanentOnBattlefield(d.player2, landTypeAura.name)
        d.replaceState(AttachmentMover.attach(d.state, source, host, d.player2).first)
        val id = d.cast(enchantmentCopier.name)
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(host)).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe false
        d.roundTrip()
        d.chooseOption("Swamp").error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(id)!!.get<AttachedToComponent>()!!.targetId shouldBe host
        d.state.getEntity(id)!!.chosenLandType() shouldBe "Swamp"
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe landTypeAura.name
    }
})
