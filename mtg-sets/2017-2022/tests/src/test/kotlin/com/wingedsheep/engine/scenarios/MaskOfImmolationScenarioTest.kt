package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mask of Immolation (M20 #151) — {1}{R} Equipment.
 *
 * "When this Equipment enters, create a 1/1 red Elemental creature token, then attach this
 *  Equipment to it. Equipped creature has 'Sacrifice this creature: It deals 1 damage to any
 *  target.' Equip {2}"
 *
 * Covers the create-then-attach ETB, the granted sacrifice ability, and the ruling that the
 * equipped creature (not the red Mask) is the source of the damage: a green Grizzly Bears wearing
 * the Mask can damage Paladin en-Vec despite its protection from red.
 */
class MaskOfImmolationScenarioTest : ScenarioTestBase() {

    private val grantedAbilityId = cardRegistry.getCard("Mask of Immolation")!!
        .staticAbilities.filterIsInstance<GrantActivatedAbility>().first().ability.id

    init {
        context("Mask of Immolation") {
            test("ETB creates a red Elemental, attaches to it, and the Elemental can sacrifice to ping") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Mask of Immolation")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Mask of Immolation")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val elemental = game.findPermanent("Elemental Token")
                withClue("An Elemental token should have been created") { elemental shouldNotBe null }
                val mask = game.findPermanent("Mask of Immolation")!!
                withClue("Mask should be attached to the Elemental token") {
                    game.state.getEntity(mask)?.get<AttachedToComponent>()?.targetId shouldBe elemental
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = elemental!!,
                        abilityId = grantedAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("The Elemental was sacrificed") { game.findPermanent("Elemental Token") shouldBe null }
                withClue("Bob took 1 damage") { game.getLifeTotal(2) shouldBe 19 }
                withClue("The Mask stays on the battlefield, unattached") {
                    game.findPermanent("Mask of Immolation") shouldNotBe null
                }
            }

            test("the equipped green creature, not the red Mask, is the damage source") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Mask of Immolation", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Paladin en-Vec")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val paladin = game.findPermanent("Paladin en-Vec")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bears,
                        abilityId = grantedAbilityId,
                        targets = listOf(ChosenTarget.Permanent(paladin))
                    )
                )
                withClue("Pro-red Paladin is targetable by the green Bears' ability: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears was sacrificed") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("Paladin en-Vec was dealt 1 damage by the green Bears") {
                    game.state.getEntity(paladin)?.get<DamageComponent>()?.amount shouldBe 1
                }
            }
        }
    }
}
