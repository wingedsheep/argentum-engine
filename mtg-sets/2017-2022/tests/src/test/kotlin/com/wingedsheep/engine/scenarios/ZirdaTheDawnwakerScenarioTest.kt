package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

/**
 * Zirda, the Dawnwaker (IKO #233):
 *
 *   Abilities you activate that aren't mana abilities cost {2} less to activate. This effect can't
 *   reduce the mana in that cost to less than one mana.
 *
 * The rulings this pins: generic-only and floored at one mana ({2} → {1}, {1}{R} → {R}, {1} stays);
 * every zone — cycling from hand is named in the ruling; mana abilities are excluded; and "you
 * activate" leaves an opponent's activations at full price.
 */
class ZirdaTheDawnwakerScenarioTest : ScenarioTestBase() {

    private fun TestGame.abilityOf(playerNumber: Int, sourceId: EntityId): LegalActionInfo =
        getLegalActions(playerNumber).first {
            (it.action as? ActivateAbility)?.sourceId == sourceId && !it.isManaAbility
        }

    init {
        context("Zirda — battlefield abilities") {

            test("Basalt Monolith's {3} untap costs {1}, and one land pays it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardOnBattlefield(1, "Basalt Monolith", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monolith = game.findPermanent("Basalt Monolith")!!
                val untap = game.abilityOf(1, monolith)
                withClue("{3} reduced by {2}, offered and affordable off one Plains") {
                    untap.description shouldStartWith "{1}:"
                    untap.isAffordable shouldBe true
                }

                game.execute(untap.action).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the untap resolved for {1}") {
                    game.state.getEntity(monolith)?.has<TappedComponent>() shouldBe false
                }
            }

            test("a mana ability keeps its printed cost — Cabal Coffers still needs {2}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardOnBattlefield(1, "Cabal Coffers")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val coffers = game.findPermanent("Cabal Coffers")!!
                val mana = game.getLegalActions(1).first {
                    (it.action as? ActivateAbility)?.sourceId == coffers
                }
                withClue("{2}, {T}: Add {B}… is a mana ability (CR 605.1a); one Swamp can't pay {2}") {
                    mana.isAffordable shouldBe false
                }
            }

            test("an opponent's activations are not reduced — the static says 'you activate'") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardOnBattlefield(2, "Basalt Monolith", tapped = true)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monolith = game.findPermanent("Basalt Monolith")!!
                val untap = game.abilityOf(2, monolith)
                untap.description shouldStartWith "{3}:"
                untap.isAffordable shouldBe false
            }

            test("Zirda's own {1}, {T} ability is already at one mana and stays {1}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zirda = game.findPermanent("Zirda, the Dawnwaker")!!
                val ability = game.abilityOf(1, zirda)
                ability.description shouldStartWith "{1}, {T}:"
                withClue("no land to pay {1} — the floor keeps a mana cost of one") {
                    ability.isAffordable shouldBe false
                }
            }
        }

        context("Zirda — abilities activated from hand") {

            test("cycling {2} costs {1} and resolves off one land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardInHand(1, "Hush")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.getLegalActions(1).first { it.action is CycleCard }
                withClue("the offered cycle is priced at the reduced {1}") {
                    cycle.manaCostString shouldBe "{1}"
                    cycle.isAffordable shouldBe true
                }

                game.cycleCard(1, "Hush").error shouldBe null
                game.isInGraveyard(1, "Hush") shouldBe true
                game.handSize(1) shouldBe 1
            }

            test("colored pips are untouched — cycling {1}{U} costs {U}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardInHand(1, "Choking Tethers")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.getLegalActions(1).first { it.action is CycleCard }
                cycle.manaCostString shouldBe "{U}"
                game.cycleCard(1, "Choking Tethers").error shouldBe null
                game.isInGraveyard(1, "Choking Tethers") shouldBe true
            }

            test("typecycling is reduced too — Swampcycling {2} costs {1}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardInHand(1, "Twisted Abomination")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val typecycle = game.getLegalActions(1).first { it.action is TypecycleCard }
                typecycle.manaCostString shouldBe "{1}"
                typecycle.isAffordable shouldBe true
                game.typecycleCard(1, "Twisted Abomination").error shouldBe null
                game.isInGraveyard(1, "Twisted Abomination") shouldBe true
            }

            test("a graveyard ability is reduced — Vineweft's {4}{G} return costs {2}{G}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zirda, the Dawnwaker")
                    .withCardInGraveyard(1, "Vineweft")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vineweft = game.findCardsInGraveyard(1, "Vineweft").single()
                val ret = game.abilityOf(1, vineweft)
                withClue("three Forests pay the reduced {2}{G}; the printed {4}{G} would need five") {
                    ret.description shouldStartWith "{2}{G}:"
                    ret.isAffordable shouldBe true
                }
                game.execute(ret.action).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.findCardsInHand(1, "Vineweft").size shouldBe 1
            }

            test("without Zirda the same cycle costs the printed {2} and one land can't pay it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hush")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.getLegalActions(1).first { it.action is CycleCard }
                cycle.manaCostString shouldBe "{2}"
                cycle.isAffordable shouldBe false
            }
        }
    }
}
