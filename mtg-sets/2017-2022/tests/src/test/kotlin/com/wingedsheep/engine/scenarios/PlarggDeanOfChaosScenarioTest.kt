package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.stx.cards.PlarggDeanOfChaos
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Plargg, Dean of Chaos // Augusta, Dean of Order (STX #155) — a modal DFC with two creature faces.
 *
 * Plargg's "{4}{R}, {T}" walks the library by *revealing* (the cards stay in the library), stops on
 * the first nonlegendary nonland card with mana value 3 or less, offers a free cast, and bottoms
 * every revealed card not cast — including a declined hit. Augusta's statics split on tapped /
 * untapped, and her attack trigger untaps everything before you choose what to tap again.
 */
class PlarggDeanOfChaosScenarioTest : ScenarioTestBase() {

    private val cheapBear = card("Plargg Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val cheapLegend = card("Plargg Test Legend") {
        manaCost = "{1}{G}"
        typeLine = "Legendary Creature — Elf"
        power = 2
        toughness = 2
    }
    private val bigGiant = card("Plargg Test Giant") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 4
    }
    private val vanillaA = card("Augusta Test Soldier A") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Soldier"
        power = 2
        toughness = 2
    }
    private val vanillaB = card("Augusta Test Soldier B") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Soldier"
        power = 2
        toughness = 2
    }

    init {
        listOf(cheapBear, cheapLegend, bigGiant, vanillaA, vanillaB).forEach { cardRegistry.register(it) }

        fun plarggBoard() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Plargg, Dean of Chaos")
            .withLandsOnBattlefield(1, "Mountain", 5)
            // Top → bottom: a land, a cheap legend, an expensive creature, the hit, then two unrevealed.
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Plargg Test Legend")
            .withCardInLibrary(1, "Plargg Test Giant")
            .withCardInLibrary(1, "Plargg Test Bear")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.libraryNames(): List<String> =
            state.getLibrary(player1Id).map { state.getEntity(it)!!.get<CardComponent>()!!.name }

        fun TestGame.activateReveal() {
            val plargg = findPermanent("Plargg, Dean of Chaos")!!
            val abilityId = PlarggDeanOfChaos.activatedAbilities[1].id
            execute(ActivateAbility(player1Id, plargg, abilityId)).error shouldBe null
            resolveStack()
        }

        context("Plargg — reveal until a cheap nonlegendary nonland card") {

            test("casts the hit for free and bottoms the rest, leaving the unrevealed cards on top") {
                val game = plarggBoard()
                game.activateReveal()

                withClue("the walk passes the land, the legend and the MV-4 creature and offers the bear") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the bear was cast without paying its mana cost") {
                    game.isOnBattlefield("Plargg Test Bear") shouldBe true
                }
                val library = game.libraryNames()
                withClue("unrevealed cards stay on top; the three passed-over cards go to the bottom") {
                    library.take(2) shouldBe listOf("Forest", "Forest")
                    library.drop(2) shouldContainExactlyInAnyOrder listOf(
                        "Island", "Plargg Test Legend", "Plargg Test Giant",
                    )
                }
            }

            test("a declined hit goes to the bottom with the other revealed cards") {
                val game = plarggBoard()
                game.activateReveal()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("nothing was cast") {
                    game.isOnBattlefield("Plargg Test Bear") shouldBe false
                }
                val library = game.libraryNames()
                library.take(2) shouldBe listOf("Forest", "Forest")
                library.drop(2) shouldContainExactlyInAnyOrder listOf(
                    "Island", "Plargg Test Legend", "Plargg Test Giant", "Plargg Test Bear",
                )
            }
        }

        context("Augusta, Dean of Order") {

            fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

            test("other tapped creatures get +1/+0 and other untapped creatures get +0/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Augusta, Dean of Order", tapped = true)
                    .withCardOnBattlefield(1, "Augusta Test Soldier A", tapped = true)
                    .withCardOnBattlefield(1, "Augusta Test Soldier B")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val augusta = game.findPermanent("Augusta, Dean of Order")!!
                val tapped = game.findPermanent("Augusta Test Soldier A")!!
                val untapped = game.findPermanent("Augusta Test Soldier B")!!
                val projected = game.state.projectedState

                withClue("tapped soldier is 3/2") {
                    projected.getPower(tapped) shouldBe 3
                    projected.getToughness(tapped) shouldBe 2
                }
                withClue("untapped soldier is 2/3") {
                    projected.getPower(untapped) shouldBe 2
                    projected.getToughness(untapped) shouldBe 3
                }
                withClue("Augusta doesn't buff herself") {
                    projected.getPower(augusta) shouldBe 1
                    projected.getToughness(augusta) shouldBe 3
                }
            }

            test("whenever you attack, untap every creature you control, then tap the chosen ones") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Augusta, Dean of Order")
                    .withCardOnBattlefield(1, "Augusta Test Soldier A")
                    .withCardOnBattlefield(1, "Augusta Test Soldier B")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val soldierA = game.findPermanent("Augusta Test Soldier A")!!
                val soldierB = game.findPermanent("Augusta Test Soldier B")!!
                val augusta = game.findPermanent("Augusta, Dean of Order")!!

                // Augusta stays home: the trigger is "whenever you attack", not "whenever Augusta attacks".
                game.declareAttackers(
                    mapOf("Augusta Test Soldier A" to 2, "Augusta Test Soldier B" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("the trigger asks which creatures to tap") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(soldierA))
                game.resolveStack()

                withClue("only the chosen attacker is tapped again") {
                    game.isTapped(soldierA) shouldBe true
                    game.isTapped(soldierB) shouldBe false
                    game.isTapped(augusta) shouldBe false
                }
                withClue("untapping an attacker doesn't remove it from combat") {
                    game.state.getEntity(soldierB)!!.has<AttackingComponent>() shouldBe true
                    game.state.getEntity(soldierA)!!.has<AttackingComponent>() shouldBe true
                }
                withClue("the tapped attacker gets +1/+0, the untapped one +0/+1") {
                    game.state.projectedState.getPower(soldierA) shouldBe 3
                    game.state.projectedState.getToughness(soldierB) shouldBe 3
                }
            }
        }
    }
}
