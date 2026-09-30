package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Annihilating Glare (ONE #80) — {B} Sorcery.
 *
 *   "As an additional cost to cast this spell, pay {4} or sacrifice an artifact or creature.
 *    Destroy target creature or planeswalker."
 *
 * Mode 0 = pay {4}; mode 1 = sacrifice an artifact or creature.
 */
class AnnihilatingGlareScenarioTest : ScenarioTestBase() {

    private fun glareId(game: TestGame): EntityId =
        game.state.getHand(game.player1Id).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == "Annihilating Glare"
        }

    private fun cast(game: TestGame, target: EntityId, mode: Int, sacrificed: List<EntityId> = emptyList()) =
        listOf<ChosenTarget>(ChosenTarget.Permanent(target)).let { targets ->
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = glareId(game),
                    targets = targets,
                    chosenModes = listOf(mode),
                    modeTargetsOrdered = listOf(targets),
                    additionalCostPayment = if (sacrificed.isEmpty()) null
                    else AdditionalCostPayment(sacrificedPermanents = sacrificed)
                )
            )
        }

    init {
        context("Annihilating Glare") {

            test("pay {4}: destroys target planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInHand(1, "Annihilating Glare")
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                cast(game, game.findPermanent("Jace Beleren")!!, mode = 0).error shouldBe null
                game.resolveStack()

                withClue("the planeswalker was destroyed") {
                    game.isOnBattlefield("Jace Beleren") shouldBe false
                    game.isInGraveyard(2, "Jace Beleren") shouldBe true
                }
            }

            test("pay {4} can't be cast with only {B} available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(1, "Annihilating Glare")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                cast(game, game.findPermanent("Grizzly Bears")!!, mode = 0).error shouldNotBe null
            }

            test("sacrifice an artifact: costs only {B} and destroys target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(1, "Annihilating Glare")
                    .withCardOnBattlefield(1, "Welding Jar")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jar = game.findPermanent("Welding Jar")!!
                cast(game, game.findPermanent("Grizzly Bears")!!, mode = 1, sacrificed = listOf(jar))
                    .error shouldBe null
                game.resolveStack()

                withClue("the artifact was sacrificed") {
                    game.isInGraveyard(1, "Welding Jar") shouldBe true
                }
                withClue("the creature was destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("sacrifice a creature: destroys target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(1, "Annihilating Glare")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fodder = game.findPermanent("Grizzly Bears")!!
                cast(game, game.findPermanent("Hill Giant")!!, mode = 1, sacrificed = listOf(fodder))
                    .error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }
    }
}
