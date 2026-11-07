package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Wildest Dreams (KLD #174) — {X}{X}{G} Sorcery.
 *
 *   Return X target cards from your graveyard to your hand. Exile Wildest Dreams.
 *
 * Covers the cost-linked target count ({X} paid twice, X targets) with the self-exile on
 * resolution, and the 2016-09-20 ruling: with every target gone it doesn't resolve and goes to the
 * graveyard, not exile.
 */
class WildestDreamsScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.graveyardCard(name: String): EntityId =
        state.getGraveyard(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Wildest Dreams") {

            test("X = 2 returns exactly two target cards and exiles Wildest Dreams") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wildest Dreams")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Giant Growth")
                    .withCardInGraveyard(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dreams = game.findCardsInHand(1, "Wildest Dreams").first()
                val bears = game.graveyardCard("Grizzly Bears")
                val growth = game.graveyardCard("Giant Growth")

                val result = game.execute(
                    CastSpell(
                        game.player1Id, dreams,
                        listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(growth, game.player1Id, Zone.GRAVEYARD)
                        ),
                        2
                    )
                )
                withClue("{2}{2}{G} from five Forests: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Giant Growth") shouldBe true
                withClue("the untargeted card stays put") { game.isInGraveyard(1, "Island") shouldBe true }
                withClue("Wildest Dreams exiles itself") {
                    game.isInExile(1, "Wildest Dreams") shouldBe true
                    game.isInGraveyard(1, "Wildest Dreams") shouldBe false
                }
            }

            test("with every target gone it doesn't resolve and goes to the graveyard, not exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wildest Dreams")
                    .withCardOnBattlefield(1, "Scavenging Ooze")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dreams = game.findCardsInHand(1, "Wildest Dreams").first()
                val bears = game.graveyardCard("Grizzly Bears")

                val cast = game.execute(
                    CastSpell(
                        game.player1Id, dreams,
                        listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                        1
                    )
                )
                withClue("{1}{1}{G}: ${cast.error}") { cast.error shouldBe null }

                // In response, Scavenging Ooze exiles the only target.
                val ooze = game.findPermanent("Scavenging Ooze").shouldNotBeNull()
                val oozeAbility = cardRegistry.getCard("Scavenging Ooze")!!.activatedAbilities.first().id
                val activate = game.execute(
                    ActivateAbility(
                        game.player1Id, ooze, oozeAbility,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD))
                    )
                )
                withClue("Ooze's {G}: ${activate.error}") { activate.error shouldBe null }
                game.resolveStack()

                withClue("the target was exiled by the Ooze") { game.isInExile(1, "Grizzly Bears") shouldBe true }
                withClue("Wildest Dreams fizzled into the graveyard — not exiled") {
                    game.isInGraveyard(1, "Wildest Dreams") shouldBe true
                    game.isInExile(1, "Wildest Dreams") shouldBe false
                }
            }
        }
    }
}
