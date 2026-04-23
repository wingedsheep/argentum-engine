package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AjaniOutlandChaperoneScenarioTest : ScenarioTestBase() {

    init {
        context("Ajani, Outland Chaperone") {

            test("-2 ability enumerates tapped creature as valid target") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(2, "Gloom Ripper", tapped = true)
                    .withCardOnBattlefield(2, "Chomping Changeling")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!
                val gloomRipperId = game.findPermanent("Gloom Ripper")!!

                game.state = game.state.updateEntity(ajaniId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 8))
                }

                val enumerator = LegalActionEnumerator.create(cardRegistry)
                val legalActions = enumerator.enumerate(game.state, game.player1Id)

                val ajaniActions = legalActions.filter { it.action is ActivateAbility && (it.action as ActivateAbility).sourceId == ajaniId }

                withClue("Ajani should have all three loyalty abilities enumerated, got: ${ajaniActions.map { it.description }}") {
                    ajaniActions.size shouldBe 3
                }

                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val minusTwoAbility = cardDef.script.activatedAbilities[1]
                val minusTwoLegalAction = ajaniActions.find { (it.action as ActivateAbility).abilityId == minusTwoAbility.id }

                withClue("The -2 ability should be in the legal actions list") {
                    minusTwoLegalAction shouldNotBe null
                }

                withClue("Gloom Ripper should be in valid targets, got: ${minusTwoLegalAction?.validTargets}") {
                    minusTwoLegalAction?.validTargets?.contains(gloomRipperId) shouldBe true
                }
            }

            test("-2 deals 4 damage to tapped creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(2, "Gloom Ripper", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!
                val gloomRipperId = game.findPermanent("Gloom Ripper")!!

                game.state = game.state.updateEntity(ajaniId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 8))
                }

                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val minusTwoAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ajaniId,
                        abilityId = minusTwoAbility.id,
                        targets = listOf(ChosenTarget.Permanent(gloomRipperId))
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Gloom Ripper (4/4) should die to 4 damage") {
                    game.isOnBattlefield("Gloom Ripper") shouldBe false
                }
            }
        }
    }
}
