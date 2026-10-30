package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Master Transmuter (CON #31) — "{U}, {T}, Return an artifact you control to its owner's hand: You
 * may put an artifact card from your hand onto the battlefield."
 *
 * The Transmuter may return itself to pay its own cost (ruling) — the tap and the bounce both land
 * on the source — and the card returned is a legal choice for the put (ruling).
 */
class MasterTransmuterScenarioTest : ScenarioTestBase() {

    init {
        context("Master Transmuter") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Master Transmuter")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInHand(1, "Juggernaut")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.activate(bounced: com.wingedsheep.sdk.model.EntityId) {
                val transmuter = findPermanent("Master Transmuter")!!
                val ability = cardRegistry.getCard("Master Transmuter")!!.script.activatedAbilities.first()
                execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = transmuter,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(bounced)),
                    )
                ).error shouldBe null
            }

            test("returning another artifact puts an artifact card from hand onto the battlefield") {
                val game = board()
                val thopter = game.findPermanent("Ornithopter")!!
                game.activate(thopter)
                withClue("Ornithopter was returned as the cost") {
                    game.isInHand(1, "Ornithopter") shouldBe true
                }

                game.resolveStack()
                val juggernaut = game.findCardsInHand(1, "Juggernaut").single()
                game.selectCards(listOf(juggernaut)).error shouldBe null

                game.isOnBattlefield("Juggernaut") shouldBe true
                game.isInHand(1, "Juggernaut") shouldBe false
            }

            test("it can return itself to pay the cost, then put itself back as a new object") {
                val game = board()
                val transmuter = game.findPermanent("Master Transmuter")!!
                game.activate(transmuter)
                withClue("the Transmuter tapped and returned itself") {
                    game.isOnBattlefield("Master Transmuter") shouldBe false
                    game.isInHand(1, "Master Transmuter") shouldBe true
                }

                game.resolveStack()
                val inHand = game.findCardsInHand(1, "Master Transmuter").single()
                game.selectCards(listOf(inHand)).error shouldBe null

                val returned = game.findPermanent("Master Transmuter")
                returned shouldNotBe null
                withClue("Juggernaut stays in hand — only one card is put") {
                    game.isInHand(1, "Juggernaut") shouldBe true
                }
            }

            test("putting a card is optional") {
                val game = board()
                game.activate(game.findPermanent("Ornithopter")!!)
                game.resolveStack()
                game.skipSelection().error shouldBe null

                game.isInHand(1, "Juggernaut") shouldBe true
                game.isInHand(1, "Ornithopter") shouldBe true
            }
        }
    }
}
