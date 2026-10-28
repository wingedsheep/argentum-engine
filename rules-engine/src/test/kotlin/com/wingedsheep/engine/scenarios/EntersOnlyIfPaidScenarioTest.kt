package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SpellCounteredEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `EntersOnlyIfPaid` — "If this would enter, you may [pay] instead. If you do, put it onto the
 * battlefield. If you don't, put it into its owner's graveyard." (Mox Diamond). A replacement
 * effect on the card's own entry (an "instead" effect, CR 614.1a): unpaid, the card never enters, so nothing that looks
 * for a permanent entering sees it (Mox Diamond ruling, 2008-05-01).
 *
 * Covered on both entry paths: a permanent spell resolving, and an effect putting the card onto
 * the battlefield — by a single move (Refurbish, `MoveToZoneEffectExecutor`) and by a collection move
 * (Arms Race, `MoveCollectionExecutor`). Reckless Fireweaver ("whenever an artifact you control
 * enters, deal 1 damage to each opponent") is the witness for "never entered".
 */
class EntersOnlyIfPaidScenarioTest : ScenarioTestBase() {

    private fun TestGame.graveyardCard(playerNumber: Int, name: String): EntityId =
        findCardsInGraveyard(playerNumber, name).first()

    private fun TestGame.paymentPrompt(): SelectCardsDecision {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        return decision
    }

    init {
        context("cast from hand") {
            test("discarding a land puts it onto the battlefield, and it enters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mox Diamond")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Reckless Fireweaver")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mox Diamond").error shouldBe null
                game.resolveStack()

                val prompt = game.paymentPrompt()
                prompt.playerId shouldBe game.player1Id
                prompt.minSelections shouldBe 0
                prompt.maxSelections shouldBe 1
                withClue("only land cards pay the cost") {
                    prompt.options shouldContainExactlyInAnyOrder game.findCardsInHand(1, "Forest")
                }
                prompt.prompt shouldBe "Discard a land to put Mox Diamond onto the battlefield? " +
                    "If you don't, it goes to the graveyard."

                game.selectCards(game.findCardsInHand(1, "Forest")).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("it entered, so the artifact-enters trigger fired") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("declining puts it into the graveyard from the stack — it never enters and isn't countered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mox Diamond")
                    .withCardInHand(1, "Forest")
                    .withCardOnBattlefield(1, "Reckless Fireweaver")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mox Diamond").error shouldBe null
                game.resolveStack()
                game.paymentPrompt()

                val answer = game.selectCards(emptyList())
                answer.error shouldBe null
                withClue("it resolved; it was not countered") {
                    answer.events.filterIsInstance<SpellCounteredEvent>().shouldBeEmpty()
                }
                val move = answer.events.filterIsInstance<ZoneChangeEvent>().single()
                move.fromZone shouldBe Zone.STACK
                move.toZone shouldBe Zone.GRAVEYARD
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe false
                game.isInGraveyard(1, "Mox Diamond") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                game.state.stack.shouldBeEmpty()
                withClue("it never entered, so nothing saw an artifact enter") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("with no land card in hand it goes straight to the graveyard without a prompt") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mox Diamond")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Reckless Fireweaver")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mox Diamond").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Mox Diamond") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("put onto the battlefield by an effect") {
            fun reanimation() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Refurbish")
                .withCardInHand(1, "Forest")
                .withCardInGraveyard(1, "Mox Diamond")
                .withCardOnBattlefield(1, "Reckless Fireweaver")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("a reanimation that pays the cost returns it to the battlefield") {
                val game = reanimation()
                val mox = game.graveyardCard(1, "Mox Diamond")
                game.castSpellTargetingGraveyardCard(1, "Refurbish", listOf(mox)).error shouldBe null
                game.resolveStack()

                game.selectCards(game.findCardsInHand(1, "Forest")).error shouldBe null
                game.resolveStack()

                game.findPermanent("Mox Diamond") shouldBe mox
                game.isInGraveyard(1, "Forest") shouldBe true
                game.getLifeTotal(2) shouldBe 19
            }

            test("a reanimation that isn't paid for leaves it in the graveyard") {
                val game = reanimation()
                val mox = game.graveyardCard(1, "Mox Diamond")
                game.castSpellTargetingGraveyardCard(1, "Refurbish", listOf(mox)).error shouldBe null
                game.resolveStack()

                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe false
                game.isInGraveyard(1, "Mox Diamond") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            fun armsRace() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mox Diamond")
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Arms Race")
                .withCardOnBattlefield(1, "Reckless Fireweaver")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.putMoxWithArmsRace() {
                val armsRace = findPermanent("Arms Race")!!
                val ability = cardRegistry.getCard("Arms Race")!!.activatedAbilities[0].id
                execute(ActivateAbility(player1Id, armsRace, ability)).error shouldBe null
                resolveStack()
                selectCards(findCardsInHand(1, "Mox Diamond")).error shouldBe null
                resolveStack()
            }

            test("putting it from hand and paying the cost puts it onto the battlefield") {
                val game = armsRace()
                game.putMoxWithArmsRace()

                val prompt = game.paymentPrompt()
                prompt.options shouldContainExactlyInAnyOrder game.findCardsInHand(1, "Forest")
                game.selectCards(prompt.options).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                game.getLifeTotal(2) shouldBe 19
            }

            test("putting it from hand without paying puts it into the graveyard instead") {
                val game = armsRace()
                game.putMoxWithArmsRace()

                game.paymentPrompt()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe false
                game.isInGraveyard(1, "Mox Diamond") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("returned by a duration ending — \"until this leaves the battlefield\"") {
            fun banished() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Banishing Light")
                .withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Mox Diamond")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.banishThenFreeMox() {
                val mox = findPermanent("Mox Diamond")!!
                castSpell(1, "Banishing Light").error shouldBe null
                resolveStack()
                selectTargets(listOf(mox)).error shouldBe null
                resolveStack()
                isInExile(2, "Mox Diamond") shouldBe true
                castSpell(1, "Disenchant", findPermanent("Banishing Light")!!).error shouldBe null
                resolveStack()
            }

            test("its owner is asked as it returns, and paying puts it back") {
                val game = banished()
                game.banishThenFreeMox()

                val prompt = game.paymentPrompt()
                withClue("the Mox's owner pays, not the player who freed it") {
                    prompt.playerId shouldBe game.player2Id
                }
                game.selectCards(game.findCardsInHand(2, "Forest")).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe true
                game.isInGraveyard(2, "Forest") shouldBe true
            }

            test("declining puts it into its owner's graveyard from exile") {
                val game = banished()
                game.banishThenFreeMox()

                game.paymentPrompt()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mox Diamond") shouldBe false
                game.isInGraveyard(2, "Mox Diamond") shouldBe true
                game.isInHand(2, "Forest") shouldBe true
            }
        }

        context("a copy of it asks too (CR 614.12)") {
            fun steel() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mox Diamond")
                .withCardInHand(1, "Sculpting Steel")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.castSteelCopyingMox(): EntityId {
                val steel = findCardsInHand(1, "Sculpting Steel").single()
                castSpell(1, "Sculpting Steel").error shouldBe null
                resolveStack()
                selectCards(listOf(findPermanent("Mox Diamond")!!)).error shouldBe null
                return steel
            }

            test("Sculpting Steel entering as a copy of Mox Diamond discards a land to enter") {
                val game = steel()
                val steel = game.castSteelCopyingMox()

                val prompt = game.paymentPrompt()
                prompt.options shouldContainExactlyInAnyOrder game.findCardsInHand(1, "Forest")
                game.selectCards(prompt.options).error shouldBe null
                game.resolveStack()

                game.findPermanents("Mox Diamond").size shouldBe 2
                game.state.getBattlefield().contains(steel) shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
            }

            test("declining puts the copy into the graveyard as its printed card") {
                val game = steel()
                game.castSteelCopyingMox()

                game.paymentPrompt()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.findPermanents("Mox Diamond").size shouldBe 1
                withClue("off the stack it is Sculpting Steel again (CR 400.7)") {
                    game.isInGraveyard(1, "Sculpting Steel") shouldBe true
                }
                game.isInHand(1, "Forest") shouldBe true
            }
        }

        test("a declined reanimation under Rest in Peace leaves it in the graveyard, not exile") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Refurbish")
                .withCardInHand(1, "Forest")
                .withCardInGraveyard(1, "Mox Diamond")
                .withCardOnBattlefield(1, "Rest in Peace")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mox = game.graveyardCard(1, "Mox Diamond")
            game.castSpellTargetingGraveyardCard(1, "Refurbish", listOf(mox)).error shouldBe null
            game.resolveStack()

            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            withClue("it never left the graveyard, so nothing replaced a move into it") {
                game.isInGraveyard(1, "Mox Diamond") shouldBe true
                game.isInExile(1, "Mox Diamond") shouldBe false
            }
        }

        test("the card's own text names the replacement") {
            val text = cardRegistry.getCard("Mox Diamond")!!.script.replacementEffects.single().description
            text shouldBe "If this permanent would enter, you may discard a land instead. If you do, " +
                "put it onto the battlefield. If you don't, put it into its owner's graveyard"
        }
    }
}
