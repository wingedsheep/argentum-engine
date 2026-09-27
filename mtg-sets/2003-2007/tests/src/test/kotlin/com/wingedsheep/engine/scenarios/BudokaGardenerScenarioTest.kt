package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.BudokaGardener
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Budoka Gardener // Dokai, Weaver of Life (CHK) — a flip card.
 *
 * "{T}: You may put a land card from your hand onto the battlefield. If you control ten or more
 * lands, flip this creature."
 * Dokai: "{4}{G}{G}, {T}: Create an X/X green Elemental creature token, where X is the number of
 * lands you control."
 */
class BudokaGardenerScenarioTest : FunSpec({

    val gardenerAbility = BudokaGardener.activatedAbilities.single().id
    val dokaiAbility = BudokaGardener.flipSide!!.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BudokaGardener)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    fun GameTestDriver.lands() = state.getBattlefield(player1).filter { state.projectedState.hasType(it, "LAND") }

    fun GameTestDriver.activateGardener(gardener: EntityId) {
        submitSuccess(ActivateAbility(player1, gardener, gardenerAbility))
        bothPass()
    }

    test("putting down a land that is not the tenth leaves it unflipped") {
        val d = driver()
        val gardener = d.putCreatureOnBattlefield(d.player1, "Budoka Gardener")
        d.removeSummoningSickness(gardener)
        repeat(3) { d.putLandOnBattlefield(d.player1, "Forest") }
        val forest = d.putCardInHand(d.player1, "Forest")

        d.activateGardener(gardener)
        d.submitCardSelection(d.player1, listOf(forest))

        d.state.getBattlefield(d.player1) shouldContain forest
        d.getHand(d.player1) shouldNotContain forest
        d.lands().size shouldBe 4
        d.name(gardener) shouldBe "Budoka Gardener"
    }

    test("the tenth land flips it into Dokai, whose token counts lands") {
        val d = driver()
        val gardener = d.putCreatureOnBattlefield(d.player1, "Budoka Gardener")
        d.removeSummoningSickness(gardener)
        repeat(9) { d.putLandOnBattlefield(d.player1, "Forest") }
        val forest = d.putCardInHand(d.player1, "Forest")

        d.activateGardener(gardener)
        d.submitCardSelection(d.player1, listOf(forest))

        d.lands().size shouldBe 10
        d.name(gardener) shouldBe "Dokai, Weaver of Life"
        d.state.projectedState.isLegendary(gardener) shouldBe true
        d.state.projectedState.getPower(gardener) shouldBe 3

        // Untap for Dokai's ability (flipping doesn't untap it).
        d.removeSummoningSickness(gardener)
        d.untapPermanent(gardener)
        val before = d.state.getBattlefield(d.player1).toSet()
        d.giveMana(d.player1, Color.GREEN, 6)
        d.submitSuccess(
            ActivateAbility(d.player1, gardener, dokaiAbility, paymentStrategy = PaymentStrategy.FromPool)
        )
        d.bothPass()

        val token = (d.state.getBattlefield(d.player1).toSet() - before).single()
        d.state.projectedState.getPower(token) shouldBe 10
        d.state.projectedState.getToughness(token) shouldBe 10
        d.state.projectedState.hasSubtype(token, "Elemental") shouldBe true
    }

    test("declining the land still flips with ten lands already in play") {
        val d = driver()
        val gardener = d.putCreatureOnBattlefield(d.player1, "Budoka Gardener")
        d.removeSummoningSickness(gardener)
        repeat(10) { d.putLandOnBattlefield(d.player1, "Forest") }
        val forest = d.putCardInHand(d.player1, "Forest")

        d.activateGardener(gardener)
        d.submitCardSelection(d.player1, emptyList())

        d.getHand(d.player1) shouldContain forest
        d.name(gardener) shouldBe "Dokai, Weaver of Life"
    }
})
