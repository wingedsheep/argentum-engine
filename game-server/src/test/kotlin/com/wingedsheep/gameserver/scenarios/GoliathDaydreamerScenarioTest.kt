package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Goliath Daydreamer.
 *
 * Card reference:
 * - Goliath Daydreamer (2RR): Creature — Giant Wizard 4/4
 *   "Whenever you cast an instant or sorcery spell from your hand, exile that
 *    card with a dream counter on it instead of putting it into your graveyard
 *    as it resolves.
 *    Whenever this creature attacks, you may cast a spell from among cards you
 *    own in exile with dream counters on them without paying its mana cost."
 */
class GoliathDaydreamerScenarioTest : ScenarioTestBase() {

    init {
        context("Goliath Daydreamer") {
            test("instant cast from hand resolves and exiles with a dream counter") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Goliath Daydreamer")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                // Shock should not be in the graveyard.
                game.isInGraveyard(1, "Shock") shouldBe false

                // Shock should be in exile with one dream counter.
                val exileZone = game.state.getExile(game.player1Id)
                val shockId = exileZone.singleOrNull { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                } ?: error("Shock should be in player 1's exile")

                val counters = game.state.getEntity(shockId)?.get<CountersComponent>()
                counters shouldNotBe null
                counters!!.getCount(CounterType.DREAM) shouldBe 1

                // Player 2 took 2 damage (Shock resolved before it was exiled).
                game.getLifeTotal(2) shouldBe 18
            }

            test("exiled spell is linked to Goliath Daydreamer for UI tethering") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Goliath Daydreamer")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goliathId = game.state.getBattlefield(game.player1Id).single { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goliath Daydreamer"
                }

                // Cast Shock at player 2; resolves, then ends up in exile with a dream counter.
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                val firstShockId = game.state.getExile(game.player1Id).single { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                }

                // The exiled Shock should be recorded on Goliath Daydreamer's LinkedExileComponent.
                val linkedAfterFirst = game.state.getEntity(goliathId)?.get<LinkedExileComponent>()
                linkedAfterFirst shouldNotBe null
                linkedAfterFirst!!.exiledIds shouldContain firstShockId

                // A second cast should append, not replace.
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                val exiledShocks = game.state.getExile(game.player1Id).filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                }
                exiledShocks.size shouldBe 2
                val secondShockId = exiledShocks.first { it != firstShockId }

                val linkedAfterSecond = game.state.getEntity(goliathId)?.get<LinkedExileComponent>()
                linkedAfterSecond shouldNotBe null
                linkedAfterSecond!!.exiledIds shouldContain firstShockId
                linkedAfterSecond.exiledIds shouldContain secondShockId
            }

            test("attacking grants free-cast permission on the exiled dream-counter card") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Goliath Daydreamer")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shock at player 2; it gets exiled with a dream counter.
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                val shockId = game.state.getExile(game.player1Id).single { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                }

                // Move to the declare attackers step and swing with Goliath.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goliath Daydreamer" to 2))

                // Goliath's attack trigger goes on the stack. Resolving asks the
                // optional may (yes/no), then surfaces a SelectFromCollection
                // decision for choosing the dream-counter card to cast for free.
                game.resolveStack()
                game.answerYesNo(true)
                game.selectCards(listOf(shockId))
                game.resolveStack()

                // Shock should now have free-cast permission.
                game.state.mayPlayPermissions.firstOrNull {
                    shockId in it.cardIds && it.controllerId == game.player1Id
                } shouldNotBe null
                game.state.getEntity(shockId)?.get<PlayWithoutPayingCostComponent>() shouldBe
                    PlayWithoutPayingCostComponent(controllerId = game.player1Id)
            }
        }
    }
}
