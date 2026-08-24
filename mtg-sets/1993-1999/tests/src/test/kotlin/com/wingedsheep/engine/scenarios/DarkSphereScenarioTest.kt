package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Dark Sphere (DRK #100).
 *
 * {0} Artifact
 * "{T}, Sacrifice this artifact: The next time a source of your choice would deal damage to you
 *  this turn, prevent half that damage, rounded down."
 *
 * The Circle of Protection family's single-instance chosen-source shield, halved. Two behaviours
 * distinguish it from a Circle and from a damage-reduction static: only half the instance is
 * prevented (rounded down, so the rest is still dealt), and the shield is consumed even when it
 * prevents nothing.
 */
class DarkSphereScenarioTest : ScenarioTestBase() {

    init {
        fun sphereAbilityId() =
            cardRegistry.getCard("Dark Sphere")!!.script.activatedAbilities[0].id

        context("Dark Sphere") {

            test("prevents half the chosen source's combat damage, rounded down") {
                // Shivan Dragon (5/5) attacks. Half of 5, rounded down, is 2 prevented — 3 is dealt.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Dark Sphere")
                    .withCardOnBattlefield(2, "Shivan Dragon")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Shivan Dragon" to 1)).error shouldBe null
                game.passPriority() // P2 passes; P1 gets priority

                val sphere = game.findPermanent("Dark Sphere")!!
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sphere,
                        abilityId = sphereAbilityId()
                    )
                )
                withClue("Activation succeeds: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Sacrificed as a cost — the Sphere is gone from the battlefield") {
                    game.findPermanent("Dark Sphere").shouldBeNull()
                }

                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val dragon = game.findPermanent("Shivan Dragon")!!
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(dragon)))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("5 damage, half rounded down (2) prevented → 20 - 3 = 17") {
                    game.getLifeTotal(1) shouldBe 17
                }
            }

            test("the shield shows up as a badge on the player it protects") {
                // The shield lives on the player, not on a card — and the Sphere sacrifices itself
                // paying for it, so without a player badge there is nothing on screen to say the
                // shield is there at all.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Dark Sphere")
                    .withCardOnBattlefield(2, "Shivan Dragon")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("nothing to show before it is activated") {
                    game.getClientState(1).players
                        .single { it.playerId == game.player1Id }
                        .activeEffects
                        .none { it.effectId.startsWith("prevent_next_damage_instance_from_source") } shouldBe true
                }

                val sphere = game.findPermanent("Dark Sphere")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sphere,
                        abilityId = sphereAbilityId()
                    )
                ).error shouldBe null
                game.resolveStack()
                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(game.findPermanent("Shivan Dragon")!!)))

                val badge = game.getClientState(1).players
                    .single { it.playerId == game.player1Id }
                    .activeEffects
                    .singleOrNull { it.effectId.startsWith("prevent_next_damage_instance_from_source") }
                withClue("the protected player carries a badge naming the chosen source") {
                    badge.shouldNotBeNull()
                    badge.name shouldBe "Halve from Shivan Dragon"
                }
                withClue("and it says the halving, which is what makes this Dark Sphere and not a Circle") {
                    badge!!.description shouldBe
                        "The next time Shivan Dragon would deal damage to you this turn, " +
                        "prevent half that damage, rounded down"
                }
            }

            test("a 1-damage instance halves to nothing prevented and still spends the shield") {
                // Half of 1 rounded down is 0, so the ping lands in full — and because this is a
                // *next instance* shield, it is consumed anyway: the second ping also lands.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Triskelion")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardOnBattlefield(1, "Dark Sphere")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Triskelion").error shouldBe null
                game.resolveStack()
                val trisk = game.findPermanent("Triskelion")!!

                val sphere = game.findPermanent("Dark Sphere")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sphere,
                        abilityId = sphereAbilityId()
                    )
                ).error shouldBe null
                game.resolveStack()
                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(trisk)))

                val triskAbility = cardRegistry.getCard("Triskelion")!!.script.activatedAbilities[0]
                fun pingSelf() {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = trisk,
                            abilityId = triskAbility.id,
                            targets = listOf(entityIdToChosenTarget(game.state, game.player1Id))
                        )
                    ).error shouldBe null
                    game.resolveStack()
                }

                pingSelf()
                withClue("Half of 1 rounded down is 0 — nothing is prevented") {
                    game.getLifeTotal(1) shouldBe 19
                }

                pingSelf()
                withClue("The shield was spent on that instance regardless — the second ping lands too") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
