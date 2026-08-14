package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ori, Keeper of Songs — "As long as you have an enduring story, Ori gets +1/+0 and has vigilance."
 *
 * Both halves are `ConditionalStaticAbility`, so the layer system evaluates the enduring-story gate
 * **while the projection is still being computed** — and the storied count is itself a projected read
 * (types, supertypes, controller). That reentrancy is the trap this test exists for: the gate has to
 * be handed the in-flight projection rather than reaching for `GameState.projectedState`, whose lazy
 * initializer would re-enter itself. Reading Ori's projected power at all is the assertion.
 *
 * The mechanic's own rules (threshold, union counting, persistence) are pinned in
 * [StoriedEnduringStoryTest]; this file covers only Ori's payoff.
 */
class OriKeeperOfSongsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    test("without an enduring story Ori is a plain 3/3 with no vigilance") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Ori is legendary, so he is one of the three himself. Two lands pad the board without
        // adding a second qualifying permanent.
        val ori = d.putCreatureOnBattlefield(active, "Ori, Keeper of Songs")
        repeat(2) { d.putLandOnBattlefield(active, "Plains") }

        EnduringStoryService.has(d.state, active) shouldBe false
        d.state.projectedState.getPower(ori) shouldBe 3
        d.state.projectedState.getToughness(ori) shouldBe 3
        d.state.projectedState.hasKeyword(ori, Keyword.VIGILANCE) shouldBe false
    }

    test("with an enduring story Ori is a 4/3 with vigilance") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ori = d.putCreatureOnBattlefield(active, "Ori, Keeper of Songs")
        d.putCreatureOnBattlefield(active, "Óin the Brave")
        d.putCreatureOnBattlefield(active, "Thorin Oakenshield")

        EnduringStoryService.has(d.state, active) shouldBe true
        // +1/+0 lands in layer 7c, the vigilance grant in layer 6; both gates re-evaluate every
        // projection, which is what the reads below exercise.
        d.state.projectedState.getPower(ori) shouldBe 4
        d.state.projectedState.getToughness(ori) shouldBe 3
        d.state.projectedState.hasKeyword(ori, Keyword.VIGILANCE) shouldBe true
    }

    test("Ori keeps the bonus after the board falls apart — the designation is permanent") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ori = d.putCreatureOnBattlefield(active, "Ori, Keeper of Songs")
        val oin = d.putCreatureOnBattlefield(active, "Óin the Brave")
        val thorin = d.putCreatureOnBattlefield(active, "Thorin Oakenshield")

        // Walk to an SBA poll so the marker is written; without it the live read would simply drop
        // back to false when the board shrinks and this test would prove nothing.
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.moveToGraveyard(oin)
        d.moveToGraveyard(thorin)

        EnduringStoryService.qualifiesViaStoried(d.state, active) shouldBe false
        d.state.projectedState.getPower(ori) shouldBe 4
        d.state.projectedState.hasKeyword(ori, Keyword.VIGILANCE) shouldBe true
    }
})
