package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.Outwit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Outwit (AVR #70) — "Counter target spell that targets a player."
 *
 * First card on `CardPredicate.TargetsPlayer(Player.Any)`: any player counts, not only you.
 */
class OutwitScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Outwit)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Player 2 (active) bolts [target] and passes; returns the spell on the stack. */
    fun GameTestDriver.opponentBolts(target: ChosenTarget): EntityId {
        giveMana(player2, Color.RED, 1)
        val bolt = putCardInHand(player2, "Lightning Bolt")
        castSpellWithTargets(player2, bolt, listOf(target)).error shouldBe null
        passPriority(player2)
        return bolt
    }

    fun GameTestDriver.outwit(spell: EntityId) =
        run {
            giveMana(player1, Color.BLUE, 1)
            val outwit = putCardInHand(player1, "Outwit")
            castSpellWithTargets(player1, outwit, listOf(ChosenTarget.Spell(spell)))
        }

    test("counters a spell that targets you") {
        val d = driver()
        val bolt = d.opponentBolts(ChosenTarget.Player(d.player1))
        d.outwit(bolt).error shouldBe null
        d.bothPass()

        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)) shouldContain bolt
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("counters a spell that targets another player too") {
        val d = driver()
        val bolt = d.opponentBolts(ChosenTarget.Player(d.player2))
        d.outwit(bolt).error shouldBe null
        d.bothPass()

        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)) shouldContain bolt
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("can't target a spell that targets only a creature") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val bolt = d.opponentBolts(ChosenTarget.Permanent(courser))
        d.outwit(bolt).error shouldNotBe null
    }
})
