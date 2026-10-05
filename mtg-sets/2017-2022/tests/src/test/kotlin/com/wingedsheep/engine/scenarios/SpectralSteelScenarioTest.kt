package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spectral Steel (KHM #30) — {1}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+2.
 *   {1}{W}, Exile this card from your graveyard: Return another target Aura or Equipment card
 *   from your graveyard to your hand.
 */
class SpectralSteelScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy { cardRegistry.getCard("Spectral Steel")!!.activatedAbilities.single().id }

    init {
        context("Spectral Steel") {

            test("enchanted creature gets +2/+2") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Spectral Steel", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }

            test("exiling it from the graveyard returns an Equipment card to hand") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInGraveyard(1, "Spectral Steel")
                    .withCardInGraveyard(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val steel = game.findCardsInGraveyard(1, "Spectral Steel").single()
                val bonesplitter = game.findCardsInGraveyard(1, "Bonesplitter").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = steel,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bonesplitter, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldBe null

                withClue("Spectral Steel is exiled as a cost") {
                    game.isInExile(1, "Spectral Steel") shouldBe true
                }
                game.resolveStack()

                game.isInHand(1, "Bonesplitter") shouldBe true
                game.isInGraveyard(1, "Bonesplitter") shouldBe false
            }

            test("returns another Aura card to hand") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInGraveyard(1, "Spectral Steel")
                    .withCardInGraveyard(1, "Holy Strength")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val steel = game.findCardsInGraveyard(1, "Spectral Steel").single()
                val holyStrength = game.findCardsInGraveyard(1, "Holy Strength").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = steel,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(holyStrength, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Holy Strength") shouldBe true
                game.isInExile(1, "Spectral Steel") shouldBe true
            }

            test("legal targets are only other Aura or Equipment cards in your own graveyard") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInGraveyard(1, "Spectral Steel")
                    .withCardInGraveyard(1, "Holy Strength")
                    .withCardInGraveyard(1, "Bonesplitter")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val steel = game.findCardsInGraveyard(1, "Spectral Steel").single()
                val holyStrength = game.findCardsInGraveyard(1, "Holy Strength").single()
                val bonesplitter = game.findCardsInGraveyard(1, "Bonesplitter").single()

                val activation = game.getLegalActions(1).firstOrNull {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == steel && a.abilityId == abilityId
                }
                activation.shouldNotBeNull()
                activation.validTargets.shouldNotBeNull() shouldContainExactlyInAnyOrder
                    listOf(holyStrength, bonesplitter)
            }

            test("cannot target itself") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInGraveyard(1, "Spectral Steel")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val steel = game.findCardsInGraveyard(1, "Spectral Steel").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = steel,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(steel, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldNotBe null

                game.isInGraveyard(1, "Spectral Steel") shouldBe true
            }
        }
    }
}
