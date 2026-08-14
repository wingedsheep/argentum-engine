package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.DramaticAccusation
import com.wingedsheep.sdk.core.AbilityFlag
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dramatic Accusation (MKM #53) — {2}{U} Enchantment — Aura.
 *
 * "Enchant creature"
 * "When this Aura enters, tap enchanted creature."
 * "Enchanted creature doesn't untap during its controller's untap step."
 * "{U}{U}: Shuffle enchanted creature into its owner's library."
 *
 * The lock is two separate pieces — a one-shot enters trigger and a continuous static — so both are
 * asserted: the creature ends up tapped, and it carries the untap restriction while the Aura is
 * attached. The activated ability gets its own test because it is the card's actual removal mode,
 * and because shuffling the host away should take the now-unattached Aura with it.
 */
class DramaticAccusationScenarioTest : ScenarioTestBase() {

    private val shuffleAbility = DramaticAccusation.activatedAbilities.first().id

    init {
        context("Dramatic Accusation") {

            test("entering taps the enchanted creature and keeps it from untapping") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Dramatic Accusation")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Dramatic Accusation", bears).error shouldBe null
                game.resolveStack()

                withClue("the enters trigger taps it") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
                withClue("and the static keeps it from untapping") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
                }
            }

            test("the activated ability shuffles the enchanted creature into its owner's library") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Dramatic Accusation", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .build()

                val aura = game.findPermanent("Dramatic Accusation")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = aura, abilityId = shuffleAbility)
                ).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("the creature is gone from the battlefield and back in its owner's library") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.findCardsInLibrary(2, "Grizzly Bears").size shouldBe 1
                }
                withClue("an Aura attached to nothing is put into its owner's graveyard") {
                    game.isOnBattlefield("Dramatic Accusation") shouldBe false
                    game.isInGraveyard(1, "Dramatic Accusation") shouldBe true
                }
            }
        }
    }
}
