package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class RetainedColorsRecopyTest : FunSpec({
    val copier = card("Test Retained Color Copier") {
        manaCost = "{3}{U}{U}"
        typeLine = "Creature — Shapeshifter"
        power = 0; toughness = 0
        val recopy = grantedTriggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            val creature = target(TargetFilter.Creature)
            effect = Effects.Composite(listOf(Effects.May(Effects.EachPermanentBecomesCopyOfTarget(
                target = creature,
                affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainColors = true, retainResolvingTriggeredAbility = true),
            ))))
        }
        replacementEffect(EntersAsCopy(
            optional = true,
            exceptions = CopyExceptions(retainColors = true, addedTriggeredAbilities = listOf(recopy)),
        ))
    }
    val devoid = card("Test Vesuvan Devoid") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Eldrazi"
        power = 3; toughness = 4
        keywords(Keyword.DEVOID, Keyword.FLYING)
    }
    val indicator = card("Test Vesuvan Indicator") {
        colorIndicator = "G"
        typeLine = "Creature — Bear"
        power = 2; toughness = 2
    }
    val redWash = card("Test Vesuvan Red Wash") {
        manaCost = "{R}"; typeLine = "Instant"
        spell { val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
            effect = Effects.ChangeColor(t, setOf(Color.RED), com.wingedsheep.sdk.scripting.Duration.Permanent) }
    }
    val remove = card("Test Vesuvan Remove") {
        manaCost = "{B}"; typeLine = "Instant"
        spell { val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
            effect = Effects.Destroy(t) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copier, devoid, indicator, redWash, remove))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun enter(d: GameTestDriver, target: EntityId?): EntityId {
        val id = d.putCardInHand(d.player1, copier.name)
        d.giveMana(d.player1, Color.BLUE, 5)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, target?.let { listOf(it) } ?: emptyList()).error shouldBe null
        return id
    }
    fun upkeep(d: GameTestDriver) {
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
    }
    fun recopy(d: GameTestDriver, target: EntityId, accept: Boolean = true) {
        d.submitTargetSelection(d.player1, listOf(target)).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, accept).error shouldBe null
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    test("entry retains blue but copies mana cost and gives a targeted optional upkeep recopy") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        card(d, id).name shouldBe "Grizzly Bears"
        card(d, id).colors shouldBe setOf(Color.BLUE)
        card(d, id).manaCost shouldBe ManaCost.parse("{1}{G}")
        card(d, id).copyTriggeredAbilities.size shouldBe 1
        val target = d.putPermanentOnBattlefield(d.player2, devoid.name)
        upkeep(d)
        d.submitTargetSelection(d.player1, listOf(target)).error shouldBe null
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.bothPass().error shouldBe null
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitYesNo(d.player1, true).error shouldBe null
        card(d, id).name shouldBe devoid.name
        card(d, id).colors shouldBe setOf(Color.BLUE)
        card(d, id).baseKeywords shouldBe setOf(Keyword.FLYING)
        d.state.projectedState.getColors(id) shouldBe setOf("BLUE")
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
    test("entry omits a copied color indicator and devoid keyword") {
        for (name in listOf(indicator.name, devoid.name)) {
            val d = driver()
            val target = d.putPermanentOnBattlefield(d.player2, name)
            val id = enter(d, target)
            card(d, id).colors shouldBe setOf(Color.BLUE)
            (Keyword.DEVOID in card(d, id).baseKeywords) shouldBe false
        }
    }
    test("declining entry gives no upkeep ability and the zero toughness creature dies") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, null)
        (id in d.state.getBattlefield()) shouldBe false
        card(d, id).name shouldBe copier.name
        card(d, id).copyTriggeredAbilities shouldBe emptyList()
    }
    test("declining upkeep preserves the current identity and does not add a trigger") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        val target = d.putPermanentOnBattlefield(d.player2, devoid.name)
        upkeep(d)
        recopy(d, target, false)
        card(d, id).name shouldBe "Grizzly Bears"
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
    test("self copy accumulates distinct upkeep triggers") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        upkeep(d)
        recopy(d, id)
        card(d, id).copyTriggeredAbilities.size shouldBe 2
        card(d, id).copyTriggeredAbilities.map { it.id }.distinct().size shouldBe 2
        upkeep(d)
        repeat(2) { d.submitTargetSelection(d.player1, listOf(id)).error shouldBe null }
        d.state.stack.size shouldBe 2
        repeat(2) {
            d.bothPass().error shouldBe null
            d.submitYesNo(d.player1, true).error shouldBe null
        }
        card(d, id).copyTriggeredAbilities.size shouldBe 4
        card(d, id).colors shouldBe setOf(Color.BLUE)
    }
    test("ordinary Clone inherits the modified color and upkeep ability") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        val clone = d.putCardInHand(d.player1, "Clone")
        d.giveMana(d.player1, Color.BLUE, 4)
        d.castSpell(d.player1, clone).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(id)).error shouldBe null
        card(d, clone).colors shouldBe setOf(Color.BLUE)
        card(d, clone).copyTriggeredAbilities shouldBe card(d, id).copyTriggeredAbilities
    }
    test("a color effect remains live but is not baked into copiable colors on upkeep recopy") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        val spell = d.putCardInHand(d.player1, redWash.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(
            com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(id))).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.projectedState.getColors(id) shouldBe setOf("RED")
        upkeep(d)
        recopy(d, bear)
        card(d, id).colors shouldBe setOf(Color.BLUE)
        d.state.projectedState.getColors(id) shouldBe setOf("RED")
    }
    test("an upkeep target destroyed before resolution fizzles and does not add text") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val id = enter(d, bear)
        val target = d.putPermanentOnBattlefield(d.player2, devoid.name)
        upkeep(d)
        d.submitTargetSelection(d.player1, listOf(target)).error shouldBe null
        val spell = d.putCardInHand(d.player1, remove.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(
            com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(target))).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.state.pendingDecision shouldBe null
        card(d, id).name shouldBe "Grizzly Bears"
        card(d, id).copyTriggeredAbilities.size shouldBe 1
    }
})
