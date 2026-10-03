package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.EssenceReliquary
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Essence Reliquary (MH3 #24) — {2}{W} Artifact.
 *
 * "{T}: Return another target permanent you control and all Auras you control attached to it to
 * their owner's hand. Activate only during your turn."
 */
class EssenceReliquaryScenarioTest : ScenarioTestBase() {

    private val abilityId = EssenceReliquary.activatedAbilities[0].id

    init {
        context("Essence Reliquary") {

            test("returns the target and your Auras on it to hand; an opponent's Aura is left behind") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Essence Reliquary")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Essence Reliquary")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the target goes back to its owner's hand") {
                    game.findPermanents("Grizzly Bears").size shouldBe 0
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("an Aura you control attached to it rides along") {
                    game.findPermanents("Holy Strength").size shouldBe 0
                    game.isInHand(1, "Holy Strength") shouldBe true
                }
                withClue("an Aura you don't control stays behind and dies unattached") {
                    game.isInHand(2, "Pacifism") shouldBe false
                    game.isInGraveyard(2, "Pacifism") shouldBe true
                }
            }

            test("can't target itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Essence Reliquary")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val reliquary = game.findPermanent("Essence Reliquary")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = reliquary,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(reliquary))
                    )
                ).error shouldNotBe null
            }

            test("can't target a permanent an opponent controls") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Essence Reliquary")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Essence Reliquary")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                    )
                ).error shouldNotBe null
            }

            test("can't be activated during an opponent's turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Essence Reliquary")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Essence Reliquary")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                    )
                ).error shouldNotBe null
                game.findPermanents("Grizzly Bears").size shouldBe 1
            }
        }
    }
}
