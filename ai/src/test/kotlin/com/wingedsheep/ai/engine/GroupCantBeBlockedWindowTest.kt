package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The team-wide unblockable grant ([com.wingedsheep.sdk.scripting.effects.CantBeBlockedGroupEffect]):
 * Glaring Spotlight's "{3}, Sacrifice this artifact: Creatures you control gain hexproof until end of
 * turn and can't be blocked this turn". The evasion is spent only by an attack of ours this turn, so
 * the sacrifice buys something on our turn before blocks and nothing on the opponent's turn — and
 * combat sees it as blocks denied when an opponent holds it.
 */
class GroupCantBeBlockedWindowTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val spotlightAbility by lazy {
        intents.definition("Glaring Spotlight")!!.script.activatedAbilities.single()
    }

    private fun TestGame.verdict(): TimingVerdict =
        HoldPolicy(intents, refuseUnspendableGrants = true, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Glaring Spotlight",
            activation = ActivateAbility(player1Id, findPermanent("Glaring Spotlight")!!, spotlightAbility.id),
        )

    private fun position(activePlayer: Int) = scenario()
        .withPlayers()
        .withTurnNumber(20)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Glaring Spotlight")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(2, "Mountain", 5)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("the team-wide can't-be-blocked is a block denial combat reads") {
            CombatReach.of(spotlightAbility.effect) shouldContain CombatReach.BLOCK_DENIAL
        }

        test("on our turn before attacks, an untapped creature of ours is the window") {
            position(activePlayer = 1).verdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("on their turn no attack of ours can be made unblockable") {
            position(activePlayer = 2).verdict() shouldBe TimingVerdict.NoWindow
        }
    }
}
