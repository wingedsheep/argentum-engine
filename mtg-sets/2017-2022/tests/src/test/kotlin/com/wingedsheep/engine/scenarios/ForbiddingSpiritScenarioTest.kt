package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Forbidding Spirit (RNA #9) — ETB: until your next turn, creatures can't attack you or
 * planeswalkers you control unless their controller pays {2} for each of those creatures.
 */
class ForbiddingSpiritScenarioTest : ScenarioTestBase() {
    init {
        test("the opponent pays {2} per attacker on their next turn, even after the Spirit is gone") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Forbidding Spirit")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpell(1, "Forbidding Spirit").error shouldBe null
            game.resolveStack()

            val spirit = game.findPermanent("Forbidding Spirit")!!
            game.state = game.state.moveToZone(
                spirit, ZoneKey(game.player1Id, Zone.BATTLEFIELD), ZoneKey(game.player1Id, Zone.GRAVEYARD)
            )

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player2Id
            val bears = game.findPermanents("Grizzly Bears")
            game.execute(DeclareAttackers(game.player2Id, bears.associateWith { game.player1Id })).error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                .requiredCost shouldBe "{1}{1}{1}{1}"
        }
    }
}
