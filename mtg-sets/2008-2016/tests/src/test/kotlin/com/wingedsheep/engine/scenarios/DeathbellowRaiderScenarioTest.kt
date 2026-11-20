package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.DeathbellowRaider
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deathbellow Raider (THS #117) — {1}{R} Creature — Minotaur Berserker 2/3.
 *
 *   This creature attacks each combat if able.
 *   {2}{B}: Regenerate this creature.
 */
class DeathbellowRaiderScenarioTest : ScenarioTestBase() {
    private val regenerateId = DeathbellowRaider.activatedAbilities.single().id

    init {
        test("must attack: declaring no attackers is rejected") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Deathbellow Raider")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, emptyMap())).error shouldNotBe null
            game.declareAttackers(mapOf("Deathbellow Raider" to 2)).error shouldBe null
        }

        test("{2}{B} regenerates it through lethal damage") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Deathbellow Raider")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val raider = game.findPermanent("Deathbellow Raider")!!
            game.execute(ActivateAbility(game.player1Id, raider, regenerateId)).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Lightning Bolt", raider).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Deathbellow Raider") shouldBe true
        }
    }
}
