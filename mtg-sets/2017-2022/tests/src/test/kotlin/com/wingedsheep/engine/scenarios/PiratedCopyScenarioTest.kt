package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.j22.cards.PiratedCopy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PiratedCopyScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(PiratedCopy))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castCopying(target: EntityId?): EntityId {
        val id = putCardInHand(player1, "Pirated Copy")
        giveMana(player1, Color.BLUE, 5)
        castSpell(player1, id).error shouldBe null
        bothPass()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(player1, listOfNotNull(target)).error shouldBe null
        return id
    }

    fun GameTestDriver.resolveCombatDamageTriggers() {
        passPriorityUntil(Step.COMBAT_DAMAGE)
        while (state.pendingDecision != null || state.stack.isNotEmpty()) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("copies a creature as a Pirate and draws for itself and each same-named creature that connects") {
        val d = driver()
        val theirBear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val myBear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val giant = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val copy = d.castCopying(theirBear)

        d.state.getEntity(copy)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        d.state.projectedState.hasSubtype(copy, "Bear") shouldBe true
        d.state.projectedState.hasSubtype(copy, "Pirate") shouldBe true
        d.state.projectedState.hasSubtype(myBear, "Pirate") shouldBe false

        listOf(copy, myBear, giant).forEach { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(copy, myBear, giant), d.player2).error shouldBe null
        val before = d.getHandSize(d.player1)
        d.resolveCombatDamageTriggers()

        // The copy and the other Grizzly Bears each draw a card; the Hill Giant doesn't share the name.
        d.getLifeTotal(d.player2) shouldBe 20 - 2 - 2 - 3
        d.getHandSize(d.player1) shouldBe before + 2
    }

    test("an opponent's creature with the same name dealing combat damage also draws you a card") {
        val d = driver()
        val theirBear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.castCopying(theirBear)

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player2
        d.removeSummoningSickness(theirBear)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player2, listOf(theirBear), d.player1).error shouldBe null
        val before = d.getHandSize(d.player1)
        d.resolveCombatDamageTriggers()

        d.getLifeTotal(d.player1) shouldBe 18
        d.getHandSize(d.player1) shouldBe before + 1
    }

    test("declining the copy leaves a 0/0 that dies to state-based actions") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.castCopying(null)

        d.getGraveyardCardNames(d.player1).contains("Pirated Copy") shouldBe true
    }

    test("the copy badge previews Pirated Copy's own printing, not a by-name lookup") {
        val d = driver()
        val theirBear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val copy = d.castCopying(theirBear)

        val card = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(d.state, viewingPlayerId = d.player1).cards[copy]!!
        card.copyOf shouldBe "Pirated Copy"
        card.copyOfImageUri shouldBe PiratedCopy.metadata.imageUri
    }
})
