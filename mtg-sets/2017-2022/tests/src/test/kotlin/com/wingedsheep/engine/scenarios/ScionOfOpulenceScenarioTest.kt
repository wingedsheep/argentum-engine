package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.voc.cards.ScionOfOpulence
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scion of Opulence (Crimson Vow Commander #28) — {2}{R} Creature — Vampire Noble 3/1.
 * "Whenever this creature or another nontoken Vampire you control dies, create a Treasure token.
 *  {R}, Sacrifice two artifacts: Exile the top card of your library. You may play that card this turn."
 */
class ScionOfOpulenceScenarioTest : ScenarioTestBase() {
    private val impulseAbility = ScionOfOpulence.activatedAbilities.single().id

    init {
        test("another nontoken Vampire dying makes a Treasure; a non-Vampire doesn't; then the Scion's own death does") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Scion of Opulence")
                .withCardOnBattlefield(1, "Child of Night")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            withClue("a non-Vampire dying makes nothing") { game.findPermanents("Treasure").size shouldBe 0 }

            game.castSpell(2, "Lightning Bolt", game.findPermanent("Child of Night")!!).error shouldBe null
            game.resolveStack()
            game.findPermanents("Treasure").size shouldBe 1

            game.castSpell(2, "Lightning Bolt", game.findPermanent("Scion of Opulence")!!).error shouldBe null
            game.resolveStack()
            game.findPermanents("Treasure").size shouldBe 2
        }

        test("{R}, sacrifice two artifacts: exiles the top card of your library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Scion of Opulence")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val scion = game.findPermanent("Scion of Opulence")!!
            val thopters = game.findPermanents("Ornithopter")

            game.execute(
                ActivateAbility(
                    game.player1Id, scion, impulseAbility,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = thopters),
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanents("Ornithopter").size shouldBe 0
            game.isInExile(1, "Grizzly Bears") shouldBe true
            withClue("the exiled card is playable this turn") {
                game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
