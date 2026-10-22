package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Flame Channeler // Embodiment of Flame (MID).
 *
 * Pins the spell-damage transform (a fight spell's creatures are the damage source, so it doesn't
 * count), the back face's flame counter, and the counter-fuelled impulse draw.
 */
class FlameChannelerScenarioTest : ScenarioTestBase() {

    private val impulseAbilityId
        get() = cardRegistry.getCard("Embodiment of Flame")!!.script.activatedAbilities[0].id

    private fun builder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Flame Channeler")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.name(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.flame(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.FLAME) ?: 0

    init {
        context("Flame Channeler") {
            test("transforms when a spell you control deals damage") {
                val game = builder().withCardInHand(1, "Shock").build()
                val channeler = game.findPermanent("Flame Channeler")!!

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.name(channeler) shouldBe "Embodiment of Flame"
                withClue("the transforming trigger doesn't also add a flame counter") {
                    game.flame(channeler) shouldBe 0
                }
            }

            test("a fight spell's damage is dealt by the creatures, not the spell") {
                val game = builder()
                    .withCardInHand(1, "Prey Upon")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .build()
                val channeler = game.findPermanent("Flame Channeler")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!

                val preyUpon = game.findCardsInHand(1, "Prey Upon").single()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        preyUpon,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(elves)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Llanowar Elves") shouldBe false
                game.name(channeler) shouldBe "Flame Channeler"
            }
        }

        context("Embodiment of Flame") {
            test("a damaging spell adds a flame counter, which pays for an impulse draw") {
                val game = builder()
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .build()
                val channeler = game.findPermanent("Flame Channeler")!!

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                game.name(channeler) shouldBe "Embodiment of Flame"

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.flame(channeler) shouldBe 1

                game.execute(ActivateAbility(game.player1Id, channeler, impulseAbilityId)).error shouldBe null
                game.resolveStack()

                game.flame(channeler) shouldBe 0
                game.isInExile(1, "Grizzly Bears") shouldBe true
                withClue("the exiled card is playable this turn") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                }
            }

            test("the impulse ability can't be activated without a flame counter") {
                val game = builder().withCardInHand(1, "Shock").withCardInLibrary(1, "Grizzly Bears").build()
                val channeler = game.findPermanent("Flame Channeler")!!
                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                game.name(channeler) shouldBe "Embodiment of Flame"

                (game.execute(ActivateAbility(game.player1Id, channeler, impulseAbilityId)).error != null) shouldBe true
            }
        }
    }
}
