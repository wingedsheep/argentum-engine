package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.ArcSpitter
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Arc Spitter (SNC #233) — {1} Artifact — Equipment
 *
 *   Equipped creature has "{1}: This creature deals 1 damage to target creature that's blocking it."
 *   Equip {1}
 *
 * The granted ability's target filter is source-relative (`blockingSource()`); granted onto the
 * host, its source is the equipped creature. Proves a creature blocking the equipped creature can
 * be targeted and takes the damage, while a creature blocking a *different* attacker cannot.
 */
class ArcSpitterScenarioTest : ScenarioTestBase() {

    private val grantedAbilityId by lazy {
        ArcSpitter.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id
    }

    /** Bears (equipped) and Lions attack; Hill Giant blocks Bears, Wall of Wood blocks Lions. */
    private fun blockedAttack(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
            .withCardAttachedTo(1, "Arc Spitter", "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
            .withCardOnBattlefield(2, "Wall of Wood", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()
        game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(
            mapOf("Hill Giant" to listOf("Grizzly Bears"), "Wall of Wood" to listOf("Savannah Lions"))
        ).error shouldBe null
        if (game.state.priorityPlayerId != game.player1Id) game.passPriority().error shouldBe null
        game.state.priorityPlayerId shouldBe game.player1Id
        return game
    }

    init {
        context("Arc Spitter") {

            test("equipped creature deals 1 damage to the creature blocking it") {
                val game = blockedAttack()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(
                    ActivateAbility(game.player1Id, bears, grantedAbilityId, listOf(ChosenTarget.Permanent(giant)))
                ).error shouldBe null
                game.resolveStack()
                withClue("Hill Giant took 1 damage from the equipped creature") {
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 1
                }
            }

            test("a creature blocking a different attacker is not a legal target") {
                val game = blockedAttack()
                val bears = game.findPermanent("Grizzly Bears")!!
                val wall = game.findPermanent("Wall of Wood")!!
                withClue("Wall of Wood blocks Savannah Lions, not the equipped Grizzly Bears") {
                    game.execute(
                        ActivateAbility(game.player1Id, bears, grantedAbilityId, listOf(ChosenTarget.Permanent(wall)))
                    ).error shouldNotBe null
                }
            }
        }
    }
}
