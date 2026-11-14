package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Consul's Lieutenant (ORI #11) — {W}{W} Creature — Human Soldier 2/1.
 *
 *   First strike
 *   Renown 1
 *   Whenever this creature attacks, if it's renowned, other attacking creatures you control get
 *   +1/+1 until end of turn.
 *
 * The first card to gate a trigger on `Conditions.SourceIsRenowned` as an intervening-"if".
 */
class ConsulsLieutenantScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Consul's Lieutenant")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    private fun TestGame.attackWithLieutenantAndOneBears(renowned: Boolean): Triple<EntityId, EntityId, EntityId> {
        val lieutenant = findPermanent("Consul's Lieutenant")!!
        if (renowned) state = state.updateEntity(lieutenant) { it.with(RenownedComponent) }
        val (attackingBears, idleBears) = findPermanents("Grizzly Bears")
        execute(
            DeclareAttackers(player1Id, mapOf(lieutenant to player2Id, attackingBears to player2Id))
        ).error shouldBe null
        resolveStack()
        return Triple(lieutenant, attackingBears, idleBears)
    }

    init {
        test("not renowned: the intervening-if fails and no creature is pumped") {
            val game = board()
            val (lieutenant, attackingBears, idleBears) = game.attackWithLieutenantAndOneBears(renowned = false)

            val projected = game.state.projectedState
            projected.getPower(lieutenant) shouldBe 2
            projected.getPower(attackingBears) shouldBe 2
            projected.getPower(idleBears) shouldBe 2
        }

        test("renowned: other attacking creatures get +1/+1; itself and non-attackers don't") {
            val game = board()
            val (lieutenant, attackingBears, idleBears) = game.attackWithLieutenantAndOneBears(renowned = true)

            val projected = game.state.projectedState
            withClue("the attacking Bears is pumped") {
                projected.getPower(attackingBears) shouldBe 3
                projected.getToughness(attackingBears) shouldBe 3
            }
            withClue("\"other\" excludes the Lieutenant itself") {
                projected.getPower(lieutenant) shouldBe 2
                projected.getToughness(lieutenant) shouldBe 1
            }
            withClue("the Bears that stayed home isn't attacking") {
                projected.getPower(idleBears) shouldBe 2
            }
        }
    }
}
