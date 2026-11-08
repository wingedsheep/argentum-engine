package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arb.cards.ViolentOutburst
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CascadeMayPutOntoBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [CascadeMayPutOntoBattlefield] — "As you cascade, you may put a land card from among the exiled
 * cards onto the battlefield tapped. (Do this after the last card is exiled but before deciding
 * whether to cast a nonland card.)" (Averna, the Chaos Bloom) — an "as you cascade" action (CR 702.85b).
 *
 * Pins the rulings: the choice comes after the walk and before the cast offer; it is offered even
 * when the walk finds nothing to cast; it isn't a land play (no land drop needed, none used, any
 * turn); only the controller's own cascades get it; each instance is its own choice.
 */
class CascadeMayPutOntoBattlefieldTest : ScenarioTestBase() {

    private fun TestGame.name(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name
    private fun TestGame.libraryNames(player: Int) =
        state.getLibrary(if (player == 1) player1Id else player2Id).mapNotNull { name(it) }
    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    /** Quandrix (MV 6, cascade) cast with Averna out; the walk exiles Mountain, Swamp, then hits Grizzly Bears. */
    private fun cascadeWithAverna(averna: Int = 1, library: List<String> = listOf("Mountain", "Swamp", "Grizzly Bears", "Plains")): TestGame {
        val builder = scenario()
            .withPlayers()
            .withCardInHand(1, "Quandrix, the Proof")
            .withLandsOnBattlefield(1, "Forest", 5)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(averna, "Averna, the Chaos Bloom")
        library.forEach { builder.withCardInLibrary(1, it) }
        return builder.build()
    }

    init {
        context("as you cascade, you may put a land card from among the exiled cards onto the battlefield") {

            test("after the last card is exiled and before the cast offer, a chosen land enters tapped") {
                val game = cascadeWithAverna()
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Only the exiled lands are offered — not the cascade hit, not the unexiled Plains") {
                    choice.options.map { game.name(it) } shouldContainExactlyInAnyOrder listOf("Mountain", "Swamp")
                }
                choice.minSelections shouldBe 0
                choice.maxSelections shouldBe 1

                val mountain = choice.options.single { game.name(it) == "Mountain" }
                game.selectCards(listOf(mountain))

                withClue("It entered tapped under Averna's controller before the cast decision") {
                    (mountain in game.state.getBattlefield()) shouldBe true
                    game.tapped(mountain) shouldBe true
                    game.state.projectedState.getController(mountain) shouldBe game.player1Id
                }
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("The uncast hit and the other land go to the bottom; the chosen land doesn't") {
                    game.libraryNames(1).first() shouldBe "Plains"
                    game.libraryNames(1).drop(1) shouldContainExactlyInAnyOrder listOf("Swamp", "Grizzly Bears")
                    game.state.getExile(game.player1Id).isEmpty() shouldBe true
                }
            }

            test("declining keeps every exiled land for the bottom, and the hit can still be cast") {
                val game = cascadeWithAverna()
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Mountain") shouldBe false
                game.libraryNames(1).drop(1) shouldContainExactlyInAnyOrder listOf("Mountain", "Swamp")
            }

            test("offered even when the cascade finds no card to cast") {
                val game = cascadeWithAverna(library = listOf("Mountain", "Swamp"))
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val swamp = choice.options.single { game.name(it) == "Swamp" }
                game.selectCards(listOf(swamp))

                withClue("No cast is offered; the rest go to the bottom") {
                    game.getPendingDecision() shouldBe null
                }
                game.resolveStack()
                (swamp in game.state.getBattlefield()) shouldBe true
                game.tapped(swamp) shouldBe true
                game.libraryNames(1) shouldBe listOf("Mountain")
            }

            test("it isn't a land play: it works with no land play left, and uses none") {
                val game = cascadeWithAverna()
                game.state = game.state.updateEntity(game.player1Id) {
                    it.with(LandDropsComponent(remaining = 0, maxPerTurn = 1, playedThisTurn = 1))
                }
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val mountain = choice.options.single { game.name(it) == "Mountain" }
                game.selectCards(listOf(mountain))

                (mountain in game.state.getBattlefield()) shouldBe true
                game.state.getEntity(game.player1Id)?.get<LandDropsComponent>() shouldBe
                    LandDropsComponent(remaining = 0, maxPerTurn = 1, playedThisTurn = 1)
            }

            test("an opponent's Averna doesn't ride your cascade") {
                val game = cascadeWithAverna(averna = 2)
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            }

            test("a cast prohibition still leaves the land choice; then everything left is bottomed, no cast offered") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Quandrix, the Proof")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Averna, the Chaos Bloom")
                    .withCardOnBattlefield(2, "Drannith Magistrate")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .build()
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val mountain = choice.options.single()
                game.selectCards(listOf(mountain))

                withClue("The Magistrate forbids the cast, so no offer follows") {
                    game.getPendingDecision() shouldBe null
                }
                game.resolveStack()
                (mountain in game.state.getBattlefield()) shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.libraryNames(1) shouldBe listOf("Grizzly Bears")
            }

