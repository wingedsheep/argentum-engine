package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.trk.cards.GwyndalaTheProgeny
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gwyndala, the Progeny (TRK #61) — {1}{U} Legendary Creature — Vau-N'Akat Advisor 1/1.
 * "When Gwyndala enters, create a colorless Equipment artifact token named Neuroflux Heirloom with
 * 'Equipped creature gets +1/+1' and equip {2}. Attach it to Gwyndala.
 * {2}: Each Equipment token you control becomes a copy of target Equipment you control until end of turn."
 */
class GwyndalaTheProgenyScenarioTest : ScenarioTestBase() {

    init {
        context("Gwyndala, the Progeny") {

            test("enters with a Neuroflux Heirloom attached to her") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gwyndala, the Progeny")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gwyndala, the Progeny").error shouldBe null
                game.resolveStack()

                val gwyndala = game.findPermanent("Gwyndala, the Progeny")!!
                val heirloom = game.findPermanent("Neuroflux Heirloom")
                withClue("The ETB creates the Neuroflux Heirloom token") { heirloom shouldNotBe null }
                val card = game.state.getEntity(heirloom!!)!!.get<CardComponent>()!!
                withClue("The Heirloom is a colorless Equipment artifact") {
                    card.colors.isEmpty() shouldBe true
                    card.typeLine.subtypes.map { it.value }.toSet() shouldBe setOf("Equipment")
                }
                withClue("It is attached to Gwyndala, making her 2/2") {
                    game.state.getEntity(heirloom)?.get<AttachedToComponent>()?.targetId shouldBe gwyndala
                    game.state.projectedState.getPower(gwyndala) shouldBe 2
                    game.state.projectedState.getToughness(gwyndala) shouldBe 2
                }
            }

            test("{2}: Equipment tokens become copies of the target Equipment until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gwyndala, the Progeny")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gwyndala, the Progeny").error shouldBe null
                game.resolveStack()
                val gwyndala = game.findPermanent("Gwyndala, the Progeny")!!
                val heirloom = game.findPermanent("Neuroflux Heirloom")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                val copyAbility = GwyndalaTheProgeny.activatedAbilities.single()
                val activated = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gwyndala,
                        abilityId = copyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(bonesplitter))
                    )
                )
                withClue("{2} should be payable: ${activated.error}") { activated.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("The Heirloom token is now a Bonesplitter, still attached to Gwyndala: 1+2 / 1+0") {
                    game.state.getEntity(heirloom)!!.get<CardComponent>()!!.name shouldBe "Bonesplitter"
                    game.state.getEntity(heirloom)?.get<AttachedToComponent>()?.targetId shouldBe gwyndala
                    game.state.projectedState.getPower(gwyndala) shouldBe 3
                    game.state.projectedState.getToughness(gwyndala) shouldBe 1
                }
                withClue("The nontoken Bonesplitter is not an Equipment token, so it is unchanged") {
                    game.state.getEntity(bonesplitter)!!.get<CardComponent>()!!.name shouldBe "Bonesplitter"
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("Until end of turn: the Heirloom is itself again on the next turn") {
                    game.state.getEntity(heirloom)!!.get<CardComponent>()!!.name shouldBe "Neuroflux Heirloom"
                    game.state.projectedState.getPower(gwyndala) shouldBe 2
                    game.state.projectedState.getToughness(gwyndala) shouldBe 2
                }
            }
        }
    }
}
