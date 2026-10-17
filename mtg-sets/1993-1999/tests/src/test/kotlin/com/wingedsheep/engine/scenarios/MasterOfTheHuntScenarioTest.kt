package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Master of the Hunt (LEG) — {2}{G}{G} Creature — Human 2/2: "{2}{G}{G}: Create a 1/1 green Wolf
 * creature token named Wolves of the Hunt. It has 'bands with other creatures named Wolves of the
 * Hunt.'" (CR 702.22b)
 */
class MasterOfTheHuntScenarioTest : ScenarioTestBase() {

    private val wolvesKeyword = KeywordAbility.bandsWithOtherNamed("Wolves of the Hunt").projectedKeyword

    /** Activate the Master [times] times, resolving each, and return the Wolves ready to attack. */
    private fun TestGame.huntWolves(times: Int): List<EntityId> {
        val master = findPermanent("Master of the Hunt")!!
        val abilityId = cardRegistry.getCard("Master of the Hunt")!!.script.activatedAbilities.single().id
        repeat(times) {
            execute(ActivateAbility(player1Id, master, abilityId)).error shouldBe null
            resolveStack()
        }
        val wolves = findAllPermanents("Wolves of the Hunt")
        wolves.forEach { wolf -> state = state.updateEntity(wolf) { it.without<SummoningSicknessComponent>() } }
        return wolves
    }

    private fun TestGame.declareBand(members: List<EntityId>) = execute(
        DeclareAttackers(player1Id, members.associateWith { player2Id }, bands = listOf(members.toSet()))
    )

    init {
        context("Master of the Hunt") {
            test("creates 1/1 green Wolves of the Hunt that band with each other") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Master of the Hunt")
                    .withLandsOnBattlefield(1, "Forest", 12)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolves = game.huntWolves(3)
                wolves shouldHaveSize 3
                wolves.forEach { wolf ->
                    game.state.projectedState.getPower(wolf) shouldBe 1
                    game.state.projectedState.hasKeyword(wolf, wolvesKeyword) shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareBand(wolves).error shouldBe null
                val bandIds = wolves.map { game.state.getEntity(it)?.get<AttackingComponent>()?.bandId }
                bandIds.first().shouldNotBeNull()
                bandIds.toSet() shouldHaveSize 1
            }

            test("a creature not named Wolves of the Hunt can't join their band") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Master of the Hunt")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolves = game.huntWolves(2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareBand(wolves + game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
                game.declareBand(wolves + game.findPermanent("Master of the Hunt")!!).error shouldNotBe null
            }
        }
    }
}
