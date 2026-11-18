package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ajani's Presence (JOU #2) — {W} Instant.
 *
 * "Strive — This spell costs {2}{W} more to cast for each target beyond the first.
 *  Any number of target creatures each get +1/+1 and gain indestructible until end of turn."
 *
 * The strive tax is one per-target {2}{W} increase carrying a generic and a colored part, so the
 * price of a second target is pinned from every side: short on generic, short on white, and
 * exactly enough.
 */
class AjanisPresenceScenarioTest : ScenarioTestBase() {

    private fun TestGame.castPresence(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Ajani's Presence"
            },
            targets = targets.map { ChosenTarget.Permanent(it) },
        )
    )

    private fun TestGame.assertBuffed(id: EntityId, power: Int, toughness: Int) {
        val projected = state.projectedState
        projected.getPower(id) shouldBe power
        projected.getToughness(id) shouldBe toughness
        projected.hasKeyword(id, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    init {
        context("Ajani's Presence") {

            test("one target costs the printed {W} and gets +1/+1 and indestructible") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Presence")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castPresence(listOf(bears))
                withClue("a single target owes nothing extra: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.assertBuffed(bears, 3, 3)
            }

            test("a second target is rejected when the {2} generic half can't be paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Presence")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castPresence(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost {2}{W}{W}; three lands can't pay it") { cast.error.shouldNotBeNull() }
                withClue("a rejected cast leaves the spell in hand") { game.handSize(1) shouldBe 1 }
            }

            test("a second target is rejected when the extra {W} can't be paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Presence")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castPresence(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets need two white mana; one Plains can't pay it") { cast.error.shouldNotBeNull() }
                withClue("a rejected cast leaves the spell in hand") { game.handSize(1) shouldBe 1 }
            }

            test("two targets on {2}{W}{W} buff both, including an opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Presence")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val cast = game.castPresence(listOf(bears, lions))
                withClue("{2}{W}{W} is payable here: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.assertBuffed(bears, 3, 3)
                game.assertBuffed(lions, 2, 2)
            }

            test("no targets is a legal cast for the printed {W}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Presence")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castPresence(emptyList())
                withClue("\"any number of target creatures\" includes none: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("nothing was targeted, so nothing is buffed") {
                    game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
                }
            }
        }
    }
}