            test("no exiled land, no prompt") {
                val game = cascadeWithAverna(library = listOf("Grizzly Bears", "Mountain"))
                game.castSpell(1, "Quandrix, the Proof").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            }
        }

        context("several instances, on an instant cascaded on the opponent's turn") {
            test("each instance is its own choice of one land, untapped when the text doesn't say tapped") {
                val gardener = card("Test Cascade Gardener") {
                    manaCost = "{2}{G}"
                    typeLine = "Creature — Elf Druid"
                    power = 1
                    toughness = 1
                    staticAbility { ability = CascadeMayPutOntoBattlefield(tapped = false) }
                }
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + ViolentOutburst + gardener)
                driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                val active = driver.activePlayer!!
                val me = driver.getOpponent(active)
                driver.putCreatureOnBattlefield(me, "Test Cascade Gardener")
                driver.putCreatureOnBattlefield(me, "Test Cascade Gardener")

                // Top of library: Forest, Mountain, then Grizzly Bears (MV 2 < 3) as the hit.
                driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
                driver.putCardOnTopOfLibrary(me, "Mountain")
                driver.putCardOnTopOfLibrary(me, "Forest")

                val outburst = driver.putCardInHand(me, "Violent Outburst")
                driver.giveMana(me, Color.RED, 1)
                driver.giveMana(me, Color.GREEN, 1)
                driver.giveColorlessMana(me, 1)
                driver.passPriority(active)
                driver.castSpell(me, outburst).outcome shouldBe Outcome.Done
                driver.bothPass()

                val first = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                first.options.size shouldBe 2
                val forest = first.options.single { driver.getCardName(it) == "Forest" }
                driver.submitCardSelection(me, listOf(forest))

                val second = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The second instance offers what's left") {
                    second.options.map { driver.getCardName(it) } shouldBe listOf("Mountain")
                }
                driver.submitCardSelection(me, second.options)

                driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                val battlefield = driver.state.getBattlefield(me).map { driver.getCardName(it) }
                battlefield.count { it == "Forest" } shouldBe 1
                battlefield.count { it == "Mountain" } shouldBe 1
                driver.state.getBattlefield(me)
                    .filter { driver.getCardName(it) in setOf("Forest", "Mountain") }
                    .none { driver.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
            }

            test("a rider that takes the hit itself leaves nothing to cast") {
                val anything = card("Test Cascade Planter") {
                    manaCost = "{2}{G}"
                    typeLine = "Creature — Elf Druid"
                    power = 1
                    toughness = 1
                    staticAbility { ability = CascadeMayPutOntoBattlefield(filter = GameObjectFilter.Any, tapped = false) }
                }
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + ViolentOutburst + anything)
                driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                val me = driver.activePlayer!!
                driver.putCreatureOnBattlefield(me, "Test Cascade Planter")
                driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
                driver.putCardOnTopOfLibrary(me, "Forest")

                val outburst = driver.putCardInHand(me, "Violent Outburst")
                driver.giveMana(me, Color.RED, 1)
                driver.giveMana(me, Color.GREEN, 1)
                driver.giveColorlessMana(me, 1)
                driver.castSpell(me, outburst).outcome shouldBe Outcome.Done
                driver.bothPass()

                val choice = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = choice.options.single { driver.getCardName(it) == "Grizzly Bears" }
                driver.submitCardSelection(me, listOf(bears))

                withClue("The hit left exile onto the battlefield, so no cast is offered") {
                    driver.pendingDecision shouldBe null
                }
                (bears in driver.state.getBattlefield(me)) shouldBe true
                driver.state.getExile(me).none { driver.getCardName(it) == "Forest" } shouldBe true
            }
        }
    }
}
