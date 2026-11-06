package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.embalmAbility
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Embalmer's Tools (AKH #227) — {2} Artifact.
 *
 *   Activated abilities of creature cards in your graveyard cost {1} less to activate.
 *   Tap an untapped Zombie you control: Target player mills a card.
 *
 * The first card to point an activated-ability reduction at the graveyard: embalm is activated from
 * there, so it must be priced {1} cheaper both in the legal-action menu and by the activation
 * handler. The reduction trims generic mana only, so a coloured-only embalm keeps its full cost.
 */
class EmbalmersToolsScenarioTest : ScenarioTestBase() {

    private fun TestGame.embalmOffered(card: EntityId): Boolean =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == card && it.isAffordable }

    private fun TestGame.embalm(card: EntityId, cost: String) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = card,
            abilityId = embalmAbility(ManaCost.parse(cost)).id,
        )
    )

    init {
        test("embalm from the graveyard costs {1} less") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Embalmer's Tools")
                .withCardInGraveyard(1, "Anointer Priest")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val priest = game.findCardsInGraveyard(1, "Anointer Priest").single()
            withClue("three Plains pay embalm {3}{W} reduced to {2}{W}, so it is offered") {
                game.embalmOffered(priest) shouldBe true
            }
            withClue("the handler charges the reduced cost too") {
                game.embalm(priest, "{3}{W}").error shouldBe null
            }
            game.resolveStack()

            game.isInExile(1, "Anointer Priest") shouldBe true
            game.findPermanent("Anointer Priest") shouldNotBe null
            game.findAllPermanents("Plains").all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        }

        test("without the Tools, three Plains can't pay the printed embalm cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Anointer Priest")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val priest = game.findCardsInGraveyard(1, "Anointer Priest").single()
            game.embalmOffered(priest) shouldBe false
            game.embalm(priest, "{3}{W}").error shouldNotBe null
        }

        test("a coloured-only embalm cost is not reduced") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Embalmer's Tools")
                .withCardInGraveyard(1, "Sacred Cat")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cat = game.findCardsInGraveyard(1, "Sacred Cat").single()
            withClue("embalm {W} has no generic mana to trim, so it isn't free") {
                game.embalmOffered(cat) shouldBe false
                game.embalm(cat, "{W}").error shouldNotBe null
            }
            game.isInGraveyard(1, "Sacred Cat") shouldBe true
        }

        test("tapping an untapped Zombie mills the target player one card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Embalmer's Tools")
                .withCardOnBattlefield(1, "Gravedigger")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tools = game.findPermanent("Embalmer's Tools")!!
            val zombie = game.findPermanent("Gravedigger")!!
            val abilityId = cardRegistry.getCard("Embalmer's Tools")!!.activatedAbilities.first().id
            val librarySize = game.librarySize(2)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = tools,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(zombie)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(zombie)!!.has<TappedComponent>() shouldBe true
            game.librarySize(2) shouldBe librarySize - 1
            game.isInGraveyard(2, "Island") shouldBe true
        }
    }
}
