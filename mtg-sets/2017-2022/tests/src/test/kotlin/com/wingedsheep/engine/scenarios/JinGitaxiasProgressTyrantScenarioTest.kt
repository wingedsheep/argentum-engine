package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.JinGitaxiasProgressTyrant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jin-Gitaxias, Progress Tyrant — {5}{U}{U} Legendary Creature — Phyrexian Praetor 5/5
 * Whenever you cast an artifact, instant, or sorcery spell, copy that spell. You may choose new
 * targets for the copy. This ability triggers only once each turn.
 * Whenever an opponent casts an artifact, instant, or sorcery spell, counter that spell. This
 * ability triggers only once each turn.
 *
 * Ruling: "A copy is created even if the spell that caused the ability to trigger has been
 * countered by the time that ability resolves." The copy is made from the spell's last-known
 * information on the stack (CR 608.2h).
 */
class JinGitaxiasProgressTyrantScenarioTest : FunSpec({

    fun driver(startingPlayer: Int = 0) = GameTestDriver().apply {
        registerCards(TestCards.all + JinGitaxiasProgressTyrant)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = startingPlayer)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Resolve the stack, keeping the original target whenever a copy asks for new ones. */
    fun settle(d: GameTestDriver): Int {
        var retargetPrompts = 0
        var guard = 0
        while (guard++ < 30 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val decision = d.pendingDecision) {
                null -> d.bothPass()
                is ChooseTargetsDecision -> {
                    retargetPrompts++
                    d.submitTargetSelection(decision.playerId, emptyList()).error shouldBe null
                }
                else -> error("unexpected decision $decision")
            }
        }
        return retargetPrompts
    }

    fun named(d: GameTestDriver, player: EntityId, name: String) =
        d.getPermanents(player).filter { d.state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("an instant you cast is copied, and the copy may keep the original target") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Jin-Gitaxias, Progress Tyrant")
        d.putLandOnBattlefield(me, "Mountain")
        val bolt = d.putCardInHand(me, "Lightning Bolt")

        d.castSpell(me, bolt, listOf(d.player2)).error shouldBe null
        settle(d) shouldBe 1

        d.getLifeTotal(d.player2) shouldBe 14
    }

    test("the copy is made even when the spell was countered before the trigger resolved") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Jin-Gitaxias, Progress Tyrant")
        d.putLandOnBattlefield(me, "Mountain")
        repeat(2) { d.putLandOnBattlefield(me, "Island") }
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        val counterspell = d.putCardInHand(me, "Counterspell")

        d.castSpell(me, bolt, listOf(d.player2)).error shouldBe null
        // Holding priority, counter the Bolt above the copy trigger. The Counterspell is the second
        // instant this turn, so it doesn't trigger Jin-Gitaxias again.
        d.castSpellWithTargets(me, counterspell, listOf(ChosenTarget.Spell(bolt))).error shouldBe null
        d.state.stack.size shouldBe 3

        settle(d) shouldBe 1

        // The Bolt itself was countered; only its copy dealt damage.
        d.getGraveyard(me).contains(bolt) shouldBe true
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("it copies only the first artifact, instant, or sorcery spell each turn") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Jin-Gitaxias, Progress Tyrant")
        repeat(2) { d.putLandOnBattlefield(me, "Mountain") }
        val first = d.putCardInHand(me, "Lightning Bolt")
        val second = d.putCardInHand(me, "Lightning Bolt")

        d.castSpell(me, first, listOf(d.player2)).error shouldBe null
        settle(d) shouldBe 1
        d.castSpell(me, second, listOf(d.player2)).error shouldBe null
        settle(d) shouldBe 0

        d.getLifeTotal(d.player2) shouldBe 11
    }

    test("a copy of an artifact spell becomes a token") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Jin-Gitaxias, Progress Tyrant")
        repeat(2) { d.putLandOnBattlefield(me, "Island") }
        val golem = d.putCardInHand(me, "Artifact Creature")

        d.castSpell(me, golem).error shouldBe null
        settle(d) shouldBe 0

        val golems = named(d, me, "Artifact Creature")
        golems.size shouldBe 2
        golems.count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 1
    }

    test("the first artifact, instant, or sorcery spell an opponent casts each turn is countered") {
        val d = driver(startingPlayer = 1)
        val jinController = d.player1
        val opponent = d.player2
        d.activePlayer shouldBe opponent
        d.putCreatureOnBattlefield(jinController, "Jin-Gitaxias, Progress Tyrant")
        repeat(2) { d.putLandOnBattlefield(opponent, "Mountain") }
        val first = d.putCardInHand(opponent, "Lightning Bolt")
        val second = d.putCardInHand(opponent, "Lightning Bolt")

        d.castSpell(opponent, first, listOf(jinController)).error shouldBe null
        settle(d)
        d.getLifeTotal(jinController) shouldBe 20

        d.castSpell(opponent, second, listOf(jinController)).error shouldBe null
        settle(d)
        d.getLifeTotal(jinController) shouldBe 17
    }
})
