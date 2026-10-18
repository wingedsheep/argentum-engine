package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Spirit Shackle — {B}{B} Enchantment — Aura. Enchant creature.
 * Whenever enchanted creature becomes tapped, put a -0/-2 counter on it.
 *
 * Pinned: tapping by any means triggers it (attacking, a {T} ability); a vigilant attacker never
 * becomes tapped, so it doesn't; the -0/-2 counter lowers toughness only, and still lands on the
 * tapped creature — and stays — when the Aura is destroyed with the trigger on the stack.
 */
class SpiritShackleScenarioTest : ScenarioTestBase() {

    init {
        context("Spirit Shackle") {
            test("attacking taps the creature and puts a -0/-2 counter on it") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardAttachedTo(2, "Spirit Shackle", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                game.state.getEntity(giant)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ZERO_MINUS_TWO) shouldBe 1
                game.state.projectedState.getPower(giant) shouldBe 3
                game.state.projectedState.getToughness(giant) shouldBe 1
            }

            test("destroying the Aura in response doesn't stop the counter, and the counter stays") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardAttachedTo(2, "Spirit Shackle", "Hill Giant")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.castSpell(1, "Disenchant", game.findPermanent("Spirit Shackle")!!).error shouldBe null
                game.resolveStack()

                withClue("Disenchant resolved first") { game.isInGraveyard(2, "Spirit Shackle") shouldBe true }
                val giant = game.findPermanent("Hill Giant")!!
                game.state.getEntity(giant)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ZERO_MINUS_TWO) shouldBe 1
                game.state.projectedState.getToughness(giant) shouldBe 1
            }

            test("a vigilant attacker never becomes tapped, so it gets no counter") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
                    .withCardAttachedTo(2, "Spirit Shackle", "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Serra Angel" to 2)).error shouldBe null
                game.resolveStack()

                val angel = game.findPermanent("Serra Angel")!!
                game.state.projectedState.getToughness(angel) shouldBe 4
            }

            test("tapping for a {T} ability triggers it, and the counter kills a 1/1") {
                val game = scenario()
                    .withPlayers("Spy", "Victim")
                    .withCardOnBattlefield(1, "Orcish Spy", summoningSickness = false)
                    .withCardAttachedTo(2, "Spirit Shackle", "Orcish Spy")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spy = game.findPermanent("Orcish Spy")!!
                val abilityId = cardRegistry.getCard("Orcish Spy")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spy,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("-0/-2 leaves the 1/1 at toughness -1") {
                    game.isInGraveyard(1, "Orcish Spy") shouldBe true
                }
                game.isInGraveyard(2, "Spirit Shackle") shouldBe true
            }
        }
    }
}
