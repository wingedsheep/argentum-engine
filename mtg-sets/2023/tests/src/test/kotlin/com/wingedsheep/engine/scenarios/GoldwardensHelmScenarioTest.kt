package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.GoldwardensHelm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Goldwarden's Helm (ONE #13) — For Mirrodin! makes a 2/2 red Rebel and attaches to it;
 * the equipped creature gets +0/+1 (Rebel is 2/3). Equip {1}{W} moves the bonus elsewhere.
 */
class GoldwardensHelmScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        GoldwardensHelm.activatedAbilities.single { it.isEquipAbility }.id
    }

    init {
        context("Goldwarden's Helm") {
            test("For Mirrodin! creates a 2/2 red Rebel equipped as a 2/3") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Goldwarden's Helm")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Goldwarden's Helm")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.findPermanents("Rebel Token").size shouldBe 1
                val rebel = game.findPermanent("Rebel Token")!!
                val helm = game.findPermanent("Goldwarden's Helm")!!
                val rebelEntity = game.state.getEntity(rebel)!!
                rebelEntity.has<TokenComponent>() shouldBe true
                rebelEntity.get<CardComponent>()!!.colors shouldBe setOf(Color.RED)
                game.state.getEntity(helm)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                game.state.projectedState.getPower(rebel) shouldBe 2
                game.state.projectedState.getToughness(rebel) shouldBe 3
            }

            test("Equip {1}{W} moves the Helm; the Rebel drops back to 2/2") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Goldwarden's Helm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goldwarden's Helm").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val rebel = game.findPermanent("Rebel Token")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val helm = game.findPermanent("Goldwarden's Helm")!!

                val equip = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = helm,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(helm)?.get<AttachedToComponent>()?.targetId shouldBe bears
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 2
                projected.getToughness(bears) shouldBe 3
                projected.getPower(rebel) shouldBe 2
                projected.getToughness(rebel) shouldBe 2
            }

            test("Equip cannot be activated without {1}{W} available") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Goldwarden's Helm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val helm = game.findPermanent("Goldwarden's Helm")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val equip = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = helm,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                equip.error shouldNotBe null
                game.state.getEntity(helm)?.get<AttachedToComponent>() shouldBe null
            }
        }
    }
}
