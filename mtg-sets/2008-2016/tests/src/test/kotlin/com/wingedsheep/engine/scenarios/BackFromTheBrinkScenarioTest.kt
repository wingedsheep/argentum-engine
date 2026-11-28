package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.BackFromTheBrink
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Back from the Brink (ISD #44): "Exile a creature card from your graveyard and pay its mana cost:
 * Create a token that's a copy of that card. Activate only as a sorcery."
 *
 * Pins the card end to end: the chosen card's own cost is what's paid, the token copies the card
 * that paid, a bare activation asks which affordable card to exile, and the sorcery-speed limit.
 */
class BackFromTheBrinkScenarioTest : FunSpec({

    val abilityId = BackFromTheBrink.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(BackFromTheBrink)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun newPermanent(d: GameTestDriver, playerId: EntityId, before: Set<EntityId>): EntityId =
        d.getPermanents(playerId).single { it !in before }

    test("exiling Centaur Courser costs {2}{G} and makes a 3/3 token copy of it") {
        val d = driver()
        val me = d.activePlayer!!
        val brink = d.putPermanentOnBattlefield(me, "Back from the Brink")
        val courser = d.putCardInGraveyard(me, "Centaur Courser")
        d.giveMana(me, Color.GREEN, 3)

        val before = d.getPermanents(me).toSet()
        d.submit(
            ActivateAbility(
                playerId = me, sourceId = brink, abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(courser)),
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getExileCardNames(me) shouldBe listOf("Centaur Courser")
        val token = newPermanent(d, me, before)
        withClue("the token is a copy of the exiled card") {
            d.getCardName(token) shouldBe "Centaur Courser"
            d.state.projectedState.getPower(token) shouldBe 3
            d.state.projectedState.getToughness(token) shouldBe 3
        }
    }

    test("a bare activation offers only the creature cards whose cost you can pay") {
        val d = driver()
        val me = d.activePlayer!!
        val brink = d.putPermanentOnBattlefield(me, "Back from the Brink")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        d.putCardInGraveyard(me, "Force of Nature")
        d.giveMana(me, Color.GREEN, 2)

        d.submit(ActivateAbility(me, brink, abilityId)).error shouldBe null
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(bears)

        val before = d.getPermanents(me).toSet()
        d.submitCardSelection(me, listOf(bears))
        d.bothPass()
        d.getCardName(newPermanent(d, me, before)) shouldBe "Grizzly Bears"
        d.getGraveyardCardNames(me) shouldBe listOf("Force of Nature")
    }

    test("it can only be activated at sorcery speed") {
        val d = driver()
        val me = d.activePlayer!!
        val brink = d.putPermanentOnBattlefield(me, "Back from the Brink")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.giveMana(me, Color.GREEN, 2)

        d.submit(
            ActivateAbility(
                playerId = me, sourceId = brink, abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
            )
        ).error shouldNotBe null
        d.getGraveyardCardNames(me) shouldBe listOf("Grizzly Bears")
    }
})
