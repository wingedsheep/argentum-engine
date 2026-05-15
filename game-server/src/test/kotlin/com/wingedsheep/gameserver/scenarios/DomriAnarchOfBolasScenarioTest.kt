package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DomriAnarchOfBolasScenarioTest : ScenarioTestBase() {

    private fun TestGame.addLoyalty(cardName: String, loyalty: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(CounterType.LOYALTY, loyalty))
        }
    }

    private fun TestGame.activateLoyaltyAbility(
        playerNumber: Int,
        cardName: String,
        abilityIndex: Int,
        targetIds: List<EntityId> = emptyList()
    ) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val permanentId = findPermanent(cardName)!!
        val cardDef = cardRegistry.getCard(cardName)!!
        val ability = cardDef.script.activatedAbilities[abilityIndex]

        val targets = targetIds.map { ChosenTarget.Permanent(it) }

        val result = execute(
            ActivateAbility(
                playerId = playerId,
                sourceId = permanentId,
                abilityId = ability.id,
                targets = targets
            )
        )
        withClue("Loyalty ability activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    /**
     * Directly grant "creature spells you cast this turn can't be countered" to a player.
     * Bypasses the +1 activation (which would otherwise pause for the R/G color choice),
     * letting us focus on the protection logic itself.
     */
    private fun TestGame.grantCreatureSpellsUncounterable(playerNumber: Int) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        state = state.updateEntity(playerId) { c ->
            c.with(
                SpellsCantBeCounteredComponent(
                    filters = listOf(GameObjectFilter.Creature),
                    removeOn = PlayerEffectRemoval.EndOfTurn
                )
            )
        }
    }

    init {
        context("Static: Creatures you control get +1/+0") {
            test("other creatures you control get +1/+0 while Domri is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Domri, Anarch of Bolas")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 vanilla
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val projected = game.state.projectedState

                withClue("Glory Seeker should be 3/2 (base 2/2 + Domri anthem)") {
                    projected.getPower(glorySeekerId) shouldBe 3
                    projected.getToughness(glorySeekerId) shouldBe 2
                }
            }
        }

        context("+1: Creature spells you cast this turn can't be countered") {
            test("Cancel fails to counter your creature spell after +1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Domri, Anarch of Bolas")
                    .withCardInHand(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(2, "Cancel")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Domri, Anarch of Bolas", 3)

                // Simulate Domri's +1 having already resolved this turn.
                game.grantCreatureSpellsUncounterable(1)

                // P1 casts Glory Seeker; P2 tries to Cancel it.
                game.castSpell(1, "Glory Seeker")
                game.passPriority() // P1 passes to P2
                game.castSpellTargetingStackSpell(2, "Cancel", "Glory Seeker")

                // Drain the stack: Cancel resolves but can't counter; Glory Seeker resolves.
                game.resolveStack()

                withClue("Glory Seeker should still hit the battlefield (protected by Domri's grant)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Cancel should have resolved to the graveyard") {
                    game.isInGraveyard(2, "Cancel") shouldBe true
                }
            }

            test("non-creature spell is still counterable while grant is active") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Domri, Anarch of Bolas")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInHand(2, "Cancel")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.grantCreatureSpellsUncounterable(1)

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Shock", glorySeekerId)
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Cancel", "Shock")
                game.resolveStack()

                withClue("Shock should be countered (Domri's grant only covers creature spells)") {
                    game.isInGraveyard(1, "Shock") shouldBe true
                }
                withClue("Glory Seeker should still be on the battlefield (Shock fizzled)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }
        }

        context("-2: Target creature you control fights target creature you don't control") {
            test("both creatures deal damage equal to their power to each other") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Domri, Anarch of Bolas")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 → 3/2 under Domri
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Domri, Anarch of Bolas", 3)

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val grizzlyId = game.findPermanent("Grizzly Bears")!!

                // -2 is the second loyalty ability (index 1 — static abilities aren't activated).
                game.activateLoyaltyAbility(
                    1, "Domri, Anarch of Bolas", 1,
                    listOf(glorySeekerId, grizzlyId)
                )
                game.resolveStack()

                withClue("Grizzly Bears took 3 damage from Glory Seeker (anthem) and dies") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("Glory Seeker took 2 damage equal to its toughness and dies too") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInGraveyard(1, "Glory Seeker") shouldBe true
                }
                withClue("Domri loses two loyalty (3 → 1)") {
                    val domriId = game.findPermanent("Domri, Anarch of Bolas")!!
                    val counters = game.state.getEntity(domriId)?.get<CountersComponent>()
                    counters?.getCount(CounterType.LOYALTY) shouldBe 1
                }
            }
        }
    }
}
