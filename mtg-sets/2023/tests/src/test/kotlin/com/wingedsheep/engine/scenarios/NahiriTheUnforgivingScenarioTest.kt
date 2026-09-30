package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.NahiriTheUnforgiving
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nahiri, the Unforgiving (ONE #211).
 *
 * The first +1 makes a creature "attack a player each combat if able" until Nahiri's controller's
 * next turn: its controller must attack with it, and attacking Nahiri (a planeswalker) doesn't
 * satisfy the requirement while a player can be attacked. The grant wears off at the start of
 * Nahiri's controller's next turn. The 0 copies a graveyard creature whose mana value is below
 * Nahiri's loyalty, gives the token haste, and exiles it at the next end step.
 */
class NahiriTheUnforgivingScenarioTest : ScenarioTestBase() {

    private val mustAttackAbility = NahiriTheUnforgiving.activatedAbilities[0].id
    private val reanimateAbility = NahiriTheUnforgiving.activatedAbilities[2].id

    init {
        context("Nahiri, the Unforgiving") {

            test("+1: the target must attack a player — not attacking or attacking Nahiri is illegal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Unforgiving")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()
                val nahiri = game.findPermanent("Nahiri, the Unforgiving")!!
                seedLoyalty(game, nahiri, 5)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, nahiri, mustAttackAbility, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.activePlayerId shouldBe game.player2Id

                withClue("the Bears must attack") {
                    game.declareAttackers(emptyMap()).error shouldNotBe null
                }
                withClue("attacking Nahiri doesn't satisfy \"attacks a player\"") {
                    game.declareAttackersWithPermanentTargets(
                        permanentAttackers = mapOf("Grizzly Bears" to "Nahiri, the Unforgiving")
                    ).error shouldNotBe null
                }
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            }

            test("+1 wears off at the start of Nahiri's controller's next turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Unforgiving")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()
                val nahiri = game.findPermanent("Nahiri, the Unforgiving")!!
                seedLoyalty(game, nahiri, 5)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, nahiri, mustAttackAbility, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                withClue("the grant survives into the opponent's turn") {
                    game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.grantedStaticAbilities.any { it.entityId == bears } shouldBe true
                }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("and ends once Nahiri's controller's next turn begins") {
                    game.state.grantedStaticAbilities.any { it.entityId == bears } shouldBe false
                }
            }

            test("0: copies a creature card with mana value below Nahiri's loyalty, with haste, exiled at end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Unforgiving")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val nahiri = game.findPermanent("Nahiri, the Unforgiving")!!
                seedLoyalty(game, nahiri, 5)

                withClue("Craw Wurm (mana value 6) is not less than Nahiri's loyalty of 5") {
                    game.execute(
                        ActivateAbility(
                            game.player1Id, nahiri, reanimateAbility,
                            targets = listOf(ChosenTarget.Card(graveyardCard(game, "Craw Wurm"), game.player1Id, Zone.GRAVEYARD))
                        )
                    ).error shouldNotBe null
                }

                game.execute(
                    ActivateAbility(
                        game.player1Id, nahiri, reanimateAbility,
                        targets = listOf(ChosenTarget.Card(graveyardCard(game, "Hill Giant"), game.player1Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Hill Giant") shouldBe true
                val token = game.findPermanent("Hill Giant")
                token.shouldNotBeNull()
                game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("the token is exiled at the beginning of the next end step") {
                    game.findPermanent("Hill Giant") shouldBe null
                }
            }
        }
    }

    private fun graveyardCard(game: TestGame, name: String): EntityId =
        game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder skips the "enters with its starting loyalty" step, so seed it.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
