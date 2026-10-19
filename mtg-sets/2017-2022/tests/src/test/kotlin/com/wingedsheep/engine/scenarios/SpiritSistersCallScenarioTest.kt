package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.SpiritSistersCall
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spirit-Sister's Call (NEO #237).
 *
 * At the beginning of your end step, choose target permanent card in your graveyard. You may
 * sacrifice a permanent that shares a card type with the chosen card. If you do, return the chosen
 * card from your graveyard to the battlefield and it gains "If this permanent would leave the
 * battlefield, exile it instead of putting it anywhere else."
 *
 * Pinned: the sacrifice is offered only among permanents sharing a card type with the *graveyard*
 * card (read at resolution through the bound target), declining it returns nothing, and a returned
 * card is exiled rather than put into a graveyard when it next leaves.
 */
class SpiritSistersCallScenarioTest : FunSpec({

    fun createDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(SpiritSistersCall))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    /** Passes into the end step until the trigger asks for its graveyard target. */
    fun GameTestDriver.toTargetChoice(): ChooseTargetsDecision {
        passPriorityUntil(Step.END)
        var guard = 0
        while (state.pendingDecision == null && state.stack.isNotEmpty() && guard++ < 20) {
            passPriority(state.priorityPlayerId!!)
        }
        return pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    /** Resolves the trigger up to the optional-sacrifice pick. */
    fun GameTestDriver.toSacrificeChoice(): SelectCardsDecision {
        var guard = 0
        while (state.pendingDecision == null && state.stack.isNotEmpty() && guard++ < 20) {
            passPriority(state.priorityPlayerId!!)
        }
        return pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("sacrificing a creature returns the creature card, which is exiled when it next leaves") {
        val (driver, you) = createDriver()
        val call = driver.putPermanentOnBattlefield(you, "Spirit-Sister's Call")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val enchantment = driver.putPermanentOnBattlefield(you, "Test Enchantment")
        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")

        val target = driver.toTargetChoice()
        withClue("only permanent cards in your graveyard are targetable") {
            target.legalTargets[0]!! shouldContain bears
        }
        driver.submitTargetSelection(you, listOf(bears))

        val pick = driver.toSacrificeChoice()
        withClue("only permanents sharing a card type with the creature card are offered") {
            pick.options shouldContainExactlyInAnyOrder listOf(lions)
            pick.options shouldNotContain enchantment
            pick.options shouldNotContain call
            pick.minSelections shouldBe 0
        }
        driver.submitCardSelection(you, listOf(lions))

        driver.getGraveyard(you) shouldContain lions
        driver.state.getBattlefield() shouldContain bears
        driver.getGraveyard(you) shouldNotContain bears

        // The returned card now carries the exile-instead replacement: sacrificing it next end step
        // to return Lions sends Bears to exile, not the graveyard.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = you)
        driver.toTargetChoice()
        driver.submitTargetSelection(you, listOf(lions))
        driver.toSacrificeChoice()
        driver.submitCardSelection(you, listOf(bears))

        driver.getExile(you) shouldContain bears
        driver.getGraveyard(you) shouldNotContain bears
        driver.state.getBattlefield() shouldContain lions
    }

    test("declining the sacrifice leaves the chosen card in the graveyard") {
        val (driver, you) = createDriver()
        driver.putPermanentOnBattlefield(you, "Spirit-Sister's Call")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")

        driver.toTargetChoice()
        driver.submitTargetSelection(you, listOf(bears))
        driver.toSacrificeChoice()
        driver.submitCardSelection(you, emptyList())

        driver.getGraveyard(you) shouldContain bears
        driver.state.getBattlefield() shouldContain lions
        driver.state.getBattlefield() shouldNotContain bears
    }

    test("an enchantment card can be returned by sacrificing Spirit-Sister's Call itself") {
        val (driver, you) = createDriver()
        val call = driver.putPermanentOnBattlefield(you, "Spirit-Sister's Call")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val enchantmentCard = driver.putCardInGraveyard(you, "Test Enchantment")

        driver.toTargetChoice()
        driver.submitTargetSelection(you, listOf(enchantmentCard))

        val pick = driver.toSacrificeChoice()
        withClue("the creature doesn't share a card type with the enchantment card") {
            pick.options shouldContainExactlyInAnyOrder listOf(call)
        }
        driver.submitCardSelection(you, listOf(call))

        driver.getGraveyard(you) shouldContain call
        driver.state.getBattlefield() shouldContain enchantmentCard
        driver.state.getBattlefield() shouldContain lions
    }
})
