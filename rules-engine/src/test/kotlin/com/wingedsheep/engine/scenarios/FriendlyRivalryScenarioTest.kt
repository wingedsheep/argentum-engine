package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FriendlyRivalry
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FrodoBaggins
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Friendly Rivalry (LTR) — "Target creature you control and up to one **other** target legendary
 * creature you control each deal damage equal to their power to target creature you don't control."
 *
 * The word "other" requires the second target to differ from the first (CR 601.2c) — modeled with
 * `TargetOther`. Choosing the same creature for both must be illegal.
 */
class FriendlyRivalryScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(FriendlyRivalry, FrodoBaggins)
        )
        return d
    }

    test("choosing the same creature for both controlled targets is illegal") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val frodo = d.putCreatureOnBattlefield(active, "Frodo Baggins") // legendary, qualifies for both
        val victim = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val spell = d.putCardInHand(active, "Friendly Rivalry")
        d.giveMana(active, Color.RED, 1)
        d.giveMana(active, Color.GREEN, 1)

        val res = d.castSpellWithTargets(
            active, spell, listOf(ChosenTarget.Permanent(frodo), ChosenTarget.Permanent(frodo), ChosenTarget.Permanent(victim))
        )
        res.isSuccess shouldBe false
    }

    test("two different controlled creatures each deal their power to the opponent's creature") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = d.putCreatureOnBattlefield(active, "Grizzly Bears")  // 2/2 (target 0)
        val frodo = d.putCreatureOnBattlefield(active, "Frodo Baggins") // 1/3 legendary (target 1, other)
        val victim = d.putCreatureOnBattlefield(opp, "Hill Giant")      // 3/3
        val spell = d.putCardInHand(active, "Friendly Rivalry")
        d.giveMana(active, Color.RED, 1)
        d.giveMana(active, Color.GREEN, 1)

        d.castSpellWithTargets(
            active, spell, listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(frodo), ChosenTarget.Permanent(victim))
        ).error shouldBe null
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        // 2 (Grizzly) + 1 (Frodo) = 3 damage to the 3/3 Hill Giant -> it dies.
        d.findPermanent(opp, "Hill Giant") shouldBe null
    }
})
