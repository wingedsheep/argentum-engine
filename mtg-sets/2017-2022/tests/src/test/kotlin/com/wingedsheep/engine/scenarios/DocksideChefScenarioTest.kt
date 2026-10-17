package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.DocksideChef
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dockside Chef — {B} Enchantment Creature — Human Citizen 1/2
 * {1}{B}, Sacrifice an artifact or creature: Draw a card.
 */
class DocksideChefScenarioTest : FunSpec({

    val trinket = card("Chef Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    val charm = card("Chef Test Charm") {
        manaCost = "{1}"
        typeLine = "Enchantment"
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + DocksideChef + trinket + charm)
        initMirrorMatch(deck = Deck.of("Swamp" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.BLACK, 2)
    }

    fun activate(d: GameTestDriver, chef: EntityId, sacrifice: EntityId) = d.submit(
        ActivateAbility(
            d.player1, chef, DocksideChef.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice)),
            paymentStrategy = PaymentStrategy.FromPool,
        )
    )

    test("sacrificing an artifact draws a card") {
        val d = driver()
        val chef = d.putCreatureOnBattlefield(d.player1, DocksideChef.name)
        val artifact = d.putPermanentOnBattlefield(d.player1, trinket.name)
        val handBefore = d.getHandSize(d.player1)

        activate(d, chef, artifact).outcome shouldBe Outcome.Done
        d.getGraveyard(d.player1) shouldContain artifact
        d.bothPass()

        d.getHandSize(d.player1) shouldBe handBefore + 1
    }

    test("it can sacrifice itself, but not a non-artifact non-creature permanent") {
        val d = driver()
        val chef = d.putCreatureOnBattlefield(d.player1, DocksideChef.name)
        val enchantment = d.putPermanentOnBattlefield(d.player1, charm.name)

        activate(d, chef, enchantment).error shouldNotBe null

        val handBefore = d.getHandSize(d.player1)
        activate(d, chef, chef).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getHandSize(d.player1) shouldBe handBefore + 1
    }
})
