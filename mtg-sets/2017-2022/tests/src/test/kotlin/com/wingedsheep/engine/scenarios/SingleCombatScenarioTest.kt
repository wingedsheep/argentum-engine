package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.SingleCombat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Single Combat (WAR; reprinted in PIP) — {3}{W}{W} Sorcery.
 *
 * "Each player chooses a creature or planeswalker they control, then sacrifices the rest. Players
 * can't cast creature or planeswalker spells until the end of your next turn."
 */
class SingleCombatScenarioTest : FunSpec({

    val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }
    val trinket = card("Test Trinket") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = ""
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SingleCombat, walker, trinket))
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolve(keep: Map<EntityId, EntityId>) {
        var guard = 0
        while ((pendingDecision != null || state.stack.isNotEmpty()) && guard++ < 50) {
            val decision = pendingDecision
            if (decision is SelectCardsDecision) submitCardSelection(decision.playerId, listOfNotNull(keep[decision.playerId]))
            else bothPass()
        }
    }

    fun GameTestDriver.putWalker(player: EntityId): EntityId =
        putPermanentOnBattlefield(player, "Test Walker").also {
            addComponent(it, CountersComponent().withAdded(CounterType.LOYALTY, 3))
        }

    fun GameTestDriver.castOffered(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any { (it.action as? CastSpell)?.cardId == cardId }

    test("each player keeps one creature or planeswalker and sacrifices the rest; other permanents stay") {
        val d = driver()
        val caster = d.activePlayer!!
        val opp = d.getOpponent(caster)

        val myKeeper = d.putWalker(caster)
        val myBear = d.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val myTrinket = d.putPermanentOnBattlefield(caster, "Test Trinket")
        val theirKeeper = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val theirBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val theirWalker = d.putWalker(opp)

        val spell = d.putCardInHand(caster, "Single Combat")
        d.giveMana(caster, Color.WHITE, 5)
        d.castSpell(caster, spell)
        d.resolve(mapOf(caster to myKeeper, opp to theirKeeper))

        val battlefield = d.state.getBattlefield()
        withClue("keepers and noncreature, nonplaneswalker permanents survive") {
            battlefield.containsAll(listOf(myKeeper, myTrinket, theirKeeper)) shouldBe true
        }
        withClue("every other creature and planeswalker is sacrificed") {
            listOf(myBear, theirBear, theirWalker).none { it in battlefield } shouldBe true
        }
    }

    test("no one can cast creature or planeswalker spells until the end of the caster's next turn") {
        val d = driver()
        val caster = d.activePlayer!!
        val opp = d.getOpponent(caster)
        d.putCreatureOnBattlefield(caster, "Grizzly Bears")

        val spell = d.putCardInHand(caster, "Single Combat")
        d.giveMana(caster, Color.WHITE, 5)
        d.castSpell(caster, spell)
        d.resolve(emptyMap())

        val myBear = d.putCardInHand(caster, "Grizzly Bears")
        val myWalker = d.putCardInHand(caster, "Test Walker")
        val myTrinket = d.putCardInHand(caster, "Test Trinket")
        d.giveMana(caster, Color.WHITE, 3)
        withClue("the caster is banned this turn, but noncreature spells stay legal") {
            d.castOffered(caster, myBear) shouldBe false
            d.castOffered(caster, myWalker) shouldBe false
            d.castOffered(caster, myTrinket) shouldBe true
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        val theirBear = d.putCardInHand(opp, "Grizzly Bears")
        d.giveMana(opp, Color.GREEN, 2)
        withClue("the opponent is banned through their own turn") {
            d.castOffered(opp, theirBear) shouldBe false
        }

        // Fresh cards at each check: cleanup may discard down to hand size in between.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, caster)
        val nextBear = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        withClue("still banned on the caster's next turn") {
            d.castOffered(caster, nextBear) shouldBe false
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        val laterBear = d.putCardInHand(opp, "Grizzly Bears")
        d.giveMana(opp, Color.GREEN, 2)
        withClue("the ban lifts after the caster's next turn ends") {
            d.castOffered(opp, laterBear) shouldBe true
        }
    }
})
