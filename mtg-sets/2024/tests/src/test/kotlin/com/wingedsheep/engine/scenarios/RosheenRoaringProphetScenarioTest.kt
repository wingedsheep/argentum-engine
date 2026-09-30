package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.RosheenRoaringProphet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rosheen, Roaring Prophet — "When Rosheen enters, mill six cards. You may put a card with {X} in its
 * mana cost from among them into your hand. / {T}: Reveal any number of cards with {X} in their mana
 * cost in your hand. Add {C}{C} for each card revealed this way. Spend this mana only on costs that
 * contain {X}."
 */
class RosheenRoaringProphetScenarioTest : FunSpec({

    val xTrinket = CardDefinition.artifact(name = "Rosheen X Trinket", manaCost = ManaCost.parse("{X}"))
    val plainTrinket = CardDefinition.artifact(name = "Rosheen Plain Trinket", manaCost = ManaCost.parse("{2}"))
    val xEngine = card("Rosheen X Engine") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{X}: You gain X life."
        activatedAbility {
            cost = Costs.Mana("{X}")
            effect = Effects.GainLife(DynamicAmount.XValue)
            timing = TimingRule.InstantSpeed
        }
    }
    val plainEngine = card("Rosheen Plain Engine") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{2}: You gain 2 life."
        activatedAbility {
            cost = Costs.Mana("{2}")
            effect = Effects.GainLife(2)
            timing = TimingRule.InstantSpeed
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RosheenRoaringProphet, xTrinket, plainTrinket, xEngine, plainEngine))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val manaAbilityId = RosheenRoaringProphet.activatedAbilities.single().id

    fun GameTestDriver.pool(playerId: EntityId) = state.getEntity(playerId)!!.get<ManaPoolComponent>()!!

    /** Put Rosheen onto the battlefield, tap it, and reveal [reveal] from hand. */
    fun GameTestDriver.tapRosheen(playerId: EntityId, reveal: List<EntityId>) {
        val rosheen = putCreatureOnBattlefield(playerId, "Rosheen, Roaring Prophet")
        removeSummoningSickness(rosheen)
        val result = submit(ActivateAbility(playerId, rosheen, manaAbilityId))
        withClue("error=${result.error}") { result.error shouldBe null }
        if (pendingDecision != null) {
            pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            submitCardSelection(playerId, reveal)
        }
    }

    test("enters: mills six and may return a card with {X} in its mana cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val xCard = driver.putCardOnTopOfLibrary(me, "Rosheen X Trinket")
        repeat(5) { driver.putCardOnTopOfLibrary(me, "Rosheen Plain Trinket") }
        val rosheen = driver.putCardInHand(me, "Rosheen, Roaring Prophet")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)

        driver.submitSuccess(CastSpell(me, rosheen, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass() // Rosheen resolves
        driver.bothPass() // enters trigger resolves → selection
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain xCard
        driver.submitCardSelection(me, listOf(xCard))

        driver.getHand(me) shouldContain xCard
        driver.getGraveyard(me).count { driver.state.getEntity(it)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name == "Rosheen Plain Trinket" } shouldBe 5
        driver.getGraveyard(me) shouldNotContain xCard
    }

    test("tapping adds {C}{C} per revealed X card, restricted to costs that contain {X}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val a = driver.putCardInHand(me, "Rosheen X Trinket")
        val b = driver.putCardInHand(me, "Rosheen X Trinket")
        driver.tapRosheen(me, listOf(a, b))

        val pool = driver.pool(me)
        pool.restrictedMana.size shouldBe 4
        pool.restrictedMana.all { it.restriction == ManaRestriction.CostsContainingXOnly } shouldBe true
    }

    test("the mana pays for a spell with {X} in its mana cost but not for one without") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val xCard = driver.putCardInHand(me, "Rosheen X Trinket")
        val plain = driver.putCardInHand(me, "Rosheen Plain Trinket")
        driver.tapRosheen(me, listOf(xCard))

        driver.submit(CastSpell(me, plain, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldNotBe Outcome.Done
        val result = driver.submit(CastSpell(me, xCard, xValue = 2, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.pool(me).restrictedMana.size shouldBe 0
    }

    test("the mana pays for an activated ability whose cost contains {X}, not one without") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val xEnginePerm = driver.putPermanentOnBattlefield(me, "Rosheen X Engine")
        val plainEnginePerm = driver.putPermanentOnBattlefield(me, "Rosheen Plain Engine")
        val xCard = driver.putCardInHand(me, "Rosheen X Trinket")
        driver.tapRosheen(me, listOf(xCard))
        val lifeBefore = driver.getLifeTotal(me)

        driver.submit(ActivateAbility(me, plainEnginePerm, plainEngine.activatedAbilities.single().id))
            .outcome shouldNotBe Outcome.Done
        driver.submitSuccess(ActivateAbility(me, xEnginePerm, xEngine.activatedAbilities.single().id, xValue = 2))
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 2
    }

    test("revealing nothing adds no mana") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCardInHand(me, "Rosheen X Trinket")
        driver.tapRosheen(me, emptyList())
        driver.pool(me).restrictedMana.size shouldBe 0
    }
})
