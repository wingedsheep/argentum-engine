package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.FurnaceReins
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Furnace Reins (MOM #141): steal a creature for the turn, untap it, give it haste and a
 * "combat damage to a player or battle -> Treasure" trigger.
 */
class FurnaceReinsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(FurnaceReins)
        )
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.treasures(playerId: EntityId): Int =
        state.getBattlefield().count { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Treasure" &&
                state.projectedState.getController(id) == playerId
        }

    test("steals, untaps, grants haste, and a connecting hit makes a Treasure for the thief") {
        val d = driver()
        val opp = d.player2
        val guide = d.putCreatureOnBattlefield(opp, "Goblin Guide")
        d.tapPermanent(guide)

        d.giveMana(d.player1, Color.RED, 3)
        val card = d.putCardInHand(d.player1, "Furnace Reins")
        d.castSpellWithTargets(d.player1, card, listOf(ChosenTarget.Permanent(guide))).outcome shouldBe Outcome.Done
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        d.isTapped(guide) shouldBe false
        d.state.projectedState.getController(guide) shouldBe d.player1

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(guide), opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opp).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.getLifeTotal(opp) shouldBe 18
        d.treasures(d.player1) shouldBe 1
        d.treasures(opp) shouldBe 0
    }
})
