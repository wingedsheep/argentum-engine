package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dft.cards.OviyaAutomechArtisan
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Oviya, Automech Artisan (DFT #173) — {3}{G} Legendary Creature — Human
 * Artificer (1/2).
 *
 * "Each creature that's attacking one of your opponents has trample.
 *  {G}, {T}: You may put a creature or Vehicle card from your hand onto the battlefield. If you put
 *  an artifact onto the battlefield this way, put two +1/+1 counters on it."
 *
 * The trample grant is what motivates `StatePredicate.IsAttackingAnOpponent`: it follows the
 * *defender*, so an attacker aimed at an opponent's planeswalker is attacking but is not attacking
 * the opponent, and gets nothing. These tests pin both halves of that distinction plus the
 * conditional counters on the activated ability.
 */
class OviyaAutomechArtisanScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        val putAbilityId = OviyaAutomechArtisan.activatedAbilities.first().id

        context("Each creature that's attacking one of your opponents has trample") {

            test("a creature attacking the opponent gains trample; it has none before combat") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Oviya, Automech Artisan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("no trample before the Bear is attacking anything") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                }

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("attacking P1's opponent ⇒ trample") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("Oviya's controller is not an opponent of themselves — a creature attacking them gets nothing") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Oviya, Automech Artisan", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the opponent's attacker is aimed at Oviya's controller, not at an opponent of theirs") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("attacking an opponent's planeswalker is not attacking the opponent — no trample") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Oviya, Automech Artisan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Chandra, Flameshaper")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Chandra, Flameshaper")
                ).error shouldBe null

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the defender is a planeswalker, so the attacker isn't attacking a player") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                }
            }
        }

        context("{G}, {T}: put a creature or Vehicle card from your hand onto the battlefield") {

            test("an artifact put this way enters with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Oviya, Automech Artisan", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oviya = game.findPermanent("Oviya, Automech Artisan")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = oviya, abilityId = putAbilityId)
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val thopterInHand = game.findCardsInHand(1, "Ornithopter")
                if (thopterInHand.isNotEmpty()) game.selectCards(thopterInHand)
                game.resolveStack()

                val thopter = game.findPermanent("Ornithopter")
                withClue("the artifact creature was put onto the battlefield") { thopter shouldNotBe null }
                withClue("an artifact put this way gets two +1/+1 counters") {
                    plusOneCounters(game, thopter!!) shouldBe 2
                }
            }

            test("a nonartifact creature put this way gets no counters") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Oviya, Automech Artisan", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oviya = game.findPermanent("Oviya, Automech Artisan")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = oviya, abilityId = putAbilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val bearsInHand = game.findCardsInHand(1, "Grizzly Bears")
                if (bearsInHand.isNotEmpty()) game.selectCards(bearsInHand)
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")
                withClue("the creature was put onto the battlefield") { bears shouldNotBe null }
                withClue("it isn't an artifact, so the counters clause does nothing") {
                    plusOneCounters(game, bears!!) shouldBe 0
                }
            }

            test("declining the optional put leaves the card in hand and the battlefield unchanged") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Oviya, Automech Artisan", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oviya = game.findPermanent("Oviya, Automech Artisan")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = oviya, abilityId = putAbilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                if (game.hasPendingDecision()) game.skipSelection()
                game.resolveStack()

                withClue("\"you may\" — declining is legal and the card stays in hand") {
                    game.isInHand(1, "Ornithopter") shouldBe true
                }
                withClue("nothing entered the battlefield") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }
            }
        }
    }
}
