package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Geistblast (SOI #160) — {2}{R} Instant.
 *
 *   Geistblast deals 2 damage to any target.
 *   {2}{U}, Exile this card from your graveyard: Copy target instant or sorcery spell you control.
 *   You may choose new targets for the copy.
 *
 * The copy ability is activated from the graveyard, at instant speed, with the card's own exile as
 * part of the cost.
 */
class GeistblastScenarioTest : ScenarioTestBase() {
    private fun TestGame.activateFromGraveyard(spell: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findCardsInGraveyard(1, "Geistblast").single(),
            abilityId = cardRegistry.getCard("Geistblast")!!.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Spell(spell)),
        )
    )

    private fun game(geistblastInHand: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Island", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (geistblastInHand) builder.withCardInHand(1, "Geistblast") else builder.withCardInGraveyard(1, "Geistblast")
        return builder.build()
    }

    private fun TestGame.castBolt(): EntityId {
        castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
        return state.stack.last()
    }

    init {
        test("exiles itself from the graveyard to copy your spell, keeping its target") {
            val game = game()
            val bolt = game.castBolt()

            val result = game.activateFromGraveyard(bolt)
            withClue("activation: ${result.error}") { result.error shouldBe null }
            game.isInExile(1, "Geistblast") shouldBe true
            game.findCardsInGraveyard(1, "Geistblast").isEmpty() shouldBe true

            game.resolveStack()
            (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 14
            game.getLifeTotal(1) shouldBe 20
        }

        test("the copy may be given a new target") {
            val game = game()
            val bolt = game.castBolt()

            game.activateFromGraveyard(bolt).error shouldBe null
            game.resolveStack()
            (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 17
        }

        test("the ability can't be activated while Geistblast is in hand") {
            val game = game(geistblastInHand = true)
            val bolt = game.castBolt()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.state.getHand(game.player1Id).single(),
                    abilityId = cardRegistry.getCard("Geistblast")!!.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Spell(bolt)),
                )
            )
            result.error shouldNotBe null
        }
    }
}
