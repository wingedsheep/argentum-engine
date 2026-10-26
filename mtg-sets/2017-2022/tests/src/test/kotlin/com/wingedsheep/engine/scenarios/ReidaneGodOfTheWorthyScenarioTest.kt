package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.ReidaneGodOfTheWorthy
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reidane, God of the Worthy // Valkmira, Protector's Shield.
 *
 * Valkmira's trigger is the "you or another permanent you control" becomes-target wording: the
 * player half (`players = Player.You`) and the object half (`another` + "you control") each fire,
 * only for an opponent's spell or ability, and counter it unless its controller pays {1}. Its
 * prevention takes 1 off each opponent-controlled source's damage. Reidane taxes opponents'
 * noncreature spells of mana value 4+ by {2} and makes their snow lands enter tapped.
 */
class ReidaneGodOfTheWorthyScenarioTest : ScenarioTestBase() {

    private val bigSorcery = card("Reidane Test Big Sorcery") {
        manaCost = "{4}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    private val smallSorcery = card("Reidane Test Small Sorcery") {
        manaCost = "{3}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    init {
        cardRegistry.register(ReidaneGodOfTheWorthy)
        cardRegistry.register(listOf(bigSorcery, smallSorcery))

        fun valkmiraBoard(opponentMountains: Int): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Valkmira, Protector's Shield")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(2, "Lightning Bolt")
            .withLandsOnBattlefield(2, "Mountain", opponentMountains)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Valkmira, Protector's Shield") {
            test("an opponent's spell targeting you is countered unless they pay {1}") {
                val game = valkmiraBoard(opponentMountains = 2)
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                val window = game.state.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
                withClue("the opponent pays, and the prompt names Valkmira rather than ward") {
                    window.playerId shouldBe game.player2Id
                    window.context.sourceName shouldBe "Valkmira, Protector's Shield"
                    window.prompt shouldBe "Pay {1} or your spell will be countered"
                }
                game.submitDecision(ManaSourcesSelectedResponse(window.id, declined = true)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }

            test("paid, the spell resolves and Valkmira prevents 1 of its damage to you") {
                val game = valkmiraBoard(opponentMountains = 2)
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                val window = game.state.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitDecision(ManaSourcesSelectedResponse(window.id, autoPay = true)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
            }

            test("another permanent you control triggers it too") {
                val game = valkmiraBoard(opponentMountains = 1)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("no mana left to pay {1}, so the Bolt is countered without a prompt") {
                    game.state.pendingDecision shouldBe null
                }
                game.findPermanent("Grizzly Bears") shouldBe bears
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            }

            test("Valkmira itself being targeted doesn't trigger it — 'another permanent'") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Valkmira, Protector's Shield")
                    .withCardInHand(2, "Shatter")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(2, "Shatter", game.findPermanent("Valkmira, Protector's Shield")!!).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.findPermanent("Valkmira, Protector's Shield") shouldBe null
            }

            test("an opponent targeting their own creature doesn't trigger it") {
                val game = valkmiraBoard(opponentMountains = 1)
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(2, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                withClue("Bolt resolved: 3 damage to a 3/3, nothing prevented on the opponent's side") {
                    game.findPermanent("Hill Giant") shouldBe null
                }
            }

            test("your own spell targeting your creature doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Valkmira, Protector's Shield")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }

        context("Reidane, God of the Worthy") {
            fun reidaneBoard(opponentLands: Int): TestGame = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Reidane, God of the Worthy")
                .withCardInHand(2, bigSorcery.name)
                .withCardInHand(2, smallSorcery.name)
                .withCardInHand(2, "Snow-Covered Mountain")
                .withLandsOnBattlefield(2, "Mountain", opponentLands)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.castFromOpponentHand(name: String) =
                execute(CastSpell(player2Id, findCardsInHand(2, name).single()))

            test("opponents' noncreature spells with mana value 4 or greater cost {2} more") {
                val four = reidaneBoard(opponentLands = 4)
                withClue("{4} is now {6}") {
                    four.castFromOpponentHand(bigSorcery.name).error shouldNotBe null
                }
                withClue("mana value 3 is untaxed") {
                    four.castFromOpponentHand(smallSorcery.name).error shouldBe null
                }

                val six = reidaneBoard(opponentLands = 6)
                six.castFromOpponentHand(bigSorcery.name).error shouldBe null
            }

            test("snow lands your opponents control enter tapped") {
                val game = reidaneBoard(opponentLands = 0)
                val snow = game.findCardsInHand(2, "Snow-Covered Mountain").single()
                game.execute(PlayLand(game.player2Id, snow)).error shouldBe null

                game.state.getEntity(snow)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
