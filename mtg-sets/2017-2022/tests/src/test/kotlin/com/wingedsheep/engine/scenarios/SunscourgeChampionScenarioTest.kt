package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.SunscourgeChampion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sunscourge Champion (HOU #26) — {2}{W} 2/3.
 *
 *   When this creature enters, you gain life equal to its power.
 *   Eternalize—{2}{W}{W}, Discard a card.
 *
 * Pins the eternalize cost's non-mana part: the discard is paid with the mana and the self-exile,
 * the ability can't be activated with no card to discard, and it is still an eternalize ability.
 * The 4/4 token copies the enters trigger and gains 4.
 */
class SunscourgeChampionScenarioTest : FunSpec({

    val eternalize = SunscourgeChampion.activatedAbilities.single()

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SunscourgeChampion))
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the eternalize ability is marked as eternalize and costs a discard") {
        eternalize.keyword shouldBe Keyword.ETERNALIZE
        (eternalize.cost as AbilityCost.Composite).costs shouldContain Costs.DiscardCard
    }

    test("casting it gains life equal to its power") {
        val d = driver()
        val me = d.player1
        val champion = d.putCardInHand(me, "Sunscourge Champion")
        d.giveMana(me, Color.WHITE, 1)
        d.giveColorlessMana(me, 2)
        d.castSpell(me, champion).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(me) shouldBe 22
    }

    test("eternalize discards a card, exiles the champion and makes a 4/4 black Zombie that gains 4") {
        val d = driver()
        val me = d.player1
        val champion = d.putCardInGraveyard(me, "Sunscourge Champion")
        val fodder = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.WHITE, 2)
        d.giveColorlessMana(me, 2)

        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = champion,
                abilityId = eternalize.id,
                costPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
            )
        ).outcome shouldBe Outcome.Done

        withClue("the discard and the self-exile are paid as costs") {
            d.state.getGraveyard(me) shouldContain fodder
            d.state.getZone(me, Zone.EXILE) shouldContain champion
        }
        d.bothPass()
        val token = d.findPermanent(me, "Sunscourge Champion").shouldNotBeNull()
        d.state.projectedState.getPower(token) shouldBe 4
        d.state.projectedState.getToughness(token) shouldBe 4
        d.state.projectedState.getColors(token) shouldBe setOf(Color.BLACK.name)
        d.state.projectedState.hasSubtype(token, "Zombie") shouldBe true

        d.bothPass()
        withClue("the token's copied enters trigger gains its power") {
            d.getLifeTotal(me) shouldBe 24
        }
    }

    test("eternalize is offered with a card in hand and not with an empty hand") {
        val d = driver()
        val me = d.player1
        d.getHand(me).forEach(d::moveToGraveyard)
        val champion = d.putCardInGraveyard(me, "Sunscourge Champion")
        d.giveMana(me, Color.WHITE, 2)
        d.giveColorlessMana(me, 2)
        fun offered() = d.legalActions(me).any { (it.action as? ActivateAbility)?.sourceId == champion }

        withClue("an empty hand can't pay the discard") { offered() shouldBe false }
        val fodder = d.putCardInHand(me, "Grizzly Bears")
        withClue("one card in hand makes it activatable") { offered() shouldBe true }
        d.moveToGraveyard(fodder)

        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = champion,
                abilityId = eternalize.id,
                costPayment = AdditionalCostPayment(discardedCards = emptyList()),
            )
        ).outcome shouldNotBe Outcome.Done
        d.state.getGraveyard(me) shouldContain champion
    }
})
