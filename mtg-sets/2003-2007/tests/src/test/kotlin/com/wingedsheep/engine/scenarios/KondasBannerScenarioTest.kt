package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KondasBanner
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Konda's Banner (CHK #259) — "Konda's Banner can be attached only to a legendary creature.
 * Creatures that share a color with equipped creature get +1/+1. Creatures that share a creature
 * type with equipped creature get +1/+1. Equip {2}"
 */
class KondasBannerScenarioTest : ScenarioTestBase() {

    init {
        context("Konda's Banner") {

            test("equipping a legendary creature pumps every creature sharing a color and/or a creature type") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Konda's Banner")
                    .withCardOnBattlefield(1, "Konda, Lord of Eiganjo")   // W Human Samurai 3/3, legendary
                    .withCardOnBattlefield(1, "Kitsune Blademaster")      // W Fox Samurai 2/2: color + type
                    .withCardOnBattlefield(1, "Numai Outcast")            // B Human Samurai 1/1: type only
                    .withCardOnBattlefield(1, "Woodland Changeling")      // G changeling 2/2: type only
                    .withCardOnBattlefield(1, "Grizzly Bears")            // G Bear 2/2: neither
                    .withCardOnBattlefield(2, "Kitsune Healer")           // opponent's W Fox Cleric 2/2: color only
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val banner = game.findPermanent("Konda's Banner")!!
                val konda = game.findPermanent("Konda, Lord of Eiganjo")!!

                withClue("unattached, the Banner pumps nothing") {
                    game.state.projectedState.getPower(game.findPermanent("Kitsune Blademaster")!!) shouldBe 2
                }

                val equip = KondasBanner.activatedAbilities.single { it.isEquipAbility }
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = banner,
                        abilityId = equip.id,
                        targets = listOf(ChosenTarget.Permanent(konda))
                    )
                )
                withClue("equip should activate: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(banner)?.get<AttachedToComponent>()?.targetId shouldBe konda

                val projected = game.state.projectedState
                fun stats(name: String) = game.findPermanent(name)!!.let {
                    projected.getPower(it) to projected.getToughness(it)
                }
                withClue("the equipped creature shares a color and a type with itself") {
                    stats("Konda, Lord of Eiganjo") shouldBe (5 to 5)
                }
                withClue("sharing both a color and a type gives +2/+2; sharing two types still only +1/+1") {
                    stats("Kitsune Blademaster") shouldBe (4 to 4)
                }
                withClue("sharing only a creature type gives +1/+1") {
                    stats("Numai Outcast") shouldBe (2 to 2)
                }
                withClue("a changeling shares a creature type (read off the projected types)") {
                    stats("Woodland Changeling") shouldBe (3 to 3)
                }
                withClue("sharing neither gives nothing") {
                    stats("Grizzly Bears") shouldBe (2 to 2)
                }
                withClue("an opponent's creature sharing a color is pumped too") {
                    stats("Kitsune Healer") shouldBe (3 to 3)
                }
            }

            test("equip targeting a nonlegendary creature resolves but the Banner doesn't move") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Konda's Banner")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val banner = game.findPermanent("Konda's Banner")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val equip = KondasBanner.activatedAbilities.single { it.isEquipAbility }
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = banner,
                        abilityId = equip.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("equip still targets any creature you control: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("CR 701.3b: an Equipment can't be attached to a creature it can't equip") {
                    game.state.getEntity(banner)?.get<AttachedToComponent>() shouldBe null
                }
                game.state.projectedState.getPower(bears) shouldBe 2
            }

            test("attached to a nonlegendary creature, the Banner becomes unattached as a state-based action") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Konda's Banner", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val banner = game.findPermanent("Konda's Banner")!!
                game.checkStateBasedActions()

                withClue("CR 704.5n: an Equipment attached to an illegal permanent becomes unattached") {
                    game.state.getEntity(banner)?.get<AttachedToComponent>() shouldBe null
                }
                withClue("it stays on the battlefield") {
                    (banner in game.state.getBattlefield()) shouldBe true
                }
            }
        }
    }
}
