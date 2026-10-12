package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ActivateAbility
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
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class VesuvanDoppelgangerScenarioTest : FunSpec({
    val copier = com.wingedsheep.mtg.sets.definitions.lea.cards.VesuvanDoppelganger
    val cloneCard = card("Doppelganger Plain Entry Copier") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy())
    }
    fun flipCard(front: String, back: String, n: Int) = com.wingedsheep.sdk.model.CardDefinition.flipCard(
        card(front) {
            manaCost = "{G}"; typeLine = "Creature — Human"; power = 1; toughness = 2
            activatedAbility { cost = Costs.Free; effect = Effects.Flip() }
        },
        card(back) { typeLine = "Creature — Wizard"; power = n; toughness = n },
    )
    val flipA = flipCard("Vesuvan Student", "Vesuvan Teacher", 3)
    val flipB = flipCard("Vesuvan Pupil", "Vesuvan Professor", 4)
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, cloneCard, flipA, flipB))
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
    /**
     * Answers the upkeep trigger. The "you may" is answered at targeting: choosing a target is the
     * yes (no second prompt at resolution); `null` submits an empty selection, which declines and
     * keeps the ability off the stack.
     */
    fun recopy(d: GameTestDriver, target: EntityId?) {
        d.submitTargetSelection(d.player1, listOfNotNull(target)).error shouldBe null
        if (target != null) d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
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
    test("declining the upkeep recopy at the target prompt preserves its current identity") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hill = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        ownUpkeep(d)
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            .targetRequirements.single().minTargets shouldBe 0
        (hill in d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets[0].orEmpty()) shouldBe true
        recopy(d, null)
        d.state.stack.isEmpty() shouldBe true
        card(d, id).name shouldBe "Grizzly Bears"
        card(d, id).colors shouldBe setOf(Color.BLUE)
    }
    test("upkeep recopy of a face-down source survives a serialized round trip on the stack and can recopy again") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hidden = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        d.replaceState(d.state.updateEntity(hidden) { it.with(FaceDownComponent) })
        ownUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(hidden)).error shouldBe null
        // The consent answered at targeting has to survive serialization of the stacked ability.
        val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
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

    test("entry copies either source status unflipped and retains blue and upkeep after flipping") {
        for (flipped in listOf(false, true)) {
            val d = driver()
            val source = d.putPermanentOnBattlefield(d.player1, flipA.name)
            if (flipped) {
                d.submit(ActivateAbility(d.player1, source, flipA.activatedAbilities.single().id)).error shouldBe null
                d.bothPass().error shouldBe null
            }
            val id = enter(d, source)
            card(d, id).name shouldBe flipA.name
            d.submit(ActivateAbility(d.player1, id, flipA.activatedAbilities.single().id)).error shouldBe null
            d.bothPass().error shouldBe null
            card(d, id).name shouldBe flipA.flipSide!!.name
            card(d, id).colors shouldBe setOf(Color.BLUE)
            card(d, id).copyTriggeredAbilities.size shouldBe 1
            ownUpkeep(d)
            recopy(d, source)
            card(d, id).name shouldBe flipA.flipSide!!.name
            card(d, id).copyTriggeredAbilities.size shouldBe 1
        }
    }
    test("flipped upkeep recopy selects the new alternative after an ordinary creature and a serialized round trip") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, flipA.name)
        val id = enter(d, source)
        d.submit(ActivateAbility(d.player1, id, flipA.activatedAbilities.single().id)).error shouldBe null
        d.bothPass().error shouldBe null
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        ownUpkeep(d)
        recopy(d, bear)
        card(d, id).name shouldBe "Grizzly Bears"
        d.state.getEntity(id)!!.has<FlippedComponent>() shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val next = d.putPermanentOnBattlefield(d.player2, flipB.name)
        ownUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(next)).error shouldBe null
        val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        card(d, id).name shouldBe flipB.flipSide!!.name
        card(d, id).colors shouldBe setOf(Color.BLUE)
        card(d, id).copyTriggeredAbilities.size shouldBe 1
        d.state.projectedState.getPower(id) shouldBe 4
        val moved = com.wingedsheep.engine.handlers.effects.ZoneTransitionService(d.cardRegistry,
            com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = d.cardRegistry))
            .moveToZone(d.state, id, Zone.GRAVEYARD)
        d.replaceState(moved.state)
        card(d, id).name shouldBe copier.name
        d.state.getEntity(id)!!.has<FlippedComponent>() shouldBe false
    }
    test("self-copy instances answer consent independently at targeting before priority") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hill = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        ownUpkeep(d)
        recopy(d, id)
        card(d, id).copyTriggeredAbilities.size shouldBe 2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        ownUpkeep(d)
        // First instance takes Hill Giant (yes); the second declines with an empty selection.
        d.submitTargetSelection(d.player1, listOf(hill)).error shouldBe null
        d.submitTargetSelection(d.player1, emptyList()).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.stack.size shouldBe 1
        card(d, id).name shouldBe "Grizzly Bears"
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        card(d, id).name shouldBe "Hill Giant"
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
    test("leaving before upkeep resolution does not copy onto its graveyard incarnation") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hill = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        ownUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(hill)).error shouldBe null
        val moved = com.wingedsheep.engine.handlers.effects.ZoneTransitionService(d.cardRegistry,
            com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = d.cardRegistry))
            .moveToZone(d.state, id, Zone.GRAVEYARD)
        d.replaceState(moved.state)
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        card(d, id).name shouldBe copier.name
        (id in d.state.getZone(d.player1, Zone.GRAVEYARD)) shouldBe true
    }

    test("leaving and returning before upkeep resolution does not offer the old source copy choice") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val hill = d.putPermanentOnBattlefield(d.player2, "Hill Giant")
        val id = enter(d, bear)
        ownUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(hill)).error shouldBe null
        val zones = com.wingedsheep.engine.handlers.effects.ZoneTransitionService(d.cardRegistry,
            com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = d.cardRegistry))
        val identity = card(d, id)
        d.replaceState(zones.moveToZone(d.state, id, Zone.EXILE).state)
        d.replaceState(zones.moveToZone(d.state, id, Zone.BATTLEFIELD).state)
        // Give the new visit a stable identity without an entry choice in this departure fixture.
        d.replaceState(d.state.updateEntity(id) { it.with(identity) })
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        card(d, id).name shouldBe "Grizzly Bears"
    }

})
