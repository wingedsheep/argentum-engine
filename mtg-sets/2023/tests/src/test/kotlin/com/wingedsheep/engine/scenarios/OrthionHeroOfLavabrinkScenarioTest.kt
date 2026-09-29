package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.OrthionHeroOfLavabrink
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Orthion, Hero of Lavabrink (MOM #334) — two sorcery-speed copy abilities: one hasty token copy of
 * another creature you control for {1}{R}, or five for {6}{R}{R}{R}; sacrificed at the next end step.
 */
class OrthionHeroOfLavabrinkScenarioTest : ScenarioTestBase() {

    private val oneCopy = OrthionHeroOfLavabrink.activatedAbilities[0].id
    private val fiveCopies = OrthionHeroOfLavabrink.activatedAbilities[1].id

    init {
        context("Orthion, Hero of Lavabrink") {

            test("{1}{R}: one hasty copy, sacrificed at the end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orthion, Hero of Lavabrink")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val orthion = game.findPermanent("Orthion, Hero of Lavabrink")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, orthion, oneCopy, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                val token = game.findPermanents("Grizzly Bears").single { it != bears }
                withClue("the copy is a hasty token") {
                    game.state.getEntity(token)?.has<TokenComponent>() shouldBe true
                    game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
            }

            test("{6}{R}{R}{R}: five hasty copies, all sacrificed at the end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orthion, Hero of Lavabrink")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val orthion = game.findPermanent("Orthion, Hero of Lavabrink")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, orthion, fiveCopies, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Grizzly Bears").filter { it != bears }
                tokens.size shouldBe 5
                tokens.forEach { game.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe true }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
            }

            test("cannot target Orthion itself, nor activate at instant speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orthion, Hero of Lavabrink")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val orthion = game.findPermanent("Orthion, Hero of Lavabrink")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("not as a sorcery") {
                    game.execute(
                        ActivateAbility(game.player1Id, orthion, oneCopy, targets = listOf(ChosenTarget.Permanent(bears)))
                    ).error shouldNotBe null
                }
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("\"another\" excludes Orthion") {
                    game.execute(
                        ActivateAbility(game.player1Id, orthion, oneCopy, targets = listOf(ChosenTarget.Permanent(orthion)))
                    ).error shouldNotBe null
                }
            }
        }
    }
}
