package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Iron Spider, Stark Upgrade.
 *
 * Card reference:
 * - Iron Spider, Stark Upgrade ({3}): Legendary Artifact Creature — Spider Hero, 2/3
 *   Vigilance
 *   {T}: Put a +1/+1 counter on each artifact creature and/or Vehicle you control.
 *   {2}, Remove two +1/+1 counters from among artifacts you control: Draw a card.
 *
 * The "and/or Vehicle" wording (CR 110.4 / oracle) means a Vehicle gets a counter
 * whether or not it is currently a creature — important since Vehicles are not
 * creatures until crewed.
 */
class IronSpiderStarkUpgradeScenarioTest : ScenarioTestBase() {

    private val plainArtifactCreature = CardDefinition.artifactCreature(
        name = "Test Artifact Creature",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Construct")),
        power = 2,
        toughness = 2
    )

    private val plainCreature = CardDefinition.creature(
        name = "Test Plain Creature",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    init {
        cardRegistry.register(plainArtifactCreature)
        cardRegistry.register(plainCreature)

        context("Iron Spider, Stark Upgrade") {

            test("enters as a 2/3 legendary artifact creature with vigilance") {
                val game = scenario()
                    .withPlayers("Tony", "Opponent")
                    .withCardInHand(1, "Iron Spider, Stark Upgrade")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Iron Spider, Stark Upgrade")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val spiderId = game.findPermanent("Iron Spider, Stark Upgrade")!!
                val card = game.getClientState(1).cards[spiderId]!!
                card.typeLine shouldBe "Legendary Artifact Creature — Spider Hero"
                card.subtypes shouldContain "Spider"
                card.subtypes shouldContain "Hero"
                card.power shouldBe 2
                card.toughness shouldBe 3
                card.keywords shouldContain Keyword.VIGILANCE
            }

            test("{T} ability puts a +1/+1 counter on each artifact creature, each Vehicle, and itself; non-artifact creatures unaffected") {
                val game = scenario()
                    .withPlayers("Tony", "Opponent")
                    .withCardOnBattlefield(1, "Iron Spider, Stark Upgrade")
                    .withCardOnBattlefield(1, "Test Artifact Creature")
                    .withCardOnBattlefield(1, "Weatherlight") // non-creature Vehicle (not crewed)
                    .withCardOnBattlefield(1, "Test Plain Creature") // non-artifact creature
                    .withCardOnBattlefield(1, "Hot Dog Cart") // plain artifact (not creature, not Vehicle)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spiderId = game.findPermanent("Iron Spider, Stark Upgrade")!!
                val artifactCreatureId = game.findPermanent("Test Artifact Creature")!!
                val weatherlightId = game.findPermanent("Weatherlight")!!
                val hillGiantId = game.findPermanent("Test Plain Creature")!!
                val hotDogCartId = game.findPermanent("Hot Dog Cart")!!

                val cardDef = cardRegistry.getCard("Iron Spider, Stark Upgrade")!!
                val tapAbility = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spiderId,
                        abilityId = tapAbility.id
                    )
                )
                withClue("Tap ability should activate: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                fun counters(id: com.wingedsheep.sdk.model.EntityId) =
                    game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

                withClue("Iron Spider (artifact creature) gets a counter") { counters(spiderId) shouldBe 1 }
                withClue("Other artifact creatures get a counter") { counters(artifactCreatureId) shouldBe 1 }
                withClue("Vehicles get a counter even when not crewed") { counters(weatherlightId) shouldBe 1 }
                withClue("Non-artifact creatures are unaffected") { counters(hillGiantId) shouldBe 0 }
                withClue("Non-creature, non-Vehicle artifacts are unaffected") { counters(hotDogCartId) shouldBe 0 }
            }

            test("{2}, Remove two +1/+1 counters from artifacts you control: exactly two counters removed, one card drawn") {
                val game = scenario()
                    .withPlayers("Tony", "Opponent")
                    .withCardOnBattlefield(1, "Iron Spider, Stark Upgrade") // artifact creature, counter source
                    .withCardOnBattlefield(1, "Test Artifact Creature")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains") // top-of-deck draw target
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spiderId = game.findPermanent("Iron Spider, Stark Upgrade")!!
                val artifactCreatureId = game.findPermanent("Test Artifact Creature")!!

                // Seed counters: 2 on Iron Spider, 1 on the artifact creature → total 3 across artifacts.
                game.state = game.state.updateEntity(spiderId) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
                }
                game.state = game.state.updateEntity(artifactCreatureId) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }

                val handBefore = game.handSize(1)

                val cardDef = cardRegistry.getCard("Iron Spider, Stark Upgrade")!!
                val drawAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spiderId,
                        abilityId = drawAbility.id
                    )
                )
                withClue("Draw ability should activate: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                fun counters(id: com.wingedsheep.sdk.model.EntityId) =
                    game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                val totalRemaining = counters(spiderId) + counters(artifactCreatureId)
                withClue("Exactly two counters were removed (3 - 2 = 1 remaining)") {
                    totalRemaining shouldBe 1
                }
                withClue("Exactly one card was drawn") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("draw ability is not legal when fewer than two +1/+1 counters are on artifacts you control") {
                val game = scenario()
                    .withPlayers("Tony", "Opponent")
                    .withCardOnBattlefield(1, "Iron Spider, Stark Upgrade")
                    .withCardOnBattlefield(1, "Test Plain Creature") // counters on Hill Giant must NOT count
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spiderId = game.findPermanent("Iron Spider, Stark Upgrade")!!
                val hillGiantId = game.findPermanent("Test Plain Creature")!!

                // 1 counter on Iron Spider (an artifact), 5 on Hill Giant (a non-artifact).
                // Only Iron Spider's counter counts toward the cost → 1 < 2 → unpayable.
                game.state = game.state.updateEntity(spiderId) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }
                game.state = game.state.updateEntity(hillGiantId) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 5))
                }

                val cardDef = cardRegistry.getCard("Iron Spider, Stark Upgrade")!!
                val drawAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spiderId,
                        abilityId = drawAbility.id
                    )
                )
                withClue("Activation must fail when fewer than 2 +1/+1 counters are on artifacts you control") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
