package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GraveyardCastRiderSelection
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * A `MayCastFromGraveyard` grant carrying an `additionalCost` — the continuous retrace grant (Six:
 * "nonland permanent cards in your graveyard have retrace"; CR 702.81a, "cast this card from your
 * graveyard by discarding a land card as an additional cost to cast it").
 *
 * Pins: the cost is offered with a picker scoped to its filter, is unpayable without a matching card
 * (no action), is enforced by the handler (no discard, or a nonland discard, is rejected), joins the
 * CR 601.2b permission identity (a free grant beside it is its own choice), can't be dodged by
 * claiming the free permission when only the retrace grant applies, and is not owed by a cast that
 * goes through another graveyard permission (Muldrotha).
 */
class GraveyardCastGrantAdditionalCostTest : FunSpec({

    val bear = card("Grave Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val retraceBench = card("Retrace Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = MayCastFromGraveyard(
                GameObjectFilter.NonlandPermanent,
                additionalCost = Costs.additional.DiscardCards(1, GameObjectFilter.Land)
            )
        }
    }
    val freeBench = card("Free Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = MayCastFromGraveyard(GameObjectFilter.Creature) }
    }
    val muldrothaBench = card("Permanent Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = MayPlayPermanentsFromGraveyard }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bear, retraceBench, freeBench, muldrothaBench))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.clearHand(player: EntityId) {
        getHand(player).forEach { moveToGraveyard(it) }
    }

    fun GameTestDriver.bearCasts(player: EntityId, bearId: EntityId) = legalActions(player)
        .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == bearId }

    fun GameTestDriver.giveBearMana(player: EntityId) {
        giveMana(player, Color.GREEN, 1)
        giveColorlessMana(player, 1)
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 40 && state.stack.isNotEmpty() && !isPaused) bothPass()
    }

    val retraceSelection = GraveyardCastRiderSelection(
        additionalCost = Costs.additional.DiscardCards(1, GameObjectFilter.Land)
    )

    test("the grant offers the cast with a discard picker scoped to land cards") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        val forest = driver.putCardInHand(player, "Forest")
        driver.putCardInHand(player, "Grave Bear")
        driver.giveBearMana(player)

        val cast = driver.bearCasts(player, bearId).single()
        cast.description shouldContain "discard"
        (cast.action as CastSpell).graveyardCastRider shouldBe retraceSelection
        val info = cast.additionalCostInfo
        info shouldNotBe null
        info!!.validDiscardTargets shouldContainExactlyInAnyOrder listOf(forest)
    }

    test("no land card in hand means no cast action") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        driver.putCardInHand(player, "Grave Bear")
        driver.giveBearMana(player)

        driver.bearCasts(player, bearId).shouldBeEmpty()
    }

    test("discarding a land card casts the permanent card from the graveyard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        val forest = driver.putCardInHand(player, "Forest")
        driver.giveBearMana(player)

        driver.submit(
            CastSpell(
                player, bearId,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(forest)),
                graveyardCastRider = retraceSelection
            )
        ).outcome shouldBe Outcome.Done
        driver.getGraveyard(player).contains(forest) shouldBe true
        driver.resolveAll()
        driver.findPermanent(player, "Grave Bear") shouldNotBe null
    }

    test("the handler rejects a cast that discards nothing or discards a nonland card") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        driver.putCardInHand(player, "Forest")
        val otherBear = driver.putCardInHand(player, "Grave Bear")
        driver.giveBearMana(player)

        driver.submit(CastSpell(player, bearId, graveyardCastRider = retraceSelection)).error shouldNotBe null
        driver.submit(
            CastSpell(
                player, bearId,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(otherBear)),
                graveyardCastRider = retraceSelection
            )
        ).error shouldNotBe null
        driver.getGraveyard(player).contains(bearId) shouldBe true
    }

    test("claiming a costless permission when only the retrace grant applies still owes the discard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        driver.putCardInHand(player, "Forest")
        driver.giveBearMana(player)

        driver.submit(CastSpell(player, bearId, graveyardCastRider = GraveyardCastRiderSelection())).error shouldNotBe null
        driver.submit(CastSpell(player, bearId)).error shouldNotBe null
    }

    test("a free grant beside the retrace grant is a separate choice, and choosing it discards nothing") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        driver.putPermanentOnBattlefield(player, "Free Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        val forest = driver.putCardInHand(player, "Forest")
        driver.giveBearMana(player)

        val casts = driver.bearCasts(player, bearId)
        casts shouldHaveSize 2
        casts.map { (it.action as CastSpell).graveyardCastRider }
            .shouldContainExactlyInAnyOrder(listOf(retraceSelection, GraveyardCastRiderSelection()))

        driver.submit(CastSpell(player, bearId, graveyardCastRider = GraveyardCastRiderSelection()))
            .outcome shouldBe Outcome.Done
        driver.getHand(player) shouldBe listOf(forest)
        driver.resolveAll()
        driver.findPermanent(player, "Grave Bear") shouldNotBe null
    }

    test("a cast authorized by a Muldrotha-style permission doesn't owe the retrace grant's discard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.clearHand(player)
        driver.putPermanentOnBattlefield(player, "Retrace Bench")
        driver.putPermanentOnBattlefield(player, "Permanent Bench")
        val bearId = driver.putCardInGraveyard(player, "Grave Bear")
        val forest = driver.putCardInHand(player, "Forest")
        driver.giveBearMana(player)

        driver.submit(CastSpell(player, bearId)).outcome shouldBe Outcome.Done
        driver.getHand(player) shouldBe listOf(forest)
    }
})
