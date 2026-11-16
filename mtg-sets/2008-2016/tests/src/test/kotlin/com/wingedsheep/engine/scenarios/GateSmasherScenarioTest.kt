package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dtk.cards.GateSmasher
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gate Smasher (DTK #239) — "This Equipment can be attached only to a creature with toughness 4 or
 * greater. Equipped creature gets +3/+0 and has trample. Equip {3}"
 *
 * The restriction is a toughness check on projected state, so it has to hold both when the equip
 * ability resolves and continuously afterwards (the card's two rulings).
 */
class GateSmasherScenarioTest : ScenarioTestBase() {

    init {
        context("Gate Smasher") {

            fun equip(game: TestGame, creatureName: String) {
                val smasher = game.findPermanent("Gate Smasher")!!
                val creature = game.findPermanent(creatureName)!!
                val equip = GateSmasher.activatedAbilities.single { it.isEquipAbility }
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = smasher,
                        abilityId = equip.id,
                        targets = listOf(ChosenTarget.Permanent(creature))
                    )
                )
                withClue("equip may target any creature you control: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
            }

            test("equipping a creature with toughness 4 gives +3/+0 and trample") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Gate Smasher")
                    .withCardOnBattlefield(1, "Craw Wurm")   // 6/4
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                equip(game, "Craw Wurm")

                val smasher = game.findPermanent("Gate Smasher")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                game.state.getEntity(smasher)?.get<AttachedToComponent>()?.targetId shouldBe wurm
                game.state.projectedState.getPower(wurm) shouldBe 9
                game.state.projectedState.getToughness(wurm) shouldBe 4
                game.state.projectedState.hasKeyword(wurm, Keyword.TRAMPLE) shouldBe true
            }

            test("equip targeting a creature with toughness 3 or less resolves but Gate Smasher doesn't move") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Gate Smasher")
                    .withCardOnBattlefield(1, "Grizzly Bears")   // 2/2
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                equip(game, "Grizzly Bears")

                val smasher = game.findPermanent("Gate Smasher")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("an Equipment can't become attached to a creature it can't equip") {
                    game.state.getEntity(smasher)?.get<AttachedToComponent>() shouldBe null
                }
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
            }

            test("when the equipped creature's toughness drops below 4, Gate Smasher becomes unattached") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Craw Wurm")   // 6/4
                    .withCardAttachedTo(1, "Gate Smasher", "Craw Wurm")
                    .withCardInHand(1, "Disfigure")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val smasher = game.findPermanent("Gate Smasher")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                game.state.projectedState.getPower(wurm) shouldBe 9

                val cast = game.castSpell(1, "Disfigure", wurm)
                withClue("Disfigure should be cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("a 7/2 can no longer carry Gate Smasher; it falls off as a state-based action") {
                    game.state.getEntity(smasher)?.get<AttachedToComponent>() shouldBe null
                }
                withClue("Gate Smasher stays on the battlefield") {
                    (smasher in game.state.getBattlefield()) shouldBe true
                }
                game.state.projectedState.getPower(wurm) shouldBe 4
                game.state.projectedState.getToughness(wurm) shouldBe 2
                game.state.projectedState.hasKeyword(wurm, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
