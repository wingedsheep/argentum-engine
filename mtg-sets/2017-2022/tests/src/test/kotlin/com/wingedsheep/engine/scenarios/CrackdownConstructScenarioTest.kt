package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.CrackdownConstruct
import com.wingedsheep.mtg.sets.definitions.hob.cards.AlongTheCrookedWay
import com.wingedsheep.mtg.sets.definitions.inv.cards.Firescreamer
import com.wingedsheep.mtg.sets.definitions.kld.cards.CultivatorsCaravan
import com.wingedsheep.mtg.sets.definitions.kld.cards.Whirlermaker
import com.wingedsheep.mtg.sets.definitions.lea.cards.HillGiant
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Crackdown Construct (AER #148) — {4} Artifact Creature — Construct 2/2.
 *
 * "Whenever you activate an ability of an artifact or creature that isn't a mana ability, this
 * creature gets +1/+1 until end of turn."
 *
 * Proves the source filter's both halves (an artifact's ability, a creature's ability), that crew
 * — an activated ability of the Vehicle — counts, and the two exclusions: a creature's mana
 * ability, and a permanent that is neither artifact nor creature.
 */
class CrackdownConstructScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(CrackdownConstruct)
        cardRegistry.register(Whirlermaker)
        cardRegistry.register(CultivatorsCaravan)
        cardRegistry.register(Firescreamer)
        cardRegistry.register(AlongTheCrookedWay)
        cardRegistry.register(HillGiant)

        context("Crackdown Construct") {

            test("activating an artifact's non-mana ability gives it +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crackdown Construct", summoningSickness = false)
                    .withCardOnBattlefield(1, "Whirlermaker", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val construct = game.findPermanent("Crackdown Construct")!!
                val maker = game.findPermanent("Whirlermaker")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = maker,
                        abilityId = Whirlermaker.activatedAbilities.single().id
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("one artifact activation is one +1/+1") {
                    game.state.projectedState.getPower(construct) shouldBe 3
                    game.state.projectedState.getToughness(construct) shouldBe 3
                }
            }

            test("activating a creature's non-mana ability triggers it each time") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crackdown Construct", summoningSickness = false)
                    .withCardOnBattlefield(1, "Firescreamer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val construct = game.findPermanent("Crackdown Construct")!!
                val screamer = game.findPermanent("Firescreamer")!!
                val pump = Firescreamer.activatedAbilities.single().id
                repeat(2) {
                    game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = screamer, abilityId = pump)
                    ).error shouldBe null
                    if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }

                withClue("two creature-ability activations stack two +1/+1 pumps") {
                    game.state.projectedState.getPower(construct) shouldBe 4
                    game.state.projectedState.getToughness(construct) shouldBe 4
                }
            }

            test("crewing a Vehicle triggers it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crackdown Construct", summoningSickness = false)
                    .withCardOnBattlefield(1, "Cultivator's Caravan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val construct = game.findPermanent("Crackdown Construct")!!
                val caravan = game.findPermanent("Cultivator's Caravan")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(CrewVehicle(game.player1Id, caravan, listOf(giant))).error shouldBe null
                game.resolveStack()

                withClue("crew is an activated ability of the Vehicle, an artifact") {
                    game.state.projectedState.getPower(construct) shouldBe 3
                    game.state.projectedState.getToughness(construct) shouldBe 3
                }
            }

            test("a creature's mana ability does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crackdown Construct", summoningSickness = false)
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val construct = game.findPermanent("Crackdown Construct")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = elves,
                        abilityId = TestCards.LlanowarElves.activatedAbilities.single().id
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("\"that isn't a mana ability\" excludes the Elves' {T}: Add {G}") {
                    game.state.projectedState.getPower(construct) shouldBe 2
                    game.state.projectedState.getToughness(construct) shouldBe 2
                }
            }

            test("an ability of a permanent that is neither artifact nor creature does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crackdown Construct", summoningSickness = false)
                    .withCardOnBattlefield(1, "Along the Crooked Way")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val construct = game.findPermanent("Crackdown Construct")!!
                val enchantment = game.findPermanent("Along the Crooked Way")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = enchantment,
                        abilityId = AlongTheCrookedWay.activatedAbilities.single().id
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("an Enchantment's ability is outside \"an artifact or creature\"") {
                    game.state.projectedState.getPower(construct) shouldBe 2
                    game.state.projectedState.getToughness(construct) shouldBe 2
                }
            }
        }
    }
}
