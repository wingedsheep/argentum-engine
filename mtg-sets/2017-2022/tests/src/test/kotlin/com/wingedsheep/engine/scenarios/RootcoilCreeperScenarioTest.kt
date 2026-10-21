package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.RootcoilCreeper
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rootcoil Creeper (MID) — its "two mana of any one color" carries
 * `ManaRestriction.CastFromGraveyardOnly`: it pays a flashback cast from your graveyard but not a
 * spell from hand. Its exile ability returns only a card with flashback you own from exile.
 */
class RootcoilCreeperScenarioTest : ScenarioTestBase() {

    private fun TestGame.exiled(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).first { state.getEntity(it)?.get<CardComponent>()?.name == name }
    }

    init {
        context("Rootcoil Creeper") {

            test("its restricted mana pays a flashback cast from the graveyard") {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rootcoil Creeper")
                    .withCardInGraveyard(1, "Otherworldly Gaze")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { b = b.withCardInLibrary(1, "Island") }
                repeat(5) { b = b.withCardInLibrary(2, "Island") }
                val game = b.build()

                val gaze = game.findCardsInGraveyard(1, "Otherworldly Gaze").single()
                withClue("flashback {1}{U} is offered as affordable off the Creeper alone") {
                    game.getLegalActions(1).single {
                        it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == gaze
                    }.isAffordable shouldBe true
                }

                val result = game.execute(
                    CastSpell(
                        game.player1Id, gaze,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                game.state.getEntity(game.findPermanent("Rootcoil Creeper")!!)?.get<TappedComponent>() shouldNotBe null
                game.state.stack.size shouldBe 1
            }

            test("its restricted mana can't pay for a spell cast from hand") {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rootcoil Creeper")
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { b = b.withCardInLibrary(1, "Island") }
                repeat(5) { b = b.withCardInLibrary(2, "Island") }
                val game = b.build()

                withClue("the Creeper's two graveyard-only mana can't cast Grizzly Bears' {1}{G} from hand") {
                    game.castSpell(1, "Grizzly Bears").error shouldNotBe null
                }
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("exiling it returns only a card with flashback you own from exile to your hand") {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rootcoil Creeper")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInExile(1, "Otherworldly Gaze")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInExile(2, "Otherworldly Gaze")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { b = b.withCardInLibrary(1, "Island") }
                repeat(5) { b = b.withCardInLibrary(2, "Island") }
                val game = b.build()

                val creeper = game.findPermanent("Rootcoil Creeper")!!
                val returnAbility = RootcoilCreeper.activatedAbilities[2]
                val ownGaze = game.exiled(1, "Otherworldly Gaze")

                val offer = game.getLegalActions(1).single {
                    (it.action as? ActivateAbility)?.abilityId == returnAbility.id
                }
                withClue("only your own flashback card in exile is a legal target") {
                    offer.validTargets!!.shouldContainExactly(ownGaze)
                }

                val result = game.execute(
                    ActivateAbility(
                        game.player1Id, creeper, returnAbility.id,
                        listOf(ChosenTarget.Card(ownGaze, game.player1Id, Zone.EXILE))
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInHand(1, "Otherworldly Gaze") shouldBe true
                game.isInExile(1, "Rootcoil Creeper") shouldBe true
                game.isInExile(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
