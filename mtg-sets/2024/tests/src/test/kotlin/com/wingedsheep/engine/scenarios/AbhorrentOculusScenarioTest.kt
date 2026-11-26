package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Abhorrent Oculus (DSK #42) — {2}{U} Creature — Eye, 5/5, flying.
 *
 * "As an additional cost to cast this spell, exile six cards from your graveyard.
 *  Flying
 *  At the beginning of each opponent's upkeep, manifest dread."
 *
 * Exercises:
 *  - the `ExileCards(count = 6, fromZone = GRAVEYARD)` additional cost (paid as the spell is cast —
 *    so the spell is uncastable without six cards to exile), and
 *  - the `EachOpponentUpkeep` triggered ability reusing the shared manifest-dread recipe.
 */
class AbhorrentOculusScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.fillGraveyard(player: EntityId, count: Int): List<EntityId> =
        (0 until count).map { putCardInGraveyard(player, "Grizzly Bears") }

    test("casting exiles six cards from the graveyard and resolves onto the battlefield") {
        val driver = newDriver()
        val me = driver.player1

        val graveyardCards = driver.fillGraveyard(me, 6)
        val spell = driver.putCardInHand(me, "Abhorrent Oculus")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 2)

        val cast = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = emptyList(),
                additionalCostPayment = AdditionalCostPayment(exiledCards = graveyardCards),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        cast.error shouldBe null

        // The six cards are exiled as the spell is cast.
        driver.getExile(me).shouldContainAll(graveyardCards)
        driver.getGraveyard(me).size shouldBe 0

        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getCreatures(me).map { driver.getCardName(it) } shouldContainAll listOf("Abhorrent Oculus")
    }

    test("a free cast under Omniscience still prompts for the six-card graveyard exile") {
        val driver = newDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Omniscience")
        val graveyardCards = driver.fillGraveyard(me, 6)
        val spell = driver.putCardInHand(me, "Abhorrent Oculus")

        // The free variant must carry the exile prompt, or the client submits it with no
        // cards chosen and the cast is rejected outright.
        val free = driver.legalActions(me).single {
            it.actionType == "CastWithoutPayingManaCost" && (it.action as? CastSpell)?.cardId == spell
        }
        val costInfo = free.additionalCostInfo.shouldNotBeNull()
        costInfo.costType shouldBe "ExileFromGraveyard"
        costInfo.validExileTargets.shouldContainAll(graveyardCards)
        costInfo.exileMinCount shouldBe 6

        val cast = driver.submit(
            (free.action as CastSpell).copy(
                additionalCostPayment = AdditionalCostPayment(exiledCards = graveyardCards),
            )
        )
        cast.error shouldBe null
        driver.getExile(me).shouldContainAll(graveyardCards)

        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.getCreatures(me).map { driver.getCardName(it) } shouldContainAll listOf("Abhorrent Oculus")
    }

    test("the spell cannot be cast without six cards in the graveyard") {
        val driver = newDriver()
        val me = driver.player1

        val tooFew = driver.fillGraveyard(me, 5) // one short
        val spell = driver.putCardInHand(me, "Abhorrent Oculus")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 2)

        val cast = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = emptyList(),
                additionalCostPayment = AdditionalCostPayment(exiledCards = tooFew),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        // Casting must fail — the additional cost requires exiling six cards.
        (cast.error != null).shouldBe(true)
        driver.getExile(me).size shouldBe 0
    }

    test("manifests dread at the beginning of each opponent's upkeep") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Abhorrent Oculus")

        // Seed the controller's library so manifest dread has cards to look at.
        driver.putCardOnTopOfLibrary(me, "Island")
        val creature = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        // Advance to the opponent's upkeep — the trigger fires there.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.activePlayer shouldBe opponent

        // The trigger goes on the stack at upkeep; resolve until the manifest-dread pick pauses.
        var guard = 0
        while (driver.pendingDecision !is SelectCardsDecision && guard++ < 30) {
            if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
        }

        // Resolve the manifest-dread trigger by choosing the creature to manifest.
        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(me, CardsSelectedResponse(pick.id, listOf(creature)))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // The chosen card is now a face-down 2/2 the controller controls.
        driver.state.getEntity(creature)?.get<FaceDownComponent>() shouldBe FaceDownComponent
    }
})
