package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.WickedAkuba
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wicked Akuba (CHK #150) — "{B}: Target player dealt damage by this creature this turn loses
 * 1 life."
 *
 * Only a player Wicked Akuba has dealt damage to this turn is a legal target.
 */
class WickedAkubaScenarioTest : ScenarioTestBase() {

    private val drainAbility = WickedAkuba.activatedAbilities.single().id

    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Wicked Akuba")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.drain(playerId: EntityId) = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Wicked Akuba")!!,
                abilityId = drainAbility,
                targets = listOf(ChosenTarget.Player(playerId)),
            )
        )

        fun TestGame.connect() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Wicked Akuba" to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            declareNoBlockers().error shouldBe null
            passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        }

        context("Wicked Akuba") {
            test("drains a player it dealt combat damage to this turn, repeatably") {
                val game = base().build()
                game.connect()
                withClue("Akuba connected") { game.getLifeTotal(2) shouldBe 18 }

                game.drain(game.player2Id).error shouldBe null
                game.resolveStack()
                game.drain(game.player2Id).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
            }

            test("can't target a player it hasn't dealt damage to this turn") {
                val game = base().build()

                game.drain(game.player2Id).error shouldNotBe null
                game.drain(game.player1Id).error shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }

            test("the ability still resolves after Wicked Akuba leaves the battlefield (last-known information)") {
                val game = base().withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1).build()
                game.connect()

                game.drain(game.player2Id).error shouldBe null
                game.castSpell(1, "Shock", game.findPermanent("Wicked Akuba")!!).error shouldBe null
                game.resolveStack()

                withClue("Akuba died before its ability resolved") { game.isInGraveyard(1, "Wicked Akuba") shouldBe true }
                game.getLifeTotal(2) shouldBe 17
            }

            test("the player it damaged is the only legal target") {
                val game = base().build()
                game.connect()

                game.drain(game.player1Id).error shouldNotBe null
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
