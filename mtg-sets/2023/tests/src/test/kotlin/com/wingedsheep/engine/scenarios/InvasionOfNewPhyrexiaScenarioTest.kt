package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of New Phyrexia // Teferi Akosa of Zhalfir (MOM #239).
 *
 * Front: "When this Siege enters, create X 2/2 white and blue Knight creature tokens with vigilance."
 * Back (loyalty 4):
 *   +1: Draw two cards. Then discard two cards unless you discard a creature card.
 *   −2: You get an emblem with "Knights you control get +1/+0 and have ward {1}."
 *   −3: Tap any number of untapped creatures you control. When you do, shuffle target nonland
 *       permanent an opponent controls with mana value X or less into its owner's library, where X
 *       is the number of creatures tapped this way.
 */
class InvasionOfNewPhyrexiaScenarioTest : ScenarioTestBase() {

    init {
        val teferiAbilities = cardRegistry.getCard("Teferi Akosa of Zhalfir")!!.script.activatedAbilities
        fun idFor(change: Int) =
            teferiAbilities.single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

        fun TestGame.loyalty(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        fun TestGame.activate(change: Int) {
            val teferi = findPermanent("Teferi Akosa of Zhalfir")!!
            execute(
                ActivateAbility(playerId = player1Id, sourceId = teferi, abilityId = idFor(change))
            ).error shouldBe null
            resolveStack()
        }

        fun teferiBoard() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Teferi Akosa of Zhalfir")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("front face — Invasion of New Phyrexia") {
            test("the enters trigger creates X 2/2 white and blue Knight tokens with vigilance") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of New Phyrexia")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Invasion of New Phyrexia", 2).error shouldBe null
                game.resolveStack()

                val knights = game.state.getBattlefield().filter {
                    game.state.getEntity(it)?.has<TokenComponent>() == true
                }
                withClue("X = 2 made two Knight tokens") { knights.size shouldBe 2 }
                val projected = game.state.projectedState
                knights.forEach { knight ->
                    projected.getPower(knight) shouldBe 2
                    projected.getToughness(knight) shouldBe 2
                    projected.hasSubtype(knight, "Knight") shouldBe true
                    projected.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
                    projected.getColors(knight) shouldBe setOf(Color.WHITE.name, Color.BLUE.name)
                }
                game.findPermanent("Invasion of New Phyrexia") shouldBe
                    game.findPermanents("Invasion of New Phyrexia").single()
            }
        }

        context("back face — Teferi Akosa of Zhalfir") {
            test("+1 draws two, then discarding a single creature card satisfies the discard") {
                val game = teferiBoard()
                    .withCardInHand(1, "Grizzly Bears")
                    .build()
                val teferi = game.findPermanent("Teferi Akosa of Zhalfir")!!

                game.activate(+1)
                withClue("drew two: hand is Bears + two Plains") { game.handSize(1) shouldBe 3 }

                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("only the creature card was discarded") { game.handSize(1) shouldBe 2 }
                game.loyalty(teferi) shouldBe 5
            }

            test("−2 emblem: Knights you control get +1/+0 and ward {1}; a non-Knight doesn't") {
                val game = teferiBoard()
                    .withCardOnBattlefield(1, "Knight Errant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .build()
                val teferi = game.findPermanent("Teferi Akosa of Zhalfir")!!
                val knight = game.findPermanent("Knight Errant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.activate(-2)
                game.loyalty(teferi) shouldBe 2

                val projected = game.state.projectedState
                withClue("the Knight gets +1/+0 and ward") {
                    projected.getPower(knight) shouldBe 3
                    projected.getToughness(knight) shouldBe 2
                    projected.hasKeyword(knight, Keyword.WARD) shouldBe true
                }
                withClue("the Bear is not a Knight") {
                    projected.getPower(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.WARD) shouldBe false
                }

                // Hand priority to the opponent, who Bolts the Knight with their only land tapped
                // for it — nothing left to pay ward {1}.
                game.passPriority().error shouldBe null
                game.state.priorityPlayerId shouldBe game.player2Id
                game.castSpell(2, "Lightning Bolt", knight).error shouldBe null
                withClue("ward triggered on top of the Bolt") { game.state.stack.size shouldBe 2 }
                game.resolveStack()

                withClue("the Bolt was countered — the Knight survives") {
                    game.isOnBattlefield("Knight Errant") shouldBe true
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                }
            }

            test("−2 emblem: an opponent's spell targeting a non-Knight is not warded") {
                val game = teferiBoard()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.activate(-2)
                game.passPriority().error shouldBe null
                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                withClue("no ward trigger") { game.state.stack.size shouldBe 1 }
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("−3 taps the chosen creatures; X is the number tapped and caps the target's mana value") {
                val game = teferiBoard()
                    .withCardOnBattlefield(1, "Knight Errant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")     // MV 4 — too big for X = 2
                    .withCardOnBattlefield(2, "Glorious Anthem") // MV 3 — too big for X = 2
                    .withCardOnBattlefield(2, "Llanowar Elves")  // MV 1 — legal
                    .build()
                val teferi = game.findPermanent("Teferi Akosa of Zhalfir")!!
                val knight = game.findPermanent("Knight Errant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.activate(-3)
                game.loyalty(teferi) shouldBe 1
                game.selectCards(listOf(knight, bears)).error shouldBe null

                withClue("both chosen creatures are tapped") {
                    game.state.getEntity(knight)!!.has<TappedComponent>() shouldBe true
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }

                withClue("the reflexive trigger can't target a mana value above X") {
                    (game.selectTargets(listOf(giant)).error != null) shouldBe true
                }
                game.selectTargets(listOf(elves)).error shouldBe null
                game.resolveStack()

                withClue("Llanowar Elves was shuffled into its owner's library") {
                    game.isOnBattlefield("Llanowar Elves") shouldBe false
                    game.findCardsInLibrary(2, "Llanowar Elves").size shouldBe 1
                }
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isOnBattlefield("Glorious Anthem") shouldBe true
            }

            test("−3 tapping no creatures makes X 0 — a mana-value-1 permanent can't be targeted") {
                val game = teferiBoard()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .build()

                game.activate(-3)
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.findPermanent("Grizzly Bears")!!)!!.has<TappedComponent>() shouldBe false
                game.isOnBattlefield("Llanowar Elves") shouldBe true
            }
        }
    }
}
