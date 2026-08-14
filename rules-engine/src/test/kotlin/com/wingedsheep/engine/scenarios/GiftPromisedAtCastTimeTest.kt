package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gift on a **permanent** spell is promised as an additional cost while casting
 * (CR 702.174a — "as an additional cost to cast this spell, you may choose an opponent"), and the
 * gift itself is a "when this permanent enters, if its gift cost was paid, [effect]" trigger
 * (CR 702.174b).
 *
 * Regression guard for the bug these tests were written for: Kitnap / Scrapshooter / Starforged
 * Sword used to model the promise as a *resolution-time* modal choice on their enters-the-battlefield
 * trigger, so the player was asked whether to give a gift **after** the permanent had already
 * entered — long after the only legal moment to decide.
 */
class GiftPromisedAtCastTimeTest : ScenarioTestBase() {

    /** The card id of [cardName] in the given player's hand. */
    private fun TestGame.handCard(playerNumber: Int, cardName: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getZone(playerId, Zone.HAND).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == cardName
        }
    }

    private fun TestGame.stunCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    init {
        context("gift is elected while casting, never after the permanent enters") {

            test("both a plain cast and a promise-a-gift cast are offered as legal actions") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scrapshooter")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val actions = game.getLegalActions(1)
                    .filter { (it.action as? CastSpell)?.cardId == game.handCard(1, "Scrapshooter") }

                withClue("gift is optional (CR 702.174a) — the unpromised cast must stay available") {
                    actions.count { it.actionType == "CastSpell" } shouldBe 1
                }
                withClue("the promise must be offered as a cast option, naming what is gifted") {
                    val giftAction = actions.single { it.actionType == "CastWithGift" }
                    giftAction.description shouldBe "Cast Scrapshooter (Gift a card)"
                    (giftAction.action as CastSpell).giftRecipient shouldBe game.player2Id
                }
            }

            test("the promise rides a free cast too — an additional cost outlives the mana cost") {
                // CR 601.2b / 601.2f–h: additional costs are chosen and paid whichever cost pays the
                // mana cost. With Omniscience out and no Forests, the free cast is the only payable
                // path, so it must be the one carrying the gift offer.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Omniscience")
                    .withCardInHand(1, "Scrapshooter")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scrapshooter = game.handCard(1, "Scrapshooter")
                val giftActions = game.getLegalActions(1)
                    .filter { it.actionType == "CastWithGift" && (it.action as? CastSpell)?.cardId == scrapshooter }

                val freeGift = withClue("the free cast needs its own gift twin") {
                    giftActions.single { (it.action as CastSpell).useWithoutPayingManaCost }
                }
                withClue("an unaffordable cast must not spawn a greyed-out gift twin per opponent") {
                    giftActions.none { !(it.action as CastSpell).useWithoutPayingManaCost } shouldBe true
                }

                game.execute(freeGift.action).error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.findPermanent("Sol Ring").shouldNotBeNull()))
                game.resolveStack()

                withClue("promised on a free cast, the gift and its rider both happen") {
                    game.handSize(2) shouldBe 1
                    game.isOnBattlefield("Sol Ring") shouldBe false
                }
            }

            test("Kitnap with the gift promised: opponent draws, no stun counters, nothing asked at ETB") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Kitnap")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val handBefore = game.handSize(2)

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Kitnap"),
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        giftRecipient = game.player2Id
                    )
                )
                withClue("casting with a promised gift should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("the gift must not be a question asked after the Aura entered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("gift a card (CR 702.174e) — the promised opponent draws") {
                    game.handSize(2) shouldBe handBefore + 1
                }
                withClue("the printed rider only applies when the gift wasn't promised") {
                    game.stunCounters(bears) shouldBe 0
                }
                withClue("the Aura's own effect still happens either way") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("Kitnap without the gift: three stun counters, no card given") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Kitnap")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val handBefore = game.handSize(2)

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Kitnap"),
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("no promise, no gift trigger (CR 702.174b intervening if)") {
                    game.handSize(2) shouldBe handBefore
                    game.hasPendingDecision() shouldBe false
                }
                game.stunCounters(bears) shouldBe 3
            }

            test("Scrapshooter's destroy trigger only exists when the gift was promised") {
                fun play(promise: Boolean): TestGame {
                    val game = scenario()
                        .withPlayers("Caster", "Opponent")
                        .withCardInHand(1, "Scrapshooter")
                        .withLandsOnBattlefield(1, "Forest", 3)
                        .withCardOnBattlefield(2, "Sol Ring")
                        .withCardInLibrary(2, "Forest")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    game.execute(
                        CastSpell(
                            playerId = game.player1Id,
                            cardId = game.handCard(1, "Scrapshooter"),
                            giftRecipient = if (promise) game.player2Id else null
                        )
                    ).error shouldBe null
                    game.resolveStack()
                    // The promised cast's destroy trigger targets when it goes on the stack
                    // (CR 603.3d) — pick the only legal artifact, then finish resolving.
                    if (promise) {
                        game.getPendingDecision().shouldNotBeNull()
                        game.selectTargets(listOf(game.findPermanent("Sol Ring").shouldNotBeNull()))
                        game.resolveStack()
                    }
                    return game
                }

                val unpromised = play(promise = false)
                withClue("an unpromised Scrapshooter never triggers, so it never targets") {
                    unpromised.hasPendingDecision() shouldBe false
                    unpromised.isOnBattlefield("Sol Ring") shouldBe true
                }

                val promised = play(promise = true)
                withClue("with the gift promised the artifact is destroyed and the opponent draws") {
                    promised.isOnBattlefield("Sol Ring") shouldBe false
                    promised.isInGraveyard(2, "Sol Ring") shouldBe true
                    promised.handSize(2) shouldBe 1
                }
            }

            test("Starforged Sword with the gift promised: tapped Fish for the opponent, sword attached") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Starforged Sword")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions").shouldNotBeNull()
                val powerBefore = game.state.projectedState.getPower(lions)

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Starforged Sword"),
                        giftRecipient = game.player2Id
                    )
                ).error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(lions))
                game.resolveStack()

                withClue("gift a tapped Fish (CR 702.174f) goes to the promised opponent") {
                    val fish = game.findPermanent("Fish Token").shouldNotBeNull()
                    game.state.projectedState.getController(fish) shouldBe game.player2Id
                    game.state.getEntity(fish)?.has<TappedComponent>() shouldBe true
                }
                withClue("the printed attach trigger fires off the same promise: sword attaches, +3/+3") {
                    val sword = game.findPermanent("Starforged Sword").shouldNotBeNull()
                    game.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe lions
                    game.state.projectedState.getPower(lions) shouldBe (powerBefore ?: 0) + 3
                }
            }

            test("giving a gift with a permanent fires \"whenever you give a gift\" (CR 702.174c)") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scrapshooter")
                    .withCardOnBattlefield(1, "Jolly Gerbils")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val casterHandBefore = game.handSize(1) - 1 // Scrapshooter leaves the hand

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Scrapshooter"),
                        giftRecipient = game.player2Id
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the gift ability resolving is what gives the gift, so Gerbils draws") {
                    game.handSize(1) shouldBe casterHandBefore + 1
                }
            }

            test("a countered gift spell gives no gift") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scrapshooter")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(2, "Dismiss")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(2)

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Scrapshooter"),
                        giftRecipient = game.player2Id
                    )
                ).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Dismiss", "Scrapshooter").error shouldBe null
                game.resolveStack()

                withClue("Scrapshooter never entered, so its gift trigger never fired") {
                    game.isInGraveyard(1, "Scrapshooter") shouldBe true
                    // Dismiss drew its own card, so the opponent's hand is: before - Dismiss + draw.
                    game.handSize(2) shouldBe handBefore
                }
            }

            test("multiplayer: one cast variant per opponent, and only that opponent gets the gift") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + PredefinedTokens.allTokens)
                val players = driver.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

                val caster = players[0]
                val secondOpponent = players[2]
                driver.giveMana(caster, Color.GREEN, 3)
                val scrapshooter = driver.putCardInHand(caster, "Scrapshooter")

                val giftActions = driver.legalActions(caster)
                    .filter { it.actionType == "CastWithGift" }
                withClue("the opponent is chosen as part of the gift cost (CR 702.174a)") {
                    giftActions.map { (it.action as CastSpell).giftRecipient } shouldBe listOf(players[1], players[2])
                    giftActions.map { it.description } shouldBe listOf(
                        "Cast Scrapshooter (Gift a card to Player 2)",
                        "Cast Scrapshooter (Gift a card to Player 3)"
                    )
                }

                val handsBefore = players.associateWith { driver.state.getZone(it, Zone.HAND).size }
                driver.submitSuccess(CastSpell(caster, scrapshooter, giftRecipient = secondOpponent))
                var passes = 0
                while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && passes++ < 30) {
                    driver.passPriority(driver.priorityPlayer!!)
                }

                withClue("only the promised opponent draws") {
                    driver.state.getZone(secondOpponent, Zone.HAND).size shouldBe handsBefore.getValue(secondOpponent) + 1
                    driver.state.getZone(players[1], Zone.HAND).size shouldBe handsBefore.getValue(players[1])
                }
            }

            test("a gift may only be promised to an opponent, and only by a card with gift") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scrapshooter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val toSelf = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Scrapshooter"),
                        giftRecipient = game.player1Id
                    )
                )
                toSelf.error shouldNotBe null

                val noGiftCard = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.handCard(1, "Grizzly Bears"),
                        giftRecipient = game.player2Id
                    )
                )
                noGiftCard.error shouldNotBe null
            }
        }
    }
}
