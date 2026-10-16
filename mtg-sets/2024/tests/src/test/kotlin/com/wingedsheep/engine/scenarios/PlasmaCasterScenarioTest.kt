package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Plasma Caster (PIP #64) — {1}{R} Artifact — Equipment
 *
 *   Equipped creature gets +1/+1.
 *   Whenever equipped creature attacks, you get {E}{E}.
 *   Pay {E}{E}: Choose target creature that's blocking equipped creature. Flip a coin. If you win
 *   the flip, exile the chosen creature. Otherwise, this Equipment deals 1 damage to it.
 *   Equip {2}
 *
 * Proves the "blocking equipped creature" target restriction (a blocker read relative to the
 * Equipment's host, not the Equipment itself), the energy payment, both coin-flip branches, and the
 * two resolution rulings: the target is judged against the creature that was equipped when it was
 * chosen (moving the Equipment in response doesn't stop the ability), and a target that stops
 * blocking that creature makes the ability fizzle without a flip.
 */
class PlasmaCasterScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Plasma Caster")!!.script.activatedAbilities.first { !it.isEquipAbility }.id
    }

    private fun energy(game: TestGame): Int =
        game.state.getEntity(game.player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    /** Bears (equipped) attack; Hill Giant blocks them, Wall of Wood stays home. Stops in declare blockers. */
    private fun blockedAttack(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
            .withCardAttachedTo(1, "Plasma Caster", "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
            .withCardOnBattlefield(2, "Wall of Wood", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
        // Hand priority back to the attacker so it can activate during declare blockers.
        if (game.state.priorityPlayerId != game.player1Id) game.passPriority().error shouldBe null
        game.state.priorityPlayerId shouldBe game.player1Id
        return game
    }

    /** Activates against Hill Giant and returns the resolution's coin-flip event, if any. */
    private fun activateAgainstGiant(game: TestGame, beforeResolving: (TestGame) -> Unit): CoinFlipEvent? {
        val caster = game.findPermanent("Plasma Caster")!!
        val giant = game.findPermanent("Hill Giant")!!
        game.execute(
            ActivateAbility(game.player1Id, caster, abilityId, listOf(ChosenTarget.Permanent(giant)))
        ).error shouldBe null
        beforeResolving(game)
        return game.resolveStack().flatMap { it.events }.filterIsInstance<CoinFlipEvent>().firstOrNull()
    }

    init {
        context("Plasma Caster") {

            test("equipped creature gets +1/+1 and attacking gives two energy") {
                val game = blockedAttack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
                withClue("the attack trigger paid out {E}{E}") { energy(game) shouldBe 2 }
            }

            test("only a creature blocking the equipped creature can be targeted") {
                val game = blockedAttack()
                val caster = game.findPermanent("Plasma Caster")!!
                val wall = game.findPermanent("Wall of Wood")!!
                withClue("Wall of Wood is not blocking — illegal target") {
                    game.execute(
                        ActivateAbility(game.player1Id, caster, abilityId, listOf(ChosenTarget.Permanent(wall)))
                    ).error shouldNotBe null
                }
                energy(game) shouldBe 2
            }

            test("win: blocker is exiled; lose: Plasma Caster deals 1 damage to it") {
                var sawWin = false
                var sawLoss = false
                repeat(60) {
                    if (sawWin && sawLoss) return@repeat
                    val game = blockedAttack()
                    val caster = game.findPermanent("Plasma Caster")!!
                    val giant = game.findPermanent("Hill Giant")!!
                    game.execute(
                        ActivateAbility(game.player1Id, caster, abilityId, listOf(ChosenTarget.Permanent(giant)))
                    ).error shouldBe null
                    withClue("{E}{E} paid") { energy(game) shouldBe 0 }

                    val coin = game.resolveStack().flatMap { it.events }
                        .filterIsInstance<CoinFlipEvent>().firstOrNull() ?: return@repeat
                    if (coin.won) {
                        sawWin = true
                        game.isInExile(2, "Hill Giant") shouldBe true
                        game.isOnBattlefield("Hill Giant") shouldBe false
                    } else {
                        sawLoss = true
                        game.isOnBattlefield("Hill Giant") shouldBe true
                        game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 1
                    }
                }
                withClue("observed both flip outcomes") {
                    sawWin shouldBe true
                    sawLoss shouldBe true
                }
            }

            test("moving the Equipment to another creature in response doesn't stop the ability") {
                val game = blockedAttack()
                val coin = activateAgainstGiant(game) { g ->
                    // Re-equip onto Savannah Lions (not in combat) with the ability on the stack.
                    val caster = g.findPermanent("Plasma Caster")!!
                    val bears = g.findPermanent("Grizzly Bears")!!
                    val lions = g.findPermanent("Savannah Lions")!!
                    g.state = g.state
                        .updateEntity(caster) { it.with(AttachedToComponent(lions)) }
                        .updateEntity(bears) { it.without<AttachmentsComponent>() }
                        .updateEntity(lions) { it.with(AttachmentsComponent(listOf(caster))) }
                }
                withClue("Hill Giant still blocks the creature the Caster equipped at activation") {
                    coin shouldNotBe null
                }
                if (coin!!.won) {
                    game.isInExile(2, "Hill Giant") shouldBe true
                } else {
                    game.state.getEntity(game.findPermanent("Hill Giant")!!)
                        ?.get<DamageComponent>()?.amount shouldBe 1
                }
            }

            test("a target that stops blocking the equipped creature fizzles the ability without a flip") {
                val game = blockedAttack()
                val coin = activateAgainstGiant(game) { g ->
                    val giant = g.findPermanent("Hill Giant")!!
                    g.state = g.state.updateEntity(giant) { it.without<BlockingComponent>() }
                }
                withClue("no coin is flipped") { coin shouldBe null }
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.state.getEntity(game.findPermanent("Hill Giant")!!)
                    ?.get<DamageComponent>()?.amount shouldBe null
                withClue("the {E}{E} cost stays paid") { energy(game) shouldBe 0 }
            }
        }
    }
}
