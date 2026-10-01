package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bladegraft Aspirant — "Menace. Equipment spells you cast cost {1} less to cast. Activated
 * abilities of Equipment you control that target this creature cost {1} less to activate."
 *
 * Barrow-Blade ({1}, Equip {1}) is the Equipment: free to cast, and its equip is free when it
 * targets the Aspirant but full price onto any other creature.
 */
class BladegraftAspirantScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Barrow-Blade").activatedAbilities.single { it.isEquipAbility }.id
    }

    private fun TestGame.equip(blade: com.wingedsheep.sdk.model.EntityId, target: com.wingedsheep.sdk.model.EntityId) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = blade,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).also { if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    init {
        test("has menace") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bladegraft Aspirant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.state.projectedState.hasKeyword(game.findPermanent("Bladegraft Aspirant")!!, Keyword.MENACE) shouldBe true
        }

        test("Equipment spells cost {1} less: Barrow-Blade is cast with no mana") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bladegraft Aspirant")
                .withCardInHand(1, "Barrow-Blade")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Barrow-Blade").error shouldBe null
            game.resolveStack()
            game.findPermanent("Barrow-Blade") shouldNotBe null
        }

        test("equip targeting the Aspirant costs {1} less — free with no lands") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bladegraft Aspirant")
                .withCardOnBattlefield(1, "Barrow-Blade")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val aspirant = game.findPermanent("Bladegraft Aspirant")!!
            val blade = game.findPermanent("Barrow-Blade")!!
            game.equip(blade, aspirant).error shouldBe null
            game.resolveStack()

            game.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe aspirant
            game.state.projectedState.getPower(aspirant) shouldBe 3
            game.state.projectedState.getToughness(aspirant) shouldBe 4
        }

        test("equip targeting another creature pays the full {1}") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bladegraft Aspirant")
                .withCardOnBattlefield(1, "Barrow-Blade")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val blade = game.findPermanent("Barrow-Blade")!!
            val giant = game.findPermanent("Hill Giant")!!
            withClue("with no lands, the unreduced equip {1} can't be paid") {
                game.equip(blade, giant).error shouldNotBe null
            }
        }
    }
}
