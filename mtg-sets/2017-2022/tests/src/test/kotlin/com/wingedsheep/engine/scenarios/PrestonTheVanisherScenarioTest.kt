package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Preston, the Vanisher — {3}{W} Legendary Creature — Rabbit Wizard 2/5.
 *   Whenever another nontoken creature you control enters, if it wasn't cast, create a token that's
 *   a copy of that creature, except it's a 0/1 white Illusion.
 *   {1}{W}, Sacrifice five Illusions: Exile target nonland permanent.
 */
class PrestonTheVanisherScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokensNamed(name: String) =
        findAllPermanents(name).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        test("a reanimated creature gets a 0/1 white Illusion token copy") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Preston, the Vanisher")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Resurrection")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpellTargetingGraveyardCard(1, "Resurrection", listOf(bearsCard)).error shouldBe null
            game.resolveStack()

            val bears = game.findAllPermanents("Grizzly Bears")
            withClue("the original plus one token copy") { bears.size shouldBe 2 }
            val token = game.tokensNamed("Grizzly Bears").single()
            val projected = game.state.projectedState
            withClue("the copy is a 0/1") {
                projected.getPower(token) shouldBe 0
                projected.getToughness(token) shouldBe 1
            }
            withClue("the copy is white only") { projected.getColors(token) shouldBe setOf("WHITE") }
            withClue("the copy is an Illusion instead of a Bear") {
                projected.getSubtypes(token) shouldBe setOf("Illusion")
            }
            val original = bears.single { it != token }
            withClue("the original is untouched") {
                projected.getPower(original) shouldBe 2
                projected.getSubtypes(original) shouldBe setOf("Bear")
            }
        }

        test("a cast creature is not copied") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Preston, the Vanisher")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Grizzly Bears").size shouldBe 1
            game.tokensNamed("Grizzly Bears").size shouldBe 0
        }

        test("a creature cast from exile was cast, so it is not copied") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Preston, the Vanisher")
                .withCardInExile(1, "Squee, the Immortal")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellFromExile(1, "Squee, the Immortal").error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Squee, the Immortal").size shouldBe 1
            game.tokensNamed("Squee, the Immortal").size shouldBe 0
        }

        test("creature tokens are not copied") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Preston, the Vanisher")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            withClue("only the two Soldier tokens — no Illusion copies") {
                game.findAllPermanents("Soldier Token").size shouldBe 2
                game.state.getBattlefield().count { game.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 2
            }
        }

        test("{1}{W}, sacrifice five Illusions exiles target nonland permanent") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Preston, the Vanisher")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder.withCardOnBattlefield(1, "Phantom Warrior") }
            val game = builder.build()

            val preston = game.findPermanent("Preston, the Vanisher")!!
            val illusions = game.findAllPermanents("Phantom Warrior")
            illusions.size shouldBe 5
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Preston, the Vanisher")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = preston,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = illusions),
                )
            ).outcome shouldBe Outcome.Done
            game.resolveStack()

            withClue("the five Illusions were sacrificed") { game.findAllPermanents("Phantom Warrior").size shouldBe 0 }
            withClue("the target was exiled") { game.isInExile(2, "Grizzly Bears") shouldBe true }
        }

        test("four Illusions are not enough to activate") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Preston, the Vanisher")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(4) { builder.withCardOnBattlefield(1, "Phantom Warrior") }
            val game = builder.build()

            val preston = game.findPermanent("Preston, the Vanisher")!!
            val abilityId = cardRegistry.getCard("Preston, the Vanisher")!!.script.activatedAbilities[0].id
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = preston,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = game.findAllPermanents("Phantom Warrior")),
                )
            )
            result.error shouldNotBe null
            withClue("nothing was sacrificed or exiled") {
                game.findAllPermanents("Phantom Warrior").size shouldBe 4
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
