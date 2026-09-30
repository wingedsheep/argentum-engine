package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.chosenModeId
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.EntryCharacteristicsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * "As this creature enters, it becomes your choice of …" ([ModeOption.becomes] — Primal Clay,
 * Corrupted Shapeshifter). Such an ability sets copiable values (CR 707.2: "as … enters"
 * abilities that set power and toughness), so:
 *  - the chosen P/T, keywords and subtypes are the permanent's own base characteristics;
 *  - an object that becomes a copy of it is the chosen shape without choosing;
 *  - an object entering as a copy of it makes its own choice on top — abilities from both, the
 *    last-chosen P/T (the Corrupted Shapeshifter ruling);
 *  - the card that leaves the battlefield is its printed star/star self again (CR 400.7).
 */
class EntryCharacteristicsChoiceTest : FunSpec({
    val shifter = card("Test Entry Shifter") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(
            EntersWithChoice(
                ChoiceType.MODE,
                modeOptions = listOf(
                    ModeOption("flier", "3/3 with flying", becomes = EntryCharacteristics(3, 3, setOf(Keyword.FLYING))),
                    ModeOption(
                        "wall", "0/12 Wall with defender",
                        becomes = EntryCharacteristics(0, 12, setOf(Keyword.DEFENDER), subtypes = listOf("Wall")),
                    ),
                ),
            )
        )
    }
    val clone = card("Test Entry Clone") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy(optional = true))
    }
    val bounce = card("Test Entry Bounce") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { effect = Effects.ReturnToHand(target(TargetFilter.Creature)) }
    }
    val reanimate = card("Test Entry Reanimate") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { effect = Effects.PutOntoBattlefield(target(TargetFilter.CardInGraveyard)) }
    }
    val mimic = card("Test Entry Mimic") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val subject = target(TargetFilter.Creature)
            val model = target(TargetFilter.Creature)
            effect = Effects.EachPermanentBecomesCopyOfTarget(target = model, affected = subject)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(shifter, clone, bounce, reanimate, mimic))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.cast(name: String, targets: List<EntityId> = emptyList()): EntityId {
        val id = putCardInHand(player1, name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, id, targets).error shouldBe null
        bothPass()
        return id
    }
    fun GameTestDriver.choose(label: String): ExecutionResult {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        return submitDecision(player1, OptionChosenResponse(decision.id, decision.options.indexOf(label)))
    }
    fun GameTestDriver.castShifter(label: String): EntityId =
        cast(shifter.name).also { choose(label).error shouldBe null }
    fun GameTestDriver.card(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!
    fun GameTestDriver.roundTrip() {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        replaceState(json.decodeFromString<GameState>(json.encodeToString(state)))
    }

    test("the chosen option becomes the permanent's base power, toughness, keywords and subtypes") {
        val d = driver()
        val id = d.castShifter("0/12 Wall with defender")
        d.roundTrip()

        val projected = d.state.projectedState
        projected.getPower(id) shouldBe 0
        projected.getToughness(id) shouldBe 12
        projected.hasKeyword(id, Keyword.DEFENDER) shouldBe true
        projected.hasKeyword(id, Keyword.FLYING) shouldBe false
        projected.getSubtypes(id).contains("Wall") shouldBe true
        projected.getSubtypes(id).contains("Shapeshifter") shouldBe true
        // The choice is still recorded for anything that reads the mode.
        d.state.getEntity(id)!!.chosenModeId() shouldBe "wall"
    }

    test("the card that leaves the battlefield is its printed star/star self again") {
        val d = driver()
        val id = d.castShifter("3/3 with flying")
        d.cast(bounce.name, listOf(id))

        (id in d.getHand(d.player1)) shouldBe true
        val printed = d.card(id)
        printed.baseStats shouldBe com.wingedsheep.sdk.model.CreatureStats(0, 0)
        printed.baseKeywords shouldBe emptySet()
        d.state.getEntity(id)!!.has<EntryCharacteristicsComponent>() shouldBe false

        // Cast again, it chooses afresh.
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass()
        d.choose("0/12 Wall with defender").error shouldBe null
        d.state.projectedState.getToughness(id) shouldBe 12
        d.state.projectedState.hasKeyword(id, Keyword.FLYING) shouldBe false
    }

    test("an object that becomes a copy of it copies the chosen values without choosing") {
        val d = driver()
        val model = d.castShifter("3/3 with flying")
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.cast(mimic.name, listOf(bear, model))

        d.state.pendingDecision shouldBe null
        val projected = d.state.projectedState
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 3
        projected.hasKeyword(bear, Keyword.FLYING) shouldBe true
    }

    test("a copy entering as a copy makes its own choice on top of the copied one") {
        val d = driver()
        val model = d.castShifter("0/12 Wall with defender")
        val id = d.cast(clone.name)
        d.submitCardSelection(d.player1, listOf(model)).error shouldBe null
        d.choose("3/3 with flying").error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        val projected = d.state.projectedState
        // The ruling: "copying an 0/12 … with defender, you could make it a 3/3 … with flying and defender."
        projected.getPower(id) shouldBe 3
        projected.getToughness(id) shouldBe 3
        projected.hasKeyword(id, Keyword.FLYING) shouldBe true
        projected.hasKeyword(id, Keyword.DEFENDER) shouldBe true

        // Leaving undoes both the copy and the choice: the Clone card again.
        d.cast(bounce.name, listOf(id))
        d.card(id).name shouldBe clone.name
        d.card(id).baseKeywords shouldBe emptySet()
    }

    test("a permanent that chose, then became a copy of something else, leaves as its printed self") {
        val d = driver()
        val id = d.castShifter("3/3 with flying")
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.cast(mimic.name, listOf(id, bear))
        d.state.projectedState.getPower(id) shouldBe 2
        d.state.projectedState.hasKeyword(id, Keyword.FLYING) shouldBe false

        d.cast(bounce.name, listOf(id))
        d.card(id).name shouldBe shifter.name
        d.card(id).baseStats shouldBe com.wingedsheep.sdk.model.CreatureStats(0, 0)
        d.card(id).baseKeywords shouldBe emptySet()
    }

    test("an effect putting it onto the battlefield asks first and enters as the chosen shape") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, shifter.name)
        val spell = d.putCardInHand(d.player1, reanimate.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(
            d.player1, spell,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(id, d.player1, Zone.GRAVEYARD)),
        ).error shouldBe null
        d.bothPass()
        d.choose("3/3 with flying").error shouldBe null

        (id in d.state.getBattlefield()) shouldBe true
        d.state.projectedState.getPower(id) shouldBe 3
        d.state.projectedState.hasKeyword(id, Keyword.FLYING) shouldBe true
    }
})

