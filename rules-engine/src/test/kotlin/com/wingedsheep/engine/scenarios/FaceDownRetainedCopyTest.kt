package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class FaceDownRetainedCopyTest : FunSpec({
    val copier = card("Face Down Retained Copier") {
        manaCost = "{3}{U}{U}"
        typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        val upkeep = grantedTriggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            val creature = target(TargetFilter.Creature)
            effect = Effects.Composite(listOf(Effects.May(Effects.EachPermanentBecomesCopyOfTarget(
                target = creature, affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainColors = true, retainResolvingTriggeredAbility = true),
            ))))
        }
        replacementEffect(EntersAsCopy(exceptions = CopyExceptions(
            retainColors = true, addedTriggeredAbilities = listOf(upkeep))))
    }
    val cloneCard = card("Plain Face Down Entry Copier") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy())
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, cloneCard))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun enter(d: GameTestDriver, target: EntityId?): EntityId {
        val id = d.putCardInHand(d.player1, copier.name)
        d.giveMana(d.player1, Color.BLUE, 5)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOfNotNull(target)).error shouldBe null
        return id
    }
    fun ownUpkeep(d: GameTestDriver) {
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
    }
    fun recopy(d: GameTestDriver, target: EntityId, yes: Boolean = true) {
        d.submitTargetSelection(d.player1, listOf(target)).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, yes).error shouldBe null
    }
    test("entry copies mana cost stats and subtypes but keeps blue and gains the upkeep ability") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        val c = card(d, id)
        c.name shouldBe "Grizzly Bears"
        c.manaCost shouldBe card(d, bear).manaCost
        c.typeLine shouldBe card(d, bear).typeLine
        c.baseStats shouldBe card(d, bear).baseStats
        c.colors shouldBe setOf(Color.BLUE)
        c.copyTriggeredAbilities.size shouldBe 1
    }
    test("declining entry leaves a zero toughness creature that dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, null)
        (id in d.state.getZone(d.player1, Zone.GRAVEYARD)) shouldBe true
    }
    test("face-down entry produces a face-up unnamed blue 2 2 with only its upkeep ability") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) })
        val id = enter(d, source)
        val c = card(d, id)
        c.name shouldBe ""
        c.typeLine shouldBe TypeLine.parse("Creature")
        c.manaValue shouldBe 0
        c.colors shouldBe setOf(Color.BLUE)
        c.baseStats!!.basePower shouldBe 2
        c.baseStats!!.baseToughness shouldBe 2
        c.copyTriggeredAbilities.size shouldBe 1
        d.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe false
        d.state.getEntity(id)!!.has<MorphDataComponent>() shouldBe false
        ownUpkeep(d)
        recopy(d, source)
        card(d, id).name shouldBe ""
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
    test("upkeep chooses targets before resolution consent and declining preserves its current identity") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hill = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        ownUpkeep(d)
        recopy(d, hill, false)
        card(d, id).name shouldBe "Grizzly Bears"
        card(d, id).colors shouldBe setOf(Color.BLUE)
    }
    test("upkeep recopy of a face-down source survives a serialized resolution pause and can recopy again") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hidden = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        d.replaceState(d.state.updateEntity(hidden) { it.with(FaceDownComponent) })
        ownUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(hidden)).error shouldBe null
        d.bothPass().error shouldBe null
        val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitYesNo(d.player1, true).error shouldBe null
        card(d, id).name shouldBe ""
        card(d, id).colors shouldBe setOf(Color.BLUE)
        card(d, id).copyTriggeredAbilities.size shouldBe 1
        d.replaceState(d.state.updateEntity(hidden) { it.without<FaceDownComponent>() })
        card(d, id).name shouldBe ""
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        ownUpkeep(d)
        recopy(d, hidden)
        card(d, id).name shouldBe "Hill Giant"
        card(d, id).colors shouldBe setOf(Color.BLUE)
    }
    test("removed upkeep target fizzles without changing the copy") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hill = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        ownUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(hill)).error shouldBe null
        d.moveToGraveyard(hill)
        d.bothPass().error shouldBe null
        card(d, id).name shouldBe "Grizzly Bears"
        d.pendingDecision shouldBe null
    }
    test("self-copy adds another instance and cloneCard inherits the blue copy and both abilities") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        ownUpkeep(d)
        recopy(d, id)
        card(d, id).copyTriggeredAbilities.size shouldBe 2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val clone = d.putCardInHand(d.player1, cloneCard.name)
        d.giveMana(d.player1, Color.BLUE, 4)
        d.castSpell(d.player1, clone).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(id)).error shouldBe null
        card(d, clone).colors shouldBe setOf(Color.BLUE)
        card(d, clone).copyTriggeredAbilities.size shouldBe 2
    }
})
