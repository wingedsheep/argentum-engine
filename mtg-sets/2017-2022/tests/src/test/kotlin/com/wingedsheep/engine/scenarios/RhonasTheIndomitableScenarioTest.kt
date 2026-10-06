package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rhonas the Indomitable (AKH #182) — {2}{G} 5/5 Legendary Creature — God.
 *
 *   Deathtouch, indestructible
 *   Rhonas can't attack or block unless you control another creature with power 4 or greater.
 *   {2}{G}: Another target creature gets +2/+0 and gains trample until end of turn.
 *
 * Proves Rhonas's own power 5 never satisfies "another", that the condition reads *projected*
 * power (its own pump turns a 3-power Hill Giant into an enabler), and that blocking is gated too.
 */
class RhonasTheIndomitableScenarioTest : ScenarioTestBase() {

    init {
        context("Rhonas the Indomitable") {

            test("cannot attack when its only other creature has power 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rhonas the Indomitable", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("Rhonas's own power must not count as 'another creature'") {
                    game.declareAttackers(mapOf("Rhonas the Indomitable" to 2)).error shouldNotBe null
                }
            }

            test("its pump makes Hill Giant a 5-power enabler, so Rhonas can attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rhonas the Indomitable", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rhonas = game.findPermanent("Rhonas the Indomitable")!!
                val giant = game.findPermanent("Hill Giant")!!
                val abilityId = cardRegistry.getCard("Rhonas the Indomitable")!!
                    .script.activatedAbilities[0].id

                withClue("'another target creature' — Rhonas can't target itself") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = rhonas,
                            abilityId = abilityId,
                            targets = listOf(entityIdToChosenTarget(game.state, rhonas)),
                        )
                    ).error shouldNotBe null
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rhonas,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(giant) shouldBe 5
                game.state.projectedState.getToughness(giant) shouldBe 3
                game.state.projectedState.hasKeyword(giant, Keyword.TRAMPLE.name) shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("projected power 5 on Hill Giant satisfies the condition") {
                    game.declareAttackers(mapOf("Rhonas the Indomitable" to 2)).error shouldBe null
                }
            }

            test("cannot block without another 4-power creature, can block with Craw Wurm") {
                val blocked = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Rhonas the Indomitable", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()
                blocked.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                blocked.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                blocked.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                blocked.declareBlockers(mapOf("Rhonas the Indomitable" to listOf("Grizzly Bears"))).error shouldNotBe null

                val allowed = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Rhonas the Indomitable", summoningSickness = false)
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()
                allowed.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                allowed.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                allowed.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                allowed.declareBlockers(mapOf("Rhonas the Indomitable" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }
    }
}
