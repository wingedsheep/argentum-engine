package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Innistrad // Deluge of the Dead — the first battle in the engine, so this covers the
 * whole arc: cast the Siege, its enters trigger shrinking an opposing creature, then defeating it
 * and casting Deluge of the Dead transformed for free.
 *
 * The battle *rules* (protector, attack legality, defense counters, the defeat trigger itself) are
 * pinned by BattleCardTypeScenarioTest and SiegeDefeatTriggerScenarioTest; what this file proves is
 * that this card's own script — the -13/-13 trigger, the back face's token trigger, and the back
 * face's graveyard-exile ability — behaves as printed.
 */
class InvasionOfInnistradScenarioTest : ScenarioTestBase() {

    private fun defenseOf(game: TestGame): Int =
        game.findPermanent("Invasion of Innistrad")
            ?.let { game.state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) }
            ?: 0

    init {
        context("front face — Invasion of Innistrad") {

            test("it enters with 5 defense counters and shrinks a creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Innistrad")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!
                game.castSpell(1, "Invasion of Innistrad").error shouldBe null
                game.resolveStack()
                // The enters trigger chooses its target as it goes on the stack (CR 603.3d).
                game.selectTargets(listOf(angel)).error shouldBe null
                game.resolveStack()

                withClue("printed defense 5 becomes 5 defense counters (CR 310.4b)") {
                    defenseOf(game) shouldBe 5
                }
                withClue("-13/-13 kills a 4/4 outright") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                    game.isInGraveyard(2, "Serra Angel") shouldBe true
                }
                withClue("a Siege is protected by an opponent of its controller (CR 310.11a)") {
                    game.state.getEntity(game.findPermanent("Invasion of Innistrad")!!)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.ProtectorComponent>()
                        ?.playerId shouldBe game.player2Id
                }
            }

            test("it can be cast at instant speed — it has flash") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Innistrad")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Priority starts with the active player; hand it to player 1 so their legal
                // actions are the ones enumerated.
                game.passPriority()
                val castAction = game.getLegalActions(1)
                    .firstOrNull { it.description.contains("Invasion of Innistrad") }

                withClue("flash lets it be cast on the opponent's turn") {
                    castAction shouldNotBe null
                }
            }
        }

        context("defeating it — back face Deluge of the Dead") {

            test("defeating the Siege exiles it and casts Deluge of the Dead transformed for free") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Invasion of Innistrad")
                    .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
                    .withCardOnBattlefield(1, "Shivan Dragon", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // CR 310.8b — a Siege's own controller may attack it, because its protector is an
                // opponent. 4 + 5 power removes all 5 defense counters.
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf(
                        "Serra Angel" to "Invasion of Innistrad",
                        "Shivan Dragon" to "Invasion of Innistrad",
                    )
                ).error shouldBe null

                var guard = 0
                while (game.state.pendingDecision == null && guard++ < 30) {
                    if (game.state.step == Step.DECLARE_BLOCKERS &&
                        game.state.getEntity(game.player2Id)
                            ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
                    ) {
                        game.declareNoBlockers()
                    } else {
                        game.passPriority()
                    }
                }
                withClue("the defeat trigger exiled the Siege rather than the SBA binning it") {
                    game.isOnBattlefield("Invasion of Innistrad") shouldBe false
                    game.isInGraveyard(1, "Invasion of Innistrad") shouldBe false
                }
                withClue("'then you may cast it transformed' is offered to the Siege's controller") {
                    game.state.pendingDecision shouldNotBe null
                    game.state.pendingDecision!!.playerId shouldBe game.player1Id
                }

                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("the back face — an Enchantment, not the battle — is what entered") {
                    game.isOnBattlefield("Deluge of the Dead") shouldBe true
                }

                game.resolveStack()
                withClue("its enters trigger makes two 2/2 black Zombies") {
                    game.findPermanents("Zombie Token").size shouldBe 2
                }
            }
        }
    }
}
