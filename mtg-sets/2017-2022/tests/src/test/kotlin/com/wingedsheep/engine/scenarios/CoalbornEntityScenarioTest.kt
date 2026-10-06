package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.j22.cards.CoalbornEntity
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Coalborn Entity (J22 #32) — {2}{R}: 1 damage to target creature token, player, or planeswalker.
 *
 * Proves the narrowed "permanent or player" union: a creature token, a planeswalker and a
 * player are legal targets, a nontoken creature is not.
 */
class CoalbornEntityScenarioTest : ScenarioTestBase() {
    init {
        val pingId = CoalbornEntity.activatedAbilities.first().id

        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Coalborn Entity")
            .withCardOnBattlefield(2, "Memnite", isToken = true)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("pings a creature token") {
            val game = board().build()
            val entity = game.findPermanent("Coalborn Entity")!!
            val token = game.findPermanent("Memnite")!!

            val r = game.execute(
                ActivateAbility(game.player1Id, entity, pingId, targets = listOf(ChosenTarget.Permanent(token)))
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.findPermanent("Memnite") shouldBe null
        }

        test("pings a player") {
            val game = board().build()
            val entity = game.findPermanent("Coalborn Entity")!!

            val r = game.execute(
                ActivateAbility(game.player1Id, entity, pingId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
        }

        test("pings a planeswalker") {
            val game = board().withCardOnBattlefield(2, "Liliana, Dreadhorde General").build()
            val entity = game.findPermanent("Coalborn Entity")!!
            val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
            // The scenario builder doesn't run "enters with loyalty"; seed it.
            game.state = game.state.updateEntity(liliana) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
            }

            val r = game.execute(
                ActivateAbility(game.player1Id, entity, pingId, targets = listOf(ChosenTarget.Permanent(liliana)))
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            game.state.getEntity(liliana)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5
        }

        test("cannot target a nontoken creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Coalborn Entity")
                .withCardOnBattlefield(2, "Memnite")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val entity = game.findPermanent("Coalborn Entity")!!
            val memnite = game.findPermanent("Memnite")!!

            game.execute(
                ActivateAbility(game.player1Id, entity, pingId, targets = listOf(ChosenTarget.Permanent(memnite)))
            ).error shouldNotBe null
        }
    }
}
