package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Glyph Keeper (AKH #55) — "Whenever this creature becomes the target of a spell or ability for the
 * first time each turn, counter that spell or ability."
 *
 * Pins that the counter hits the *targeting* object (the spell, or the activated ability — never
 * the Sphinx itself), and that only the first targeting each turn is countered.
 */
class GlyphKeeperScenarioTest : ScenarioTestBase() {

    init {
        test("the first spell targeting it each turn is countered; the second resolves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glyph Keeper")
                .withCardInHand(2, "Shock")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val keeper = game.findPermanent("Glyph Keeper")
            keeper.shouldNotBeNull()

            game.castSpell(2, "Shock", keeper).error shouldBe null
            game.resolveStack()

            withClue("the first Shock was countered: in the graveyard, no damage dealt") {
                game.isInGraveyard(2, "Shock") shouldBe true
                (game.state.getEntity(keeper)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }

            game.castSpell(2, "Shock", keeper).error shouldBe null
            game.resolveStack()

            withClue("the second Shock this turn resolves and deals 2 to the 5/3") {
                game.graveyardSize(2) shouldBe 2
                game.findPermanent("Glyph Keeper") shouldBe keeper
                game.state.getEntity(keeper)?.get<DamageComponent>()?.amount shouldBe 2
            }
        }

        test("an activated ability targeting it is countered") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glyph Keeper")
                .withCardOnBattlefield(2, "Prodigal Sorcerer")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val keeper = game.findPermanent("Glyph Keeper")
            keeper.shouldNotBeNull()
            val tim = game.findPermanent("Prodigal Sorcerer")!!
            val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player2Id,
                    sourceId = tim,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, keeper))
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the ping was countered: no damage, the stack is empty") {
                (game.state.getEntity(keeper)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                game.state.stack.isEmpty() shouldBe true
            }
        }
    }
}
