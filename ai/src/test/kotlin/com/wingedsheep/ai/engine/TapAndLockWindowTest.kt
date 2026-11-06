package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The tap-and-lock half of [AiProfile.refuseWastedPayoffs]'s restriction window: Deadlock Trap's
 * "{T}, Pay {E}: Tap target creature or planeswalker. Its activated abilities can't be activated
 * this turn." On a creature with no activated ability the lock buys nothing, so the tap is all
 * there is — worth it before their attack or before our blocks are declared, wasted after our own
 * combat. On a creature with an activated ability the lock is worth spending in any window.
 */
class TapAndLockWindowTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun position(activePlayer: Int, step: Step, theirs: String) = scenario()
        .withPlayers()
        .withTurnNumber(12)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, "Deadlock Trap")
        .withCardOnBattlefield(2, theirs)
        .withLandsOnBattlefield(2, "Mountain", 3)
        .build()
        .advanceToPriority(1, step)

    private fun TestGame.trapVerdict(theirs: String): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Deadlock Trap",
            activation = ActivateAbility(
                player1Id, findPermanent("Deadlock Trap")!!,
                intents.definition("Deadlock Trap")!!.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(findPermanent(theirs)!!)),
            ),
        )

    init {
        test("after our own combat, tapping a creature with no abilities buys nothing") {
            position(activePlayer = 1, step = Step.POSTCOMBAT_MAIN, theirs = "Grizzly Bears")
                .trapVerdict("Grizzly Bears") shouldBe TimingVerdict.NoWindow
        }

        test("on their turn before attacks, tapping their attacker is the window") {
            position(activePlayer = 2, step = Step.PRECOMBAT_MAIN, theirs = "Grizzly Bears")
                .trapVerdict("Grizzly Bears") shouldNotBe TimingVerdict.NoWindow
        }

        test("on our turn before blocks, tapping their blocker is the window") {
            position(activePlayer = 1, step = Step.PRECOMBAT_MAIN, theirs = "Grizzly Bears")
                .trapVerdict("Grizzly Bears") shouldNotBe TimingVerdict.NoWindow
        }

        test("a creature with an activated ability is worth locking in any window") {
            position(activePlayer = 1, step = Step.POSTCOMBAT_MAIN, theirs = "Prodigal Sorcerer")
                .trapVerdict("Prodigal Sorcerer") shouldNotBe TimingVerdict.NoWindow
        }
    }
}
