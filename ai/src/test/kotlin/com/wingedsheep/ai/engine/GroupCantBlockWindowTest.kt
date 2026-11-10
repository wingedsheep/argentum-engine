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
 * The group form of the can't-block restriction window ([RestrictionWindowTest] covers the
 * targeted one): Immobilizer Eldrazi's "{2}{C}: Each creature with toughness greater than its
 * power can't block this turn" buys something only on our own turn, before blocks, and only when
 * an opposing creature the group holds could still block.
 */
class GroupCantBlockWindowTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val immobilizerAbility by lazy {
        intents.definition("Immobilizer Eldrazi")!!.script.activatedAbilities.single()
    }

    private fun TestGame.verdict(): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Immobilizer Eldrazi",
            activation = ActivateAbility(player1Id, findPermanent("Immobilizer Eldrazi")!!, immobilizerAbility.id),
        )

    private fun position(activePlayer: Int, opposing: String, ours: String? = null) = scenario()
        .withPlayers()
        .withTurnNumber(20)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Wastes", 3)
        .withCardOnBattlefield(1, "Immobilizer Eldrazi")
        .also { b -> ours?.let { b.withCardOnBattlefield(1, it) } }
        .withCardOnBattlefield(2, opposing)
        .withLandsOnBattlefield(2, "Swamp", 5)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("the group can't-block is a block denial the crack-back reads") {
            CombatReach.of(immobilizerAbility.effect) shouldContain CombatReach.BLOCK_DENIAL
        }

        test("on our turn before blocks, an opposing creature with toughness over power is the window") {
            position(activePlayer = 1, opposing = "Ondu Giant").verdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("on their turn the group keeps nothing out of a block") {
            position(activePlayer = 2, opposing = "Ondu Giant").verdict() shouldBe TimingVerdict.NoWindow
        }

        test("a group that holds no opposing creature buys nothing") {
            // Reaper from the Abyss is 6/6: toughness is not greater than power.
            position(activePlayer = 1, opposing = "Reaper from the Abyss").verdict() shouldBe TimingVerdict.NoWindow
        }

        test("a creature of ours in the group hands the decision back to the leaf") {
            position(activePlayer = 2, opposing = "Reaper from the Abyss", ours = "Ondu Giant")
                .verdict() shouldNotBe TimingVerdict.NoWindow
        }
    }
}
