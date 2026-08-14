package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Skyseer's Chariot (DFT #28) — {1}{W} Artifact — Vehicle, 3/3.
 *
 *   Flying
 *   As this Vehicle enters, choose a nonland card name.
 *   Activated abilities of sources with the chosen name cost {2} more to activate.
 *   Crew 2
 *
 * Two things worth proving: the as-enters choice draws from the **nonland** pool (lands must not be
 * offered — the Petrified Hamlet / Sorcerous Spyglass pools are the land-only and everything
 * variants), and the tax is a real {2}, not a lock. Prodigal Sorcerer's ping costs a bare `{T}`, so
 * with no mana available naming it turns the ability off entirely, and adding exactly two untapped
 * lands turns it back on.
 */
class SkyseersChariotScenarioTest : ScenarioTestBase() {

    private fun TestGame.castChariotToChoice(): ChooseOptionDecision {
        castSpell(1, "Skyseer's Chariot")
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
        val decision = getPendingDecision()
        withClue("Skyseer's Chariot must present an as-enters card-name choice on resolution") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseName(decision: ChooseOptionDecision, name: String) {
        withClue("The chosen name must be offered by the nonland pool") {
            decision.options shouldContain name
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    /**
     * Whether the ability is offered as *payable*. The enumerator still lists an unaffordable
     * ability (greyed out in the client), so the tax shows up as `affordable = false`, not as a
     * missing action.
     */
    private fun TestGame.canActivate(sourceId: EntityId): Boolean =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.isAffordable }

    private fun TestGame.abilityCostText(sourceId: EntityId): String =
        getLegalActions(1).first { (it.action as? ActivateAbility)?.sourceId == sourceId }.description

    init {
        context("Skyseer's Chariot — as-enters nonland card name") {

            test("offers nonland names and withholds land names") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skyseer's Chariot")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val decision = game.castChariotToChoice()

                withClue("the NONLAND pool offers nonland card names") {
                    decision.options shouldContain "Grizzly Bears"
                }
                withClue("lands are excluded — that is what separates this pool from ANY") {
                    decision.options shouldNotContain "Plains"
                    decision.options shouldNotContain "Forest"
                }

                game.chooseName(decision, "Grizzly Bears")

                val chariot = game.findPermanent("Skyseer's Chariot")!!
                withClue("the pick is stored durably under ChoiceSlot.CARD_NAME") {
                    game.state.getEntity(chariot)?.chosenCardName() shouldBe "Grizzly Bears"
                }
            }
        }

        context("Skyseer's Chariot — activated abilities of the named source cost {2} more") {

            test("naming a source taxes its {T} ability out of reach with no mana available") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skyseer's Chariot")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tim = game.findPermanent("Prodigal Sorcerer")!!
                withClue("before the Chariot lands, the bare {T} ping is activatable") {
                    game.canActivate(tim) shouldBe true
                }

                val decision = game.castChariotToChoice()
                game.chooseName(decision, "Prodigal Sorcerer")

                withClue("the offered cost is rebuilt from the taxed cost") {
                    game.abilityCostText(tim) shouldBe "{2}, {T}: Deal 1 damage to target"
                }
                withClue("both Plains paid for the Chariot — a {2}-taxed {T} ability is unaffordable") {
                    game.canActivate(tim) shouldBe false
                }
            }

            test("the tax is exactly {2} — two spare lands make the ability affordable again") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skyseer's Chariot")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tim = game.findPermanent("Prodigal Sorcerer")!!
                val decision = game.castChariotToChoice()
                game.chooseName(decision, "Prodigal Sorcerer")

                withClue("two Plains remain untapped — exactly the {2} the tax adds") {
                    game.canActivate(tim) shouldBe true
                }
            }

            test("an unnamed source keeps its ability at printed cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skyseer's Chariot")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tim = game.findPermanent("Prodigal Sorcerer")!!
                val decision = game.castChariotToChoice()
                game.chooseName(decision, "Grizzly Bears")

                withClue("the tax keys off the chosen name — a different name leaves Tim untouched") {
                    game.canActivate(tim) shouldBe true
                }
            }
        }
    }
}
