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
 * Duration.EndOfTurn and should persist for the rest of the turn regardless of
 * any subsequent fights or other interactions.
 *
 * Bug report: after using level 2 on an attacking Grizzly Bears, casting
 * Longstalk Brawl (to fight) in the post-combat main phase appeared to make
 * Grizzly Bears blockable again.
 */
class GossipsTalentLongstalkBrawlTest : FunSpec({

    test("CANT_BE_BLOCKED from Gossip's Talent level 2 persists after Longstalk Brawl fight") {
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

        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        // Attack with Grizzly Bears → level-2 "Whenever you attack" trigger fires.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bears), opponent)

        // Trigger targets "attacking creature with power 3 or less".
        driver.pendingDecision.shouldBeChooseTargets()
        driver.submitTargetSelection(player, listOf(bears))

        // Drain stack so the GrantKeyword floating effect is applied.
        driver.bothPass()

        // Sanity check: Grizzly Bears is unblockable for the turn.
        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

        // Skip blocks and combat damage, land in postcombat main phase.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Grizzly Bears should still be unblockable after combat ended.
        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

        // Cast Longstalk Brawl, mode 0 (no gift): just fight.
        driver.giveMana(player, Color.GREEN, 1)
        val spell = driver.putCardInHand(player, "Longstalk Brawl")

        val cast = driver.submit(CastSpell(
            playerId = player,
            cardId = spell,
            targets = listOf(
                ChosenTarget.Permanent(bears),
                ChosenTarget.Permanent(theirs)
            ),
            chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(
                ChosenTarget.Permanent(bears),
                ChosenTarget.Permanent(theirs)
            ))
        ))
        cast.isSuccess shouldBe true

        // Resolve the fight.
        driver.bothPass()

        // The bug: after Longstalk Brawl resolved, the CANT_BE_BLOCKED grant
        // from Gossip's Talent level 2 was gone. It should still be active for
        // the remainder of the turn.
        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
    }
})

private fun Any?.shouldBeChooseTargets() {
    check(this is ChooseTargetsDecision) {
        "Expected ChooseTargetsDecision, got ${this?.let { it::class.simpleName } ?: "null"}"
    }
}
