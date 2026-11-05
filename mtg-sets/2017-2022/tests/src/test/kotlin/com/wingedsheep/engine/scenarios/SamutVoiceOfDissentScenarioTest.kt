package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.SamutVoiceOfDissent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Samut, Voice of Dissent (AKH #205) — {3}{R}{G} Legendary Creature — Human Warrior 3/4.
 *
 *   Flash
 *   Double strike, vigilance, haste
 *   Other creatures you control have haste.
 *   {W}, {T}: Untap another target creature.
 */
class SamutVoiceOfDissentScenarioTest : ScenarioTestBase() {

    private val abilityId = SamutVoiceOfDissent.activatedAbilities.single().id

    init {
        context("Samut, Voice of Dissent") {

            test("other creatures you control have haste; opponents' don't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Samut, Voice of Dissent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.HASTE) shouldBe true
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.HASTE) shouldBe false

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the summoning-sick Bears can attack thanks to Samut") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                }
            }

            test("{W}, {T}: untaps another target creature, including an opponent's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Samut, Voice of Dissent")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val samut = game.findPermanent("Samut, Voice of Dissent")!!
                val giant = game.findPermanent("Hill Giant")!!

                val result = game.execute(
                    ActivateAbility(game.player1Id, samut, abilityId, listOf(ChosenTarget.Permanent(giant)))
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe false
                game.state.getEntity(samut)!!.has<TappedComponent>() shouldBe true
            }

            test("can't target itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Samut, Voice of Dissent")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val samut = game.findPermanent("Samut, Voice of Dissent")!!

                game.execute(
                    ActivateAbility(game.player1Id, samut, abilityId, listOf(ChosenTarget.Permanent(samut)))
                ).error shouldNotBe null
            }
        }
    }
}
