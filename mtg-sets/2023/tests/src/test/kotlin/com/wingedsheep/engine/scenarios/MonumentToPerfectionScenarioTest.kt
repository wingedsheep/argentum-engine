package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Monument to Perfection (ONE #233, {2}, Artifact).
 *
 *   {3}, {T}: Search your library for a basic, Sphere, or Locus land card, reveal it, put it
 *   into your hand, then shuffle.
 *   {3}: This artifact becomes a 9/9 Phyrexian Construct artifact creature, loses all abilities,
 *   and gains indestructible and toxic 9. Activate only if there are nine or more lands with
 *   different names among the basic, Sphere, and Locus lands you control.
 */
class MonumentToPerfectionScenarioTest : ScenarioTestBase() {

    private val searchAbility get() = cardRegistry.getCard("Monument to Perfection")!!.script.activatedAbilities[0]
    private val animateAbility get() = cardRegistry.getCard("Monument to Perfection")!!.script.activatedAbilities[1]

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    /** Nine differently named basic/Locus lands: five basics, three snow basics, and Cloudpost (a Locus). */
    private fun ScenarioBuilder.withNineDistinctLands(): ScenarioBuilder = this
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Snow-Covered Plains", 1)
        .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
        .withLandsOnBattlefield(1, "Snow-Covered Swamp", 1)
        .withLandsOnBattlefield(1, "Cloudpost", 1)

    init {
        test("{3}, {T} finds a basic, Sphere or Locus land but not other lands") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Monument to Perfection")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Cloudpost")
                .withCardInLibrary(1, "The Dross Pits")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val monument = game.findPermanent("Monument to Perfection")!!
            val forest = game.findCardsInLibrary(1, "Forest").single()
            val cloudpost = game.findCardsInLibrary(1, "Cloudpost").single()
            val drossPits = game.findCardsInLibrary(1, "The Dross Pits").single()

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = monument, abilityId = searchAbility.id)
            ).error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("basic Forest, Sphere Dross Pits and Locus Cloudpost are searchable; Grizzly Bears isn't") {
                decision.options shouldContainExactlyInAnyOrder listOf(forest, drossPits, cloudpost)
            }
            game.selectCards(listOf(cloudpost))
            game.resolveStack()

            game.isInHand(1, "Cloudpost") shouldBe true
        }

        test("can't animate with only eight differently named lands — duplicates don't count") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Monument to Perfection")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Snow-Covered Plains", 1)
                .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                .withLandsOnBattlefield(1, "Cloudpost", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val monument = game.findPermanent("Monument to Perfection")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = monument, abilityId = animateAbility.id)
            ).error shouldNotBe null
            game.state.projectedState.isCreature(monument) shouldBe false
        }

        test("with nine names it becomes a 9/9 indestructible toxic 9 creature that loses its abilities; toxic doesn't stack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Monument to Perfection")
                .withNineDistinctLands()
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val monument = game.findPermanent("Monument to Perfection")!!

            // Activate twice before either resolves (ruling: toxic 9 doesn't stack).
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = monument, abilityId = animateAbility.id)
            ).error shouldBe null
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = monument, abilityId = animateAbility.id)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(monument) shouldBe true
            projected.hasType(monument, "ARTIFACT") shouldBe true
            projected.getPower(monument) shouldBe 9
            projected.getToughness(monument) shouldBe 9
            projected.hasKeyword(monument, Keyword.INDESTRUCTIBLE) shouldBe true
            projected.hasSubtype(monument, "Phyrexian") shouldBe true
            projected.hasSubtype(monument, "Construct") shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Monument to Perfection" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 11
            withClue("one instance of toxic 9 — the second activation's wipe removed the first grant") {
                poison(game, game.player2Id) shouldBe 9
            }

            withClue("it lost all abilities, including the animate ability") {
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = monument, abilityId = animateAbility.id)
                ).error shouldNotBe null
            }
        }
    }
}
