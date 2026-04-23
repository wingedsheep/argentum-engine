package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression test: Gossip's Talent (level 2) grants "target attacking creature
 * with power 3 or less can't be blocked this turn". That floating effect has
 * Duration.EndOfTurn and should persist for the rest of the turn.
 *
 * Bug report: after using level 2 on an attacking Grizzly Bears, casting
 * High Stride (instant: +1/+3, reach, untap) on Grizzly Bears during combat
 * appeared to make Grizzly Bears blockable again.
 *
 * High Stride is interesting because its `Untap` effect runs on an attacking
 * creature mid-combat — if the engine strips any combat/floating state on
 * untap, that would drop the CANT_BE_BLOCKED grant.
 */
class GossipsTalentHighStrideTest : FunSpec({

    test("CANT_BE_BLOCKED from Gossip's Talent level 2 persists after High Stride untaps the attacker") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        // Gossip's Talent already leveled up to level 2.
        val talent = driver.putPermanentOnBattlefield(player, "Gossip's Talent")
        driver.replaceState(
            driver.state.updateEntity(talent) { it.with(ClassLevelComponent(currentLevel = 2)) }
        )

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        // Opponent has a blocker available for the attempted block.
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Attack with Grizzly Bears → level-2 "Whenever you attack" trigger fires.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bears), opponent)

        // Trigger targets "attacking creature with power 3 or less".
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(player, listOf(bears))

        // Drain stack so the GrantKeyword floating effect is applied.
        driver.bothPass()

        // Sanity: Grizzly Bears is unblockable.
        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

        // Still in the attackers-declared window (post-declare, pre-blockers).
        // Cast High Stride (instant) on Grizzly Bears: +1/+3, reach, untap.
        driver.giveMana(player, Color.GREEN, 1)
        val spell = driver.putCardInHand(player, "High Stride")

        val cast = driver.submit(CastSpell(
            playerId = player,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(bears))
        ))
        cast.isSuccess shouldBe true

        // Resolve High Stride.
        driver.bothPass()

        // After High Stride resolves, CANT_BE_BLOCKED should still be on Grizzly Bears.
        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

        // Advance to declare blockers and try to block Grizzly Bears — should fail.
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val blockResult = driver.declareBlockers(opponent, mapOf(blocker to listOf(bears)))
        blockResult.isSuccess shouldBe false
    }
})
