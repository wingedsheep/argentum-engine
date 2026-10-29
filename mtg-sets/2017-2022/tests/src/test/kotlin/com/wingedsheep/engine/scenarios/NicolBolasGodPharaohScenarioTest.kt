package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.NicolBolasGodPharaoh
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Nicol Bolas, God-Pharaoh (HOU #140).
 *
 * +2 walks the *target opponent's* library to their first nonland card, exiling everything it
 * passes, and lets Bolas's controller — not the card's owner — cast that card free this turn.
 * −12 exiles only the opponents' nonland permanents.
 */
class NicolBolasGodPharaohScenarioTest : ScenarioTestBase() {

    private val plusTwo = NicolBolasGodPharaoh.activatedAbilities[0].id
    private val minusTwelve = NicolBolasGodPharaoh.activatedAbilities[3].id

    private fun TestGame.setLoyalty(id: EntityId, loyalty: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) }
    }

    private fun TestGame.namesIn(ids: List<EntityId>): List<String> =
        ids.map { state.getEntity(it)!!.get<CardComponent>()!!.name }

    init {
        context("Nicol Bolas, God-Pharaoh") {

            test("+2 exiles the opponent's library down to a nonland card, which you cast for free") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, God-Pharaoh")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, God-Pharaoh")!!
                game.setLoyalty(bolas, 7)

                game.execute(
                    ActivateAbility(
                        game.player1Id, bolas, plusTwo, targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("everything down to the first nonland card is exiled; the rest stays") {
                    game.namesIn(game.state.getExile(game.player2Id)) shouldContainExactlyInAnyOrder
                        listOf("Forest", "Swamp", "Grizzly Bears")
                    game.namesIn(game.state.getLibrary(game.player2Id)) shouldBe listOf("Island")
                }

                val bears = game.state.getExile(game.player2Id).single {
                    game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Grizzly Bears"
                }
                withClue("Bolas's controller casts the opponent's card without paying its mana cost") {
                    game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
                }
                game.resolveStack()
                game.state.getBattlefield(game.player1Id).contains(bears) shouldBe true
            }

            test("−12 exiles each nonland permanent opponents control and nothing else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, God-Pharaoh")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, God-Pharaoh")!!
                game.setLoyalty(bolas, 12)

                game.execute(ActivateAbility(game.player1Id, bolas, minusTwelve)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Forest") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
