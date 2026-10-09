package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ScoutTheWilderness
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScoutTheWildernessScenarioTest : FunSpec({
    val nonbasic = card("Scout Test Nonbasic") { typeLine = "Land — Forest" }
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(ScoutTheWilderness, DominariaUnitedForest274, nonbasic))
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.GREEN, 4)
        giveMana(player1, Color.WHITE, 1)
    }
    for (kicked in listOf(false, true)) {
        for (findLand in listOf(false, true)) {
            test("kicked=$kicked, find land=$findLand: search continuation preserves token rider") {
                val d = driver()
                val land = d.putCardOnTopOfLibrary(d.player1, "Forest")
                val excluded = d.putCardOnTopOfLibrary(d.player1, nonbasic.name)
                val spell = d.putCardInHand(d.player1, "Scout the Wilderness")
                val eventCount = d.events.size
                d.submit(CastSpell(
                    playerId = d.player1,
                    cardId = spell,
                    declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                d.bothPass()
                val search = d.pendingDecision as SelectCardsDecision
                (land in search.options) shouldBe true
                (excluded in search.options) shouldBe false
                d.submitCardSelection(d.player1, if (findLand) listOf(land) else emptyList())
                d.pendingDecision shouldBe null
                d.stackSize shouldBe 0
                (land in d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))) shouldBe findLand
                if (findLand) d.isTapped(land) shouldBe true
                d.events.drop(eventCount).filterIsInstance<LibraryShuffledEvent>().size shouldBe 1
                val tokens = d.getCreatures(d.player1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
                tokens.size shouldBe if (kicked) 2 else 0
                for (token in tokens) {
                    d.state.projectedState.getPower(token) shouldBe 1
                    d.state.projectedState.getToughness(token) shouldBe 1
                    d.state.projectedState.getColors(token) shouldBe setOf("WHITE")
                    d.state.projectedState.getSubtypes(token).contains("Soldier") shouldBe true
                    d.isTapped(token) shouldBe false
                }
                d.getCreatures(d.player2).size shouldBe 0
            }
        }
    }
})
