package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Jackal, Genius Geneticist.
 *
 * Card reference (Scryfall oracle):
 * - Jackal, Genius Geneticist ({G}{U}): Legendary Creature — Human Scientist Villain, 1/1
 *   Trample
 *   Whenever you cast a creature spell with mana value equal to Jackal's power, copy that
 *   spell, except the copy isn't legendary. Then put a +1/+1 counter on Jackal.
 *   (The copy becomes a token.)
 */
class JackalGeniusGeneticistScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        // 1-MV legendary creature used to exercise the "except the copy isn't legendary"
        // clause. Registered here because `ScenarioTestBase` only loads cards from registered
        // sets, and we want a guaranteed 1-MV legendary that matches Jackal's printed power.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Legendary Elf",
                manaCost = ManaCost.parse("{G}"),
                subtypes = emptySet(),
                power = 1,
                toughness = 1,
                supertypes = setOf(Supertype.LEGENDARY)
            )
        )

        context("Jackal, Genius Geneticist — card definition") {

            test("casts for {G}{U} and enters as a legendary green-blue 1/1 creature with trample") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Jackal, Genius Geneticist")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Jackal, Genius Geneticist")
                withClue("Casting Jackal, Genius Geneticist should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Jackal, Genius Geneticist should be on the battlefield") {
                    game.isOnBattlefield("Jackal, Genius Geneticist") shouldBe true
                }

                val jackalId = game.findPermanent("Jackal, Genius Geneticist")
                jackalId shouldNotBe null
                val jackal = jackalId!!

                val projected = stateProjector.project(game.state)

                withClue("Jackal should be legendary") {
                    projected.isLegendary(jackal) shouldBe true
                }
                withClue("Jackal should be a creature") {
                    projected.isCreature(jackal) shouldBe true
                }
                withClue("Jackal should have power 1") {
                    projected.getPower(jackal) shouldBe 1
                }
                withClue("Jackal should have toughness 1") {
                    projected.getToughness(jackal) shouldBe 1
                }
                withClue("Jackal should have trample") {
                    projected.hasKeyword(jackal, Keyword.TRAMPLE) shouldBe true
                }
                withClue("Jackal should be green and blue") {
                    projected.getColors(jackal) shouldBe setOf("GREEN", "BLUE")
                }
                withClue("Jackal should have Human subtype") {
                    projected.getSubtypes(jackal).contains("Human") shouldBe true
                }
                withClue("Jackal should have Scientist subtype") {
                    projected.getSubtypes(jackal).contains("Scientist") shouldBe true
                }
                withClue("Jackal should have Villain subtype") {
                    projected.getSubtypes(jackal).contains("Villain") shouldBe true
                }
            }
        }

        context("Jackal, Genius Geneticist — triggered ability") {

            test("matching mana value creates a token copy and puts a +1/+1 counter on Jackal") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Jackal, Genius Geneticist")
                    .withCardInHand(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Llanowar Elves")
                withClue("Casting Llanowar Elves should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val elves = game.findAllPermanents("Llanowar Elves")
                withClue("Both the original and the token copy of Llanowar Elves should be on the battlefield") {
                    elves.size shouldBe 2
                }

                val tokenCount = elves.count { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() == true
                }
                withClue("Exactly one Llanowar Elves on the battlefield should be a token (the copy)") {
                    tokenCount shouldBe 1
                }

                val jackal = game.findPermanent("Jackal, Genius Geneticist")!!
                val counters = game.state.getEntity(jackal)?.get<CountersComponent>()
                withClue("Jackal should have one +1/+1 counter from its own trigger") {
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }

                val projected = stateProjector.project(game.state)
                withClue("Jackal's power should be 2 after gaining a +1/+1 counter") {
                    projected.getPower(jackal) shouldBe 2
                }
            }

            test("the token copy of a legendary creature spell is not legendary (CR 707.10f)") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Jackal, Genius Geneticist")
                    .withCardInHand(1, "Test Legendary Elf")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Test Legendary Elf")
                withClue("Casting Test Legendary Elf should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val elves = game.findAllPermanents("Test Legendary Elf")
                withClue("Both the original and the token copy should survive — the copy isn't legendary so the legend rule doesn't kill one") {
                    elves.size shouldBe 2
                }

                val tokenId = elves.first { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() == true
                }
                val originalId = elves.first { id ->
                    game.state.getEntity(id)?.has<TokenComponent>() == false
                }

                val projected = stateProjector.project(game.state)
                withClue("Original Test Legendary Elf is still legendary") {
                    projected.isLegendary(originalId) shouldBe true
                }
                withClue("Token copy must not be legendary (oracle: \"except the copy isn't legendary\")") {
                    projected.isLegendary(tokenId) shouldBe false
                }
            }

            test("non-matching mana value does not trigger the ability") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Jackal, Genius Geneticist")
                    .withCardInHand(1, "Angry Rabble")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Angry Rabble")
                withClue("Casting Angry Rabble should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Angry Rabble (MV 2) does not match Jackal's power (1), so only the original is on the battlefield") {
                    game.findAllPermanents("Angry Rabble").size shouldBe 1
                }

                val jackal = game.findPermanent("Jackal, Genius Geneticist")!!
                val counters = game.state.getEntity(jackal)?.get<CountersComponent>()
                withClue("Jackal should not have any +1/+1 counters") {
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                }
            }
        }
    }
}
