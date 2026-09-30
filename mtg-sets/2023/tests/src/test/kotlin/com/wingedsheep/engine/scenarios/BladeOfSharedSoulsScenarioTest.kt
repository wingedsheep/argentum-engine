package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.BladeOfSharedSouls
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Blade of Shared Souls (ONE #42) — whenever it becomes attached to a creature, for as long as it
 * remains attached, you may have that creature become a copy of another target creature you
 * control. The copy reverts the moment the Equipment moves to another creature.
 */
class BladeOfSharedSoulsScenarioTest : ScenarioTestBase() {

    private val equipAbilityId = BladeOfSharedSouls.activatedAbilities.single { it.isEquipAbility }.id

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    /** Cast the Blade and resolve For Mirrodin! and the attach trigger, targeting the Hill Giant. */
    private fun TestGame.castBladeAndCopy(giant: EntityId, accept: Boolean) {
        val cast = castSpell(1, "Blade of Shared Souls")
        withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
        // The optional attach trigger asks "you may" first, then for its target.
        withClue("The attach trigger asks whether to copy") { hasPendingDecision() shouldBe true }
        answerYesNo(accept)
        if (accept) selectTargets(listOf(giant))
        resolveStack()
    }

    init {
        context("Blade of Shared Souls") {
            test("the Rebel becomes a copy of another creature you control, and reverts when the Blade moves") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Blade of Shared Souls")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castBladeAndCopy(giant, accept = true)

                val blade = game.findPermanent("Blade of Shared Souls")!!
                val rebel = game.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId!!
                withClue("The equipped Rebel token is now a copy of Hill Giant (3/3)") {
                    game.nameOf(rebel) shouldBe "Hill Giant"
                    game.state.projectedState.getPower(rebel) shouldBe 3
                    game.state.projectedState.getToughness(rebel) shouldBe 3
                }

                // Equip {2} onto the original Hill Giant: the Rebel's copy ends at once.
                val equip = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = blade,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                // The new attach trigger (Giant may copy the Rebel) — decline it.
                withClue("The new attach trigger offers the Giant a copy of the Rebel") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Blade moved to the Hill Giant") {
                    game.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe giant
                }
                withClue("The Rebel reverted to a 2/2 Rebel token") {
                    game.nameOf(rebel) shouldBe "Rebel Token"
                    game.state.projectedState.getPower(rebel) shouldBe 2
                    game.state.projectedState.getToughness(rebel) shouldBe 2
                }
                withClue("The Giant kept its own identity (copy declined)") {
                    game.nameOf(giant) shouldBe "Hill Giant"
                }
            }

            test("declining leaves the Rebel as itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Blade of Shared Souls")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castBladeAndCopy(giant, accept = false)

                val blade = game.findPermanent("Blade of Shared Souls")!!
                val rebel = game.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId!!
                game.nameOf(rebel) shouldBe "Rebel Token"
                game.findPermanents("Hill Giant").size shouldBe 1
            }
        }
    }
}
