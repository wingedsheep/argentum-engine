package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m11.cards.AngelicArbiter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Angelic Arbiter — "Each opponent who cast a spell this turn can't attack with creatures.
 * Each opponent who attacked with a creature this turn can't cast spells."
 *
 * On an opponent's turn that opponent may cast spells or attack, not both; on the Arbiter
 * controller's own turn neither half does anything. Both halves read the whole turn's history,
 * so an Arbiter arriving mid-turn still sees an earlier spell or attack.
 */
class AngelicArbiterScenarioTest : FunSpec({

    val probe = card("Arbiter Test Glint") { manaCost = "{G}"; typeLine = "Instant"; oracleText = "" }

    fun driver(startingPlayer: Int) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AngelicArbiter, probe))
        initMirrorMatch(Deck.of("Forest" to 40), startingLife = 20, startingPlayer = startingPlayer)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    fun GameTestDriver.validAttackers(playerId: EntityId): List<EntityId> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .firstOrNull { it.actionType == "DeclareAttackers" }
            ?.validAttackers
            .orEmpty()

    fun GameTestDriver.castProbe(playerId: EntityId) {
        val spell = putCardInHand(playerId, "Arbiter Test Glint")
        giveMana(playerId, Color.GREEN, 1)
        castSpell(playerId, spell).error shouldBe null
        bothPass()
    }

    test("an opponent who cast a spell this turn can't attack") {
        val d = driver(startingPlayer = 1)
        d.putCreatureOnBattlefield(d.player1, "Angelic Arbiter")
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.castProbe(d.player2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        d.validAttackers(d.player2).shouldBeEmpty()
        d.declareAttackers(d.player2, listOf(bear), d.player1).error shouldNotBe null
    }

    test("an opponent who cast no spell may attack, and then can't cast spells") {
        val d = driver(startingPlayer = 1)
        d.putCreatureOnBattlefield(d.player1, "Angelic Arbiter")
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        val spell = d.putCardInHand(d.player2, "Arbiter Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.castActionsFor(d.player2, spell).isNotEmpty() shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.validAttackers(d.player2) shouldContain bear
        d.declareAttackers(d.player2, listOf(bear), d.player1).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.giveMana(d.player2, Color.GREEN, 1)
        d.castActionsFor(d.player2, spell).isEmpty() shouldBe true
    }

    test("the Arbiter's controller is unaffected on their own turn") {
        val d = driver(startingPlayer = 0)
        d.putCreatureOnBattlefield(d.player1, "Angelic Arbiter")
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.castProbe(d.player1)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bear), d.player2).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        val spell = d.putCardInHand(d.player1, "Arbiter Test Glint")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castActionsFor(d.player1, spell).isNotEmpty() shouldBe true
    }

    test("an Arbiter arriving after the spell still stops the attack") {
        val d = driver(startingPlayer = 1)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.castProbe(d.player2)
        d.putCreatureOnBattlefield(d.player1, "Angelic Arbiter")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        d.validAttackers(d.player2) shouldNotContain bear
    }

    test("the attack lock lifts when the Arbiter leaves the battlefield") {
        val d = driver(startingPlayer = 1)
        val arbiter = d.putCreatureOnBattlefield(d.player1, "Angelic Arbiter")
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.castProbe(d.player2)
        d.moveToGraveyard(arbiter)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        d.declareAttackers(d.player2, listOf(bear), d.player1).error shouldBe null
    }
})
