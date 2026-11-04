package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.MetallicMimic
import com.wingedsheep.mtg.sets.definitions.xln.cards.ArcaneAdaptation
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ArcaneAdaptationScenarioTest : FunSpec({
    test("the entry choice applies across zones and ends when the enchantment leaves") {
        val bear = CardDefinition.creature(
            name = "Test Bear", manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2
        )
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArcaneAdaptation, bear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val myBear = driver.putCreatureOnBattlefield(me, "Test Bear")
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Test Bear")
        val handBear = driver.putCardInHand(me, "Test Bear")
        val graveyardBear = driver.putCardInGraveyard(me, "Test Bear")
        val exileBear = driver.putCardInExile(me, "Test Bear")
        val libraryBear = driver.putCardOnTopOfLibrary(me, "Test Bear")
        val theirGraveyardBear = driver.putCardInGraveyard(opponent, "Test Bear")
        val forest = driver.putCardInGraveyard(me, "Forest")
        val adaptation = driver.putCardInHand(me, "Arcane Adaptation")
        val projector = StateProjector()
        val evaluator = PredicateEvaluator(cardRegistry = null)
        fun hasType(id: EntityId, type: String): Boolean = evaluator.matchesCardPredicate(
            driver.state, projector.project(driver.state), id, CardPredicate.HasSubtype(Subtype(type))
        )

        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, adaptation)
        driver.bothPass()
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(me, OptionChosenResponse(decision.id, decision.options.indexOf("Goblin")))

        for (id in listOf(myBear, handBear, graveyardBear, exileBear, libraryBear)) {
            hasType(id, "Goblin") shouldBe true
            hasType(id, "Bear") shouldBe true
        }
        for (id in listOf(theirBear, theirGraveyardBear, forest, adaptation)) {
            hasType(id, "Goblin") shouldBe false
        }

        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, handBear)
        hasType(driver.state.stack.last(), "Goblin") shouldBe true
        driver.bothPass()

        driver.moveToGraveyard(adaptation)
        for (id in listOf(myBear, handBear, graveyardBear, exileBear, libraryBear)) {
            hasType(id, "Goblin") shouldBe false
            hasType(id, "Bear") shouldBe true
        }
    }

    test("the chosen type enables Metallic Mimic's entry counter only while Adaptation is present") {
        val bear = CardDefinition.creature(
            name = "Test Bear", manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2
        )
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArcaneAdaptation, MetallicMimic, bear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        fun chooseGoblin() {
            val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            val index = decision.options.indexOf("Goblin")
            require(index >= 0)
            driver.submitDecision(me, OptionChosenResponse(decision.id, index))
        }

        fun castBear(): EntityId {
            val id = driver.putCardInHand(me, "Test Bear")
            driver.giveMana(me, Color.GREEN, 2)
            driver.castSpell(me, id)
            driver.bothPass()
            return id
        }

        fun counters(id: EntityId): Int = driver.state.getEntity(id)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        val mimic = driver.putCardInHand(me, "Metallic Mimic")
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, mimic)
        driver.bothPass()
        chooseGoblin()

        val beforeAdaptation = castBear()
        counters(beforeAdaptation) shouldBe 0

        val adaptation = driver.putCardInHand(me, "Arcane Adaptation")
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, adaptation)
        driver.bothPass()
        chooseGoblin()

        val adaptedBear = castBear()
        counters(adaptedBear) shouldBe 1
        counters(beforeAdaptation) shouldBe 0
        val projected = StateProjector().project(driver.state)
        projected.hasSubtype(adaptedBear, "Bear") shouldBe true
        projected.hasSubtype(adaptedBear, "Goblin") shouldBe true
        projected.getPower(adaptedBear) shouldBe 3
        projected.getToughness(adaptedBear) shouldBe 3

        driver.moveToGraveyard(adaptation)
        val afterAdaptation = castBear()
        counters(afterAdaptation) shouldBe 0
        counters(adaptedBear) shouldBe 1
    }
})
