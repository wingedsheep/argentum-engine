package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.WardscaleDragon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wardscale Dragon (FRF) — {4}{W}{W} 4/4 Flying Dragon. "As long as this creature is attacking,
 * defending player can't cast spells."
 *
 * The multiplayer reading of "defending player" is pinned by the engine's
 * `DefendingPlayerCantCastSpellsTest`; this covers the card: an attacked opponent can't Bolt the
 * Dragon mid-combat, its controller still casts freely, and the lock lifts once combat is over.
 */
class WardscaleDragonScenarioTest : FunSpec({

    test("defending player can't cast spells while the Dragon attacks, and can again after combat") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + WardscaleDragon)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val dragon = d.putCreatureOnBattlefield(me, "Wardscale Dragon")
        d.removeSummoningSickness(dragon)
        val theirBolt = d.putCardInHand(opponent, "Lightning Bolt")
        val myBolt = d.putCardInHand(me, "Lightning Bolt")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(dragon), opponent).error shouldBe null
        d.giveMana(me, Color.RED, 1)

        withClue("the attacking player is unaffected") {
            d.castSpell(me, myBolt, listOf(opponent)).error shouldBe null
            d.bothPass()
            d.getLifeTotal(opponent) shouldBe 17
        }

        d.giveMana(opponent, Color.RED, 1)
        d.passPriority(me)
        d.state.priorityPlayerId shouldBe opponent
        withClue("the defending player can't cast a spell while the Dragon is attacking") {
            d.castSpell(opponent, theirBolt, listOf(dragon)).outcome shouldNotBe Outcome.Done
            d.getHand(opponent).contains(theirBolt) shouldBe true
        }

        d.passPriority(opponent)
        d.declareNoBlockers(opponent)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 13
        d.giveMana(opponent, Color.RED, 1)
        d.passPriority(me)
        d.state.priorityPlayerId shouldBe opponent
        withClue("combat is over, so the Dragon is no longer attacking and the lock is gone") {
            d.castSpell(opponent, theirBolt, listOf(me)).error shouldBe null
        }
    }
})
