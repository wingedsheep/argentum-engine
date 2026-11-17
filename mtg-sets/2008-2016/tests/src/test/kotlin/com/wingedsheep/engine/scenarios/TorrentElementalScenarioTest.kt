package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario test for Torrent Elemental (FRF #56) — {4}{U} Creature — Elemental 3/5.
 *
 *   Flying
 *   Whenever this creature attacks, tap all creatures defending player controls.
 *   {3}{B/G}{B/G}: Put this card from exile onto the battlefield tapped. Activate only as a sorcery.
 *
 * The exile ability works face up only: a card exiled face down has no characteristics
 * (CR 406.3a), so the engine neither offers nor accepts its printed ability.
 */
class TorrentElementalScenarioTest : ScenarioTestBase() {

    private fun TestGame.exiledElemental(): EntityId =
        state.getZone(player1Id, Zone.EXILE).single {
            state.getEntity(it)?.get<CardComponent>()?.name == "Torrent Elemental"
        }

    private fun TestGame.exileActions(id: EntityId) = getLegalActions(1).filter {
        (it.action as? ActivateAbility)?.sourceId == id
    }

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Torrent Elemental") {

            fun exileGame(phase: Phase = Phase.PRECOMBAT_MAIN, step: Step = Step.PRECOMBAT_MAIN) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInExile(1, "Torrent Elemental")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(phase, step)
                .build()

            test("face up in exile: pay {3}{B/G}{B/G} at sorcery speed and it enters tapped") {
                val game = exileGame()
                val elemental = game.exiledElemental()

                val action = game.exileActions(elemental).single().action
                val result = game.execute(action)
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val permanent = game.findPermanent("Torrent Elemental").shouldNotBeNull()
                withClue("it entered tapped") { game.isTapped(permanent) shouldBe true }
            }

            test("not offered at instant speed") {
                val game = exileGame(Phase.BEGINNING, Step.UPKEEP)
                game.exileActions(game.exiledElemental()).shouldBeEmpty()
            }

            test("face down in exile it has no abilities to activate") {
                val game = exileGame()
                val elemental = game.exiledElemental()
                val abilityId = cardRegistry.getCard("Torrent Elemental")!!.script.activatedAbilities.single().id
                game.state = game.state.updateEntity(elemental) { it.with(FaceDownComponent) }

                game.exileActions(elemental).shouldBeEmpty()
                val result = game.execute(ActivateAbility(game.player1Id, elemental, abilityId))
                result.error.shouldNotBeNull() shouldContain "face down"
                withClue("the card stays in exile") { game.state.getZone(game.player1Id, Zone.EXILE).contains(elemental) shouldBe true }
            }

            test("attacking taps all creatures defending player controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Torrent Elemental", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Horned Turtle")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val turtle = game.findPermanent("Horned Turtle")!!

                game.declareAttackers(mapOf("Torrent Elemental" to 2)).error shouldBe null
                game.resolveStack()

                withClue("both of the defending player's creatures are tapped") {
                    game.isTapped(giant) shouldBe true
                    game.isTapped(bears) shouldBe true
                }
                withClue("the attacker's own creatures are untouched") { game.isTapped(turtle) shouldBe false }
            }
        }
    }
}
