package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gabriel Lorca, Mirror Menace — "Whenever you face a dilemma, you may pay {B}. If you do, return
 * this card from your graveyard to your hand." The trigger functions only from the graveyard and
 * watches only its owner's mode choices.
 */
class GabrielLorcaMirrorMenaceScenarioTest : ScenarioTestBase() {

    private val charm = card("Test Lorca Charm") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            effect = ModalEffect.chooseOne(
                Mode.noTarget(Effects.GainLife(1), "Gain 1 life"),
                Mode.noTarget(Effects.GainLife(2), "Gain 2 life"),
            )
        }
    }

    init {
        cardRegistry.register(listOf(charm))

        fun game(lorcaZone: String = "graveyard", caster: Int = 1): TestGame {
            val b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(caster, charm.name)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(caster)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (lorcaZone == "graveyard") b.withCardInGraveyard(1, "Gabriel Lorca, Mirror Menace")
            else b.withCardOnBattlefield(1, "Gabriel Lorca, Mirror Menace")
            return b.build()
        }

        fun TestGame.castCharm(player: Int) = execute(
            CastSpell(
                if (player == 1) player1Id else player2Id,
                findCardsInHand(player, charm.name).first(),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(emptyList()),
            )
        )

        context("Gabriel Lorca, Mirror Menace") {
            test("facing a dilemma with it in the graveyard and paying {B} returns it to hand") {
                val game = game()
                withClue("charm should cast") { game.castCharm(1).error shouldBe null }
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                game.findCardsInHand(1, "Gabriel Lorca, Mirror Menace").size shouldBe 1
                game.findCardsInGraveyard(1, "Gabriel Lorca, Mirror Menace").size shouldBe 0
            }

            test("declining to pay leaves it in the graveyard") {
                val game = game()
                game.castCharm(1).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findCardsInGraveyard(1, "Gabriel Lorca, Mirror Menace").size shouldBe 1
                game.findCardsInHand(1, "Gabriel Lorca, Mirror Menace").size shouldBe 0
            }

            test("does not trigger from the battlefield") {
                val game = game(lorcaZone = "battlefield")
                game.castCharm(1).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(1) shouldBe 21
            }

            test("does not trigger when an opponent faces a dilemma") {
                val game = game(caster = 2)
                game.castCharm(2).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.findCardsInGraveyard(1, "Gabriel Lorca, Mirror Menace").size shouldBe 1
            }
        }
    }
}
