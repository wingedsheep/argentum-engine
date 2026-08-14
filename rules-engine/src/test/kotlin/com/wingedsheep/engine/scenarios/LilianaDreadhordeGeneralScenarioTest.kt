package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.LilianaDreadhordeGeneral
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Liliana, Dreadhorde General (WAR #97, reprinted in Foundations #176; {4}{B}{B}, Loyalty 6).
 *
 *   Whenever a creature you control dies, draw a card.
 *   +1: Create a 2/2 black Zombie creature token.
 *   −4: Each player sacrifices two creatures of their choice.
 *   −9: Each opponent chooses a permanent they control of each permanent type and sacrifices
 *       the rest.
 *
 * The −9 is the interesting one and drives most of these tests: it is a *sequence* of one pick per
 * permanent type, so a permanent with two types can be spared twice over, a type the opponent
 * controls one of asks nothing, and the controller's own board is never touched. The −4 pins the
 * APNAP choice order (CR 101.4) and the "can only choose one, so choose one" ruling, and both
 * sacrifice abilities double as coverage for the draw trigger.
 */
class LilianaDreadhordeGeneralScenarioTest : ScenarioTestBase() {

    private val plusOne = LilianaDreadhordeGeneral.activatedAbilities[0].id
    private val minusFour = LilianaDreadhordeGeneral.activatedAbilities[1].id
    private val minusNine = LilianaDreadhordeGeneral.activatedAbilities[2].id

    init {
        context("Liliana, Dreadhorde General") {

            test("+1 creates a 2/2 black Zombie token") {
                val game = boardWithLiliana(loyalty = 6)
                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = liliana, abilityId = plusOne)
                ).error shouldBe null
                game.resolveStack()

                val token = game.findPermanent("Zombie Token")
                withClue("the +1 created a token") { token shouldNotBe null }
                val projected = game.state.projectedState
                withClue("2/2 black Zombie") {
                    projected.getProjectedValues(token!!)?.power shouldBe 2
                    projected.getProjectedValues(token)?.toughness shouldBe 2
                    projected.isCreature(token) shouldBe true
                }
                loyalty(game, liliana) shouldBe 7
            }

            test("−9 asks nothing when each opponent controls exactly one of each permanent type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, Dreadhorde General")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // One permanent per type: artifact, creature, enchantment, land, planeswalker.
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sigil of the New Dawn")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardOnBattlefield(2, "Domri, Anarch of Bolas")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
                seedLoyalty(game, liliana, 9)
                seedLoyalty(game, game.findPermanent("Domri, Anarch of Bolas")!!, 3)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = liliana, abilityId = minusNine)
                ).error shouldBe null
                game.resolveStack()

                withClue("every category had exactly one candidate, so nothing was asked") {
                    game.getPendingDecision() shouldBe null
                }
                withClue("and nothing was sacrificed") {
                    game.graveyardSize(2) shouldBe 0
                    game.findPermanents("Sol Ring").size shouldBe 1
                    game.findPermanents("Sigil of the New Dawn").size shouldBe 1
                    game.findPermanent("Domri, Anarch of Bolas") shouldNotBe null
                }
                withClue("the ability only hits opponents — your own creature stays too") {
                    game.findPermanents("Grizzly Bears").map { controllerOf(game, it) }.toSet() shouldBe
                        setOf(game.player1Id, game.player2Id)
                }
                loyalty(game, liliana) shouldBe 0
            }

            test("−9 keeps the chosen permanent of each type and sacrifices everything else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, Dreadhorde General")
                    // Two creatures and two artifacts, so both of those types need a real choice;
                    // the single land is spared without asking.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Air")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withCardOnBattlefield(2, "Jayemdae Tome")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
                seedLoyalty(game, liliana, 9)
                val keptArtifact = game.findPermanent("Sol Ring")!!
                val keptCreature = game.findPermanent("Wall of Air")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = liliana, abilityId = minusNine)
                ).error shouldBe null
                game.resolveStack()

                // Artifact first (the CR 110.4 order Filters.PermanentTypes follows), then creature.
                val artifactChoice = game.getPendingDecision()
                withClue("paused for the opponent's artifact pick: $artifactChoice") {
                    (artifactChoice is SelectCardsDecision) shouldBe true
                    (artifactChoice as SelectCardsDecision).playerId shouldBe game.player2Id
                    artifactChoice.options.toSet() shouldBe
                        setOf(keptArtifact, game.findPermanent("Jayemdae Tome")!!)
                }
                withClue("nothing has been sacrificed while choices are still being made") {
                    game.graveyardSize(2) shouldBe 0
                }
                game.selectCards(listOf(keptArtifact)).error shouldBe null

                val creatureChoice = game.getPendingDecision()
                withClue("then the creature pick: $creatureChoice") {
                    (creatureChoice is SelectCardsDecision) shouldBe true
                    (creatureChoice as SelectCardsDecision).options.toSet() shouldBe
                        setOf(keptCreature, game.findPermanent("Grizzly Bears")!!)
                }
                game.selectCards(listOf(keptCreature)).error shouldBe null
                game.resolveStack()

                withClue("the unchosen artifact and creature were sacrificed; the land survived") {
                    graveyardNames(game, 2).sorted() shouldContainExactly
                        listOf("Grizzly Bears", "Jayemdae Tome")
                    game.findPermanent("Sol Ring") shouldNotBe null
                    game.findPermanent("Wall of Air") shouldNotBe null
                    game.findPermanent("Forest") shouldNotBe null
                }
            }

            test("−9 lets one artifact creature be both the artifact and the creature spared") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, Dreadhorde General")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
                seedLoyalty(game, liliana, 9)
                val thopter = game.findPermanent("Ornithopter")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = liliana, abilityId = minusNine)
                ).error shouldBe null
                game.resolveStack()

                // Artifact: Ornithopter or Sol Ring.
                game.selectCards(listOf(thopter)).error shouldBe null
                // Creature: Ornithopter or Grizzly Bears — the artifact creature is offered again,
                // so a single permanent can cover both types.
                val creatureChoice = game.getPendingDecision()
                withClue("Ornithopter is still a legal creature pick: $creatureChoice") {
                    (creatureChoice is SelectCardsDecision) shouldBe true
                    (creatureChoice as SelectCardsDecision).options.contains(thopter) shouldBe true
                }
                game.selectCards(listOf(thopter)).error shouldBe null
                game.resolveStack()

                withClue("only the artifact creature survived") {
                    game.findPermanent("Ornithopter") shouldNotBe null
                    graveyardNames(game, 2).sorted() shouldContainExactly
                        listOf("Grizzly Bears", "Sol Ring")
                }
            }

            test("−4: the active player chooses first (APNAP), then everyone sacrifices two") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, Dreadhorde General")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Wall of Air")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Air")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
                seedLoyalty(game, liliana, 6)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = liliana, abilityId = minusFour)
                ).error shouldBe null
                game.resolveStack()

                val first = game.getPendingDecision()
                withClue("the active player (Liliana's controller) chooses first: $first") {
                    (first is SelectCardsDecision) shouldBe true
                }
                first as SelectCardsDecision
                first.playerId shouldBe game.player1Id
                first.minSelections shouldBe 2
                game.selectCards(first.options.take(2)).error shouldBe null

                val second = game.getPendingDecision()
                withClue("then the nonactive player: $second") {
                    (second is SelectCardsDecision) shouldBe true
                }
                second as SelectCardsDecision
                second.playerId shouldBe game.player2Id
                game.selectCards(second.options.take(2)).error shouldBe null
                game.resolveStack()

                withClue("each player is down to one creature") {
                    game.graveyardSize(1) shouldBe 2
                    game.graveyardSize(2) shouldBe 2
                }
                withClue("two of your creatures died, so the draw trigger fired twice") {
                    game.handSize(1) shouldBe 2
                    game.librarySize(1) shouldBe 0
                }
                loyalty(game, liliana) shouldBe 2
            }

            test("−4: a player with a single creature sacrifices just that one, unprompted") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana, Dreadhorde General")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Air")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
                seedLoyalty(game, liliana, 6)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = liliana, abilityId = minusFour)
                ).error shouldBe null
                game.resolveStack()

                withClue("neither player had a choice to make") {
                    game.getPendingDecision() shouldBe null
                }
                withClue("both lone creatures went to the graveyard") {
                    graveyardNames(game, 1) shouldContainExactly listOf("Grizzly Bears")
                    graveyardNames(game, 2) shouldContainExactly listOf("Wall of Air")
                }
                withClue("your creature dying drew you a card") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }

    /** A minimal board: Liliana under player 1's control with [loyalty] loyalty counters. */
    private fun boardWithLiliana(loyalty: Int): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Liliana, Dreadhorde General")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        seedLoyalty(game, game.findPermanent("Liliana, Dreadhorde General")!!, loyalty)
        return game
    }

    /**
     * The scenario builder drops permanents straight onto the battlefield without running the
     * "enters with its starting loyalty" step, so planeswalkers need their counters seeded (a
     * planeswalker on 0 loyalty is put into its owner's graveyard by a state-based action).
     */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun controllerOf(game: TestGame, id: EntityId): EntityId? =
        game.state.getEntity(id)
            ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
            ?.playerId

    private fun graveyardNames(game: TestGame, playerNumber: Int): List<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getGraveyard(playerId).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }
    }
}
