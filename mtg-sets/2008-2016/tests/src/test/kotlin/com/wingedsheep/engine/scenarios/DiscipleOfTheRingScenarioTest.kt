package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.DiscipleOfTheRing
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Disciple of the Ring (ORI #53) — {3}{U}{U} Creature — Human Wizard, 3/4.
 *
 *   {1}, Exile an instant or sorcery card from your graveyard: Choose one —
 *   • Counter target noncreature spell unless its controller pays {2}.
 *   • This creature gets +1/+1 until end of turn.
 *   • Tap target creature.
 *   • Untap target creature.
 */
class DiscipleOfTheRingScenarioTest : ScenarioTestBase() {

    private val abilityId = DiscipleOfTheRing.activatedAbilities.single().id
    private val counterMode = 0
    private val pumpMode = 1
    private val tapMode = 2

    private fun TestGame.activate(
        mode: Int,
        exiled: List<EntityId>,
        target: EntityId? = null,
        spellTarget: EntityId? = null,
    ): ExecutionResult {
        val targets = listOfNotNull(target).map { ChosenTarget.Permanent(it) } +
            listOfNotNull(spellTarget).map { ChosenTarget.Spell(it) }
        return execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Disciple of the Ring")!!,
                abilityId = abilityId,
                targets = targets,
                costPayment = AdditionalCostPayment(exiledCards = exiled),
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(targets),
            )
        )
    }

    private fun board(graveyardCard: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Disciple of the Ring")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInGraveyard(1, graveyardCard)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Disciple of the Ring") {

            test("{1}, exile an instant: the +1/+1 mode pumps the Disciple until end of turn") {
                val game = board("Lightning Bolt")
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

                val result = game.activate(pumpMode, listOf(bolt))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val disciple = game.findPermanent("Disciple of the Ring")!!
                game.state.projectedState.getPower(disciple) shouldBe 4
                game.state.projectedState.getToughness(disciple) shouldBe 5
                withClue("Lightning Bolt was exiled to pay the cost") {
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                }
            }

            test("the tap mode taps target creature") {
                val game = board("Lightning Bolt")
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                val giant = game.findPermanent("Hill Giant")!!

                val result = game.activate(tapMode, listOf(bolt), target = giant)
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant is tapped") {
                    game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("the counter mode counters a noncreature spell whose controller can't pay {2}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Disciple of the Ring")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority().error shouldBe null
                val theirBolt = game.state.stack.single()
                val myBolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

                val result = game.activate(counterMode, listOf(myBolt), spellTarget = theirBolt)
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the opponent's Bolt was countered with no mana left to pay {2}") {
                    game.state.stack shouldBe emptyList()
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("the counter mode can't target a creature spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Disciple of the Ring")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority().error shouldBe null
                val bears = game.state.stack.single()
                val myBolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

                val result = game.activate(counterMode, listOf(myBolt), spellTarget = bears)
                withClue("Grizzly Bears is a creature spell") { result.error shouldNotBe null }
            }

            test("can't be activated without an instant or sorcery card in your graveyard") {
                val game = board("Grizzly Bears")
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                val result = game.activate(pumpMode, listOf(bears))
                withClue("a creature card can't pay the exile cost") { result.error shouldNotBe null }
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.state.projectedState.getPower(game.findPermanent("Disciple of the Ring")!!) shouldBe 3
            }
        }
    }
}
