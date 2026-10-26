package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.JornGodOfWinter
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jorn, God of Winter // Kaldring, the Rimestaff.
 *
 * Front: "Whenever Jorn attacks, untap each snow permanent you control." Back: "{T}: You may play
 * target snow permanent card from your graveyard this turn. If you do, it enters tapped." — a land
 * played or a permanent spell cast through the grant enters tapped; the land still needs a land drop.
 */
class JornGodOfWinterScenarioTest : ScenarioTestBase() {

    private val kaldringAbilityId = JornGodOfWinter.backFace!!.activatedAbilities.single().id

    private val snowBear = card("Jorn Test Snow Bear") {
        manaCost = "{1}"
        typeLine = "Snow Creature — Bear"
        power = 2
        toughness = 2
    }
    private val bear = card("Jorn Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val snowLand = card("Jorn Test Snow Land") {
        typeLine = "Snow Land"
    }

    private fun TestGame.graveyardCard(name: String): EntityId =
        state.getGraveyard(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.isTapped(name: String) =
        state.getEntity(findPermanent(name)!!)?.has<TappedComponent>() == true

    private fun TestGame.activateKaldring(targetId: EntityId) = execute(
        ActivateAbility(
            player1Id, findPermanent("Kaldring, the Rimestaff")!!, kaldringAbilityId,
            targets = listOf(ChosenTarget.Card(targetId, player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)),
        )
    )

    private fun kaldringBoard(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Kaldring, the Rimestaff")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInGraveyard(1, snowBear.name)
        .withCardInGraveyard(1, bear.name)
        .withCardInGraveyard(1, snowLand.name)
        .withCardInHand(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(JornGodOfWinter)
        cardRegistry.register(listOf(snowBear, bear, snowLand))

        context("Kaldring, the Rimestaff") {
            test("a snow creature cast from the graveyard this way enters tapped") {
                val game = kaldringBoard()
                val snowBearId = game.graveyardCard(snowBear.name)
                game.activateKaldring(snowBearId).error shouldBe null
                game.resolveStack()

                game.execute(CastSpell(game.player1Id, snowBearId)).error shouldBe null
                game.resolveStack()
                game.isTapped(snowBear.name) shouldBe true
            }

            test("a snow land played from the graveyard this way enters tapped and uses the land drop") {
                val game = kaldringBoard()
                val landId = game.graveyardCard(snowLand.name)
                game.activateKaldring(landId).error shouldBe null
                game.resolveStack()

                game.execute(PlayLand(game.player1Id, landId)).error shouldBe null
                game.isTapped(snowLand.name) shouldBe true
                withClue("the graveyard play spent the turn's land drop") {
                    game.execute(PlayLand(game.player1Id, game.state.getHand(game.player1Id).single())).error shouldNotBe null
                }
            }

            test("a non-snow permanent card can't be targeted") {
                val game = kaldringBoard()
                game.activateKaldring(game.graveyardCard(bear.name)).error shouldNotBe null
            }

            test("without the ability the snow card stays locked in the graveyard") {
                val game = kaldringBoard()
                game.execute(CastSpell(game.player1Id, game.graveyardCard(snowBear.name))).error shouldNotBe null
            }
        }

        context("Jorn, God of Winter") {
            test("attacking untaps each snow permanent you control, Jorn included, and nothing else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jorn, God of Winter")
                    .withCardOnBattlefield(1, snowLand.name, tapped = true)
                    .withCardOnBattlefield(1, snowBear.name, tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Jorn, God of Winter" to 2)).error shouldBe null
                game.resolveStack()

                withClue("Jorn (snow) untaps itself after tapping to attack") { game.isTapped("Jorn, God of Winter") shouldBe false }
                withClue("snow land") { game.isTapped(snowLand.name) shouldBe false }
                withClue("snow creature") { game.isTapped(snowBear.name) shouldBe false }
                withClue("non-snow Forest stays tapped") { game.isTapped("Forest") shouldBe true }
            }
        }
    }
}
