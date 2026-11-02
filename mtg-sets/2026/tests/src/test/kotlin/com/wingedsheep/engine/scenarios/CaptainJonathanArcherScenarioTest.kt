package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Captain Jonathan Archer ({2}{G} Legendary Creature — Human Officer, 3/3):
 * "When Captain Archer enters, create Porthos, a legendary 1/1 green Dog creature token.
 * Federation — {G}, {T}: Target creature you control gets +X/+X until end of turn, where X is the
 * number of creature types among non-Borg creatures you control. Activate only as a sorcery."
 */
class CaptainJonathanArcherScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateFederation(archer: EntityId, target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = archer,
            abilityId = cardRegistry.getCard("Captain Jonathan Archer")!!.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(target)),
        )
    )

    init {
        test("entering creates Porthos, a legendary 1/1 green Dog creature token") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Captain Jonathan Archer")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Captain Jonathan Archer").error shouldBe null
            game.resolveStack()

            val porthos = game.findPermanent("Porthos")
            withClue("Porthos is on the battlefield") { porthos shouldNotBe null }
            val projected = game.state.projectedState
            projected.isCreature(porthos!!) shouldBe true
            projected.isLegendary(porthos) shouldBe true
            projected.hasSubtype(porthos, "Dog") shouldBe true
            projected.getColors(porthos) shouldBe setOf(Color.GREEN.name)
            projected.getPower(porthos) shouldBe 1
            projected.getToughness(porthos) shouldBe 1
        }

        test("Federation counts creature types among non-Borg creatures you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Captain Jonathan Archer", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Assembly Specialist")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val archer = game.findPermanent("Captain Jonathan Archer")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activateFederation(archer, bears).error shouldBe null
            game.resolveStack()

            withClue("Human + Officer + Bear = 3; the Borg Assembly Specialist adds nothing") {
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 5
            }
        }

        test("Federation can't be activated outside a main phase") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Captain Jonathan Archer", summoningSickness = false)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            val archer = game.findPermanent("Captain Jonathan Archer")!!
            game.activateFederation(archer, archer).error shouldNotBe null
        }
    }
}
