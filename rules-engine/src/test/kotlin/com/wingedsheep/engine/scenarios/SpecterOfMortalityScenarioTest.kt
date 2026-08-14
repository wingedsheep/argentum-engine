package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Specter of Mortality. */
class SpecterOfMortalityScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    /**
     * Twisted Fealty has two independent targets, which the base fixture's single-target
     * [ScenarioTestBase.TestGame.castSpell] can't express — cast it directly so the "up to one"
     * second target can be present or absent.
     */
    private fun TestGame.castTwistedFealty(targets: List<EntityId>) = execute(
        CastSpell(
            player1Id,
            findCardsInHand(1, "Twisted Fealty").first(),
            targets.map { ChosenTarget.Permanent(it) }
        )
    )

    init {
        context("Specter of Mortality — -X/-X for each creature card exiled from your graveyard") {
            test("exiling two creature cards shrinks every other creature by 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Specter of Mortality")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    // A noncreature card in the graveyard must not be selectable.
                    .withCardInGraveyard(1, "Pacifism")
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val thopter = game.findPermanent("Ornithopter").shouldNotBeNull()

                game.castSpell(1, "Specter of Mortality").error shouldBe null
                game.resolveStack()

                val specter = game.findPermanent("Specter of Mortality").shouldNotBeNull()
                val graveBears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                val graveLions = game.findCardsInGraveyard(1, "Savannah Lions").first()

                game.selectCards(listOf(graveBears, graveLions)).error shouldBe null
                game.resolveStack()

                withClue("both creature cards left the graveyard for exile") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Savannah Lions") shouldBe true
                }
                withClue("X = 2, so the 3/3 Hill Giant becomes 1/1") {
                    power(game, giant) shouldBe 1
                    toughness(game, giant) shouldBe 1
                }
                withClue("'each other creature' includes your own — the 0/2 Ornithopter dies") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }
                withClue("the Specter spares itself and stays 3/3") {
                    power(game, specter) shouldBe 3
                    toughness(game, specter) shouldBe 3
                }
            }

            test("declining the exile applies no modification at all") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Specter of Mortality")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(1, "Specter of Mortality").error shouldBe null
                game.resolveStack()

                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("nothing was exiled, so nothing shrinks") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    power(game, giant) shouldBe 3
                    toughness(game, giant) shouldBe 3
                }
            }
        }
    }
}
