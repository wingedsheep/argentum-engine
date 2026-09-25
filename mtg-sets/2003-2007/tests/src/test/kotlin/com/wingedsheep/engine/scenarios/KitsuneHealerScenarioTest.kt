package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KitsuneHealer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kitsune Healer (CHK #27) — "{T}: Prevent the next 1 damage that would be dealt to any target
 * this turn." / "{T}: Prevent all damage that would be dealt to target legendary creature this turn."
 */
class KitsuneHealerScenarioTest : ScenarioTestBase() {

    private val preventOne = KitsuneHealer.activatedAbilities[0].id
    private val preventAll = KitsuneHealer.activatedAbilities[1].id

    init {
        context("Kitsune Healer") {

            fun build() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Kitsune Healer")
                .withCardOnBattlefield(1, "Kodama of the South Tree")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("the first ability prevents only the next 1 damage") {
                val game = build()
                val healer = game.findPermanent("Kitsune Healer")!!

                game.execute(
                    ActivateAbility(
                        game.player1Id, healer, preventOne,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Bolt's 3 damage is reduced by 1") { game.getLifeTotal(2) shouldBe 18 }
            }

            test("the second ability prevents all damage to a legendary creature") {
                val game = build()
                val healer = game.findPermanent("Kitsune Healer")!!
                val kodama = game.findPermanent("Kodama of the South Tree")!!

                game.execute(
                    ActivateAbility(
                        game.player1Id, healer, preventAll,
                        targets = listOf(ChosenTarget.Permanent(kodama))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Lightning Bolt", kodama).error shouldBe null
                game.resolveStack()

                withClue("no damage was marked on the Kodama") {
                    (game.state.getEntity(kodama)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
            }

            test("the second ability can't target a nonlegendary creature") {
                val game = build()
                val healer = game.findPermanent("Kitsune Healer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        game.player1Id, healer, preventAll,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
