package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.EmblemStaticAbilityComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wrenn and Realmbreaker (MOM #217, {1}{G}{G}, loyalty 4).
 *
 *   Lands you control have "{T}: Add one mana of any color."
 *   +1: Up to one target land you control becomes a 3/3 Elemental creature with vigilance,
 *       hexproof, and haste until your next turn. It's still a land.
 *   −2: Mill three cards. You may put a permanent card from among the milled cards into your hand.
 *   −7: You get an emblem with "You may play lands and cast permanent spells from your graveyard."
 *
 * The emblem lives outside every zone, so the graveyard land-play and cast scans have to read it
 * directly — the emblem tests pin both halves, and that the permission stops at permanent spells
 * and at its owner's own graveyard.
 */
class WrennAndRealmbreakerScenarioTest : ScenarioTestBase() {

    init {
        context("Wrenn and Realmbreaker") {

            test("lands you control tap for a color they don't make") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Realmbreaker")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Lightning Bolt").first(),
                        listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a Forest paid {R} for Lightning Bolt") { game.getLifeTotal(2) shouldBe 17 }
            }

            test("+1 turns a land into a 3/3 vigilance, hexproof, haste Elemental land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Realmbreaker")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Realmbreaker")!!
                val forest = game.findPermanent("Forest")!!
                setLoyalty(game, wrenn, 4)
                activate(game, wrenn, index = 0, targets = listOf(forest))
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the Forest is a 3/3 Elemental creature land with the three keywords") {
                    projected.isCreature(forest) shouldBe true
                    projected.hasType(forest, "LAND") shouldBe true
                    projected.hasSubtype(forest, "Elemental") shouldBe true
                    projected.getPower(forest) shouldBe 3
                    projected.getToughness(forest) shouldBe 3
                    projected.hasKeyword(forest, Keyword.VIGILANCE) shouldBe true
                    projected.hasKeyword(forest, Keyword.HEXPROOF) shouldBe true
                    projected.hasKeyword(forest, Keyword.HASTE) shouldBe true
                }
                loyalty(game, wrenn) shouldBe 5
            }

            test("−2 mills three and returns a permanent card among them") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Realmbreaker")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Realmbreaker")!!
                setLoyalty(game, wrenn, 4)
                activate(game, wrenn, index = 1)
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears went to hand; the other two stayed milled") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 1
                }
                loyalty(game, wrenn) shouldBe 2
            }

            test("the −7 emblem plays lands and casts permanent spells from your graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Realmbreaker")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Realmbreaker")!!
                val yardForest = game.findCardsInGraveyard(1, "Forest").first()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").first()
                val giant = game.findCardsInGraveyard(2, "Hill Giant").first()

                withClue("before the emblem, nothing in the graveyard is playable") {
                    graveyardActionCards(game) shouldBe emptySet()
                }

                setLoyalty(game, wrenn, 7)
                activate(game, wrenn, index = 2)
                game.resolveStack()

                withClue("Wrenn died at 0 loyalty; the emblem outlives her") {
                    game.findPermanent("Wrenn and Realmbreaker") shouldBe null
                    game.state.entities.values.count { it.get<EmblemStaticAbilityComponent>() != null } shouldBe 1
                }
                // Wrenn herself is a permanent card in the graveyard now, so she can be recast too.
                val deadWrenn = game.findCardsInGraveyard(1, "Wrenn and Realmbreaker").first()
                withClue("the land and the permanent cards are offered — not the instant, not the opponent's card") {
                    graveyardActionCards(game) shouldBe setOf(yardForest, bears, deadWrenn)
                }

                game.execute(PlayLand(game.player1Id, yardForest)).error shouldBe null
                withClue("the land came from the graveyard") {
                    game.state.getBattlefield().contains(yardForest) shouldBe true
                }

                game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                withClue("Grizzly Bears was cast from the graveyard, paying its cost") {
                    game.state.getBattlefield().contains(bears) shouldBe true
                }

                withClue("the handler also refuses the instant and the opponent's card") {
                    game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))).error shouldNotBe null
                    game.execute(CastSpell(game.player1Id, giant)).error shouldNotBe null
                }
            }

            test("the emblem permission is its owner's, not the opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wrenn and Realmbreaker")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInGraveyard(2, "Forest")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wrenn = game.findPermanent("Wrenn and Realmbreaker")!!
                setLoyalty(game, wrenn, 7)
                activate(game, wrenn, index = 2)
                game.resolveStack()

                advanceToNextTurn(game)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("the opponent's own graveyard stays closed to them") {
                    game.getLegalActions(2).map { it.action }.none { action ->
                        (action is PlayLand && action.cardId in game.state.getGraveyard(game.player2Id)) ||
                            (action is CastSpell && action.cardId in game.state.getGraveyard(game.player2Id))
                    } shouldBe true
                }
            }
        }
    }

    private fun graveyardActionCards(game: TestGame): Set<EntityId> {
        val yard = game.state.getGraveyard(game.player1Id).toSet()
        return game.getLegalActions(1).mapNotNull { info ->
            when (val action = info.action) {
                is PlayLand -> action.cardId.takeIf { it in yard }
                is CastSpell -> action.cardId.takeIf { it in yard }
                else -> null
            }
        }.toSet()
    }

    private fun advanceToNextTurn(game: TestGame) {
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<EntityId> = emptyList()) {
        val ability = cardRegistry.getCard("Wrenn and Realmbreaker")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets.map { ChosenTarget.Permanent(it) }
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
