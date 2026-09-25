package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.VillainousOgre
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Villainous Ogre (CHK #148) — "As long as you control a Demon, this creature has
 * '{B}: Regenerate this creature.'"
 *
 * The regeneration ability is a self-scoped grant behind a condition, so the part worth proving is
 * that the ability exists with a Demon and is absent without one.
 */
class VillainousOgreScenarioTest : ScenarioTestBase() {

    private val regenerateAbility = VillainousOgre.staticAbilities
        .filterIsInstance<ConditionalStaticAbility>()
        .single()
        .let { (it.ability as GrantActivatedAbility).ability.id }

    init {
        context("Villainous Ogre") {

            test("with a Demon, it regenerates out of a destroy effect") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Villainous Ogre")
                    .withCardOnBattlefield(1, "Kuro, Pitlord") // Demon Spirit
                    .withCardInHand(1, "Rend Flesh")           // {2}{B}: Destroy target non-Spirit creature
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogre = game.findPermanent("Villainous Ogre")!!

                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = ogre, abilityId = regenerateAbility)
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                game.castSpell(1, "Rend Flesh", ogre).error shouldBe null
                game.resolveStack()

                withClue("the shield replaces the destruction — the Ogre survives, tapped") {
                    game.findPermanent("Villainous Ogre") shouldNotBe null
                    game.state.getEntity(ogre)?.has<TappedComponent>() shouldBe true
                }
            }

            test("without a Demon, it has no regeneration ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Villainous Ogre")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogre = game.findPermanent("Villainous Ogre")!!
                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = ogre, abilityId = regenerateAbility)
                )
                withClue("the granted ability is gated off") { activation.error shouldNotBe null }
            }
        }
    }
}
