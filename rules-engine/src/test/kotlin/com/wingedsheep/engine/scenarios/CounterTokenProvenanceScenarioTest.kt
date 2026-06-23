package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConvertCountersToTokensEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for token provenance ("tokens created with this creature") + the
 * counter↔token conversion both ways — the engine gap behind Tetravus. The inline test card
 * exposes the two halves as `{0}:` activated abilities so each direction is driven deterministically:
 *  - convert: remove any number of +1/+1 counters, mint that many provenance-stamped tokens;
 *  - reabsorb: exile any number of *its own* tokens, add that many +1/+1 counters back.
 */
class CounterTokenProvenanceScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private val testTetravus = card("Test Provenator") {
        manaCost = "{6}"
        colorIdentity = ""
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "{0}: convert counters to tokens. {0}: reabsorb its own tokens."

        replacementEffect(
            com.wingedsheep.sdk.scripting.EntersWithCounters(
                counterType = com.wingedsheep.sdk.scripting.events.CounterTypeFilter.PlusOnePlusOne,
                count = 3,
                selfOnly = true
            )
        )

        // {0}: Remove any number of +1/+1 counters; create that many provenance-stamped 1/1 tokens.
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = ConvertCountersToTokensEffect(
                tokenFactory = CreateTokenEffect(
                    count = DynamicAmount.Fixed(1),
                    power = 1,
                    toughness = 1,
                    colors = emptySet(),
                    creatureTypes = setOf("Provenite"),
                    keywords = setOf(Keyword.FLYING),
                    name = "Provenite",
                    artifactToken = true,
                    stampCreator = true
                )
            )
            description = "Remove any number of +1/+1 counters; create that many tokens."
        }

        // {0}: Exile any number of tokens created with this creature; add that many +1/+1 counters.
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.Pipeline {
                val mine = gather(
                    com.wingedsheep.sdk.scripting.effects.CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Any.youControl().createdBySource(),
                        player = Player.You
                    ),
                    name = "myTokens"
                )
                val chosen = chooseAnyNumber(from = mine, name = "chosenTokens")
                exile(chosen)
                run(
                    Effects.AddDynamicCounters(
                        counterType = "+1/+1",
                        amount = DynamicAmount.VariableReference("${chosen.key}_count"),
                        target = EffectTarget.Self
                    )
                )
            }
            description = "Exile any number of tokens created with this creature; add that many +1/+1 counters."
        }
    }

    init {
        cardRegistry.register(testTetravus)

        fun plusOne(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Counter <-> token provenance") {

            test("removing counters mints that many provenance-stamped tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Provenator")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Provenator").error shouldBe null
                game.resolveStack()
                val prov = game.findPermanent("Test Provenator")!!
                withClue("enters with 3 counters") { plusOne(game, prov) shouldBe 3 }

                val convert = cardRegistry.getCard("Test Provenator")!!.script.activatedAbilities[0]
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = prov, abilityId = convert.id)).error shouldBe null
                game.resolveStack()
                // Choose to remove 2 of the 3 counters.
                game.chooseNumber(2)
                game.resolveStack()

                withClue("2 counters removed") { plusOne(game, prov) shouldBe 1 }
                val tokens = game.state.getBattlefield().filter {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Provenite"
                }
                withClue("2 Provenite tokens created") { tokens.size shouldBe 2 }
                withClue("tokens are stamped with the creator") {
                    tokens.all {
                        game.state.getEntity(it)
                            ?.get<com.wingedsheep.engine.state.components.identity.CreatedByComponent>()?.creatorId == prov
                    } shouldBe true
                }
            }

            test("reabsorbing only its own tokens adds that many counters back") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Provenator")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Provenator").error shouldBe null
                game.resolveStack()
                val prov = game.findPermanent("Test Provenator")!!
                // Convert all 3 counters into 3 tokens.
                val convert = cardRegistry.getCard("Test Provenator")!!.script.activatedAbilities[0]
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = prov, abilityId = convert.id))
                game.resolveStack()
                game.chooseNumber(3)
                game.resolveStack()
                withClue("0 counters, 3 tokens") { plusOne(game, prov) shouldBe 0 }

                // Reabsorb 3 tokens -> 3 counters back.
                val reabsorb = cardRegistry.getCard("Test Provenator")!!.script.activatedAbilities[1]
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = prov, abilityId = reabsorb.id)).error shouldBe null
                game.resolveStack()
                // Select all 3 of its own tokens.
                val ownTokens = game.state.getBattlefield().filter {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CreatedByComponent>()?.creatorId == prov
                }
                withClue("3 own tokens available to reabsorb") { ownTokens.size shouldBe 3 }
                game.selectCards(ownTokens)
                game.resolveStack()

                withClue("3 counters added back") { plusOne(game, prov) shouldBe 3 }
                val remaining = game.state.getBattlefield().count {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Provenite"
                }
                withClue("the reabsorbed tokens left the battlefield") { remaining shouldBe 0 }

                val projected = stateProjector.project(game.state)
                withClue("1/1 base + 3 counters = 4/4") {
                    projected.getPower(prov) shouldBe 4
                    projected.getToughness(prov) shouldBe 4
                }
            }

            // The whole reason CreatedBySource exists over "tokens you control named X": with two
            // sources minting the SAME-named token, a source may only reabsorb the tokens IT made.
            test("a second source cannot reabsorb the first source's same-named tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Provenator")
                    .withCardInHand(1, "Test Provenator")
                    .withLandsOnBattlefield(1, "Mountain", 12)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Source A enters and converts all 3 of its counters into 3 Provenite tokens.
                game.castSpell(1, "Test Provenator").error shouldBe null
                game.resolveStack()
                val provA = game.findPermanent("Test Provenator")!!
                val convert = cardRegistry.getCard("Test Provenator")!!.script.activatedAbilities[0]
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = provA, abilityId = convert.id))
                game.resolveStack()
                game.chooseNumber(3)
                game.resolveStack()
                withClue("source A: 0 counters, 3 tokens stamped by A") {
                    plusOne(game, provA) shouldBe 0
                }

                // Source B enters with its own 3 counters. A's 3 Provenite tokens are on the
                // battlefield and B's controller controls them too — but B did not create them.
                game.castSpell(1, "Test Provenator").error shouldBe null
                game.resolveStack()
                val provB = game.findPermanents("Test Provenator").first { it != provA }
                withClue("source B enters with 3 counters") { plusOne(game, provB) shouldBe 3 }

                val tokensCreatedByA = game.state.getBattlefield().filter {
                    game.state.getEntity(it)
                        ?.get<com.wingedsheep.engine.state.components.identity.CreatedByComponent>()?.creatorId == provA
                }
                withClue("A's 3 tokens are present and controlled by the same player as B") {
                    tokensCreatedByA.size shouldBe 3
                }

                // B activates its reabsorb. Its gather is createdBySource(B), so none of A's tokens
                // qualify even though they are same-named and under the same controller.
                val reabsorb = cardRegistry.getCard("Test Provenator")!!.script.activatedAbilities[1]
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = provB, abilityId = reabsorb.id)).error shouldBe null
                game.resolveStack()
                // If B is prompted at all, the only eligible set is empty — selecting nothing.
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectCardsDecision) {
                    game.selectCards(emptyList())
                    game.resolveStack()
                }

                withClue("B gained no counters — it could not see A's tokens") {
                    plusOne(game, provB) shouldBe 3
                }
                withClue("A's tokens are untouched (not exiled by B)") {
                    val stillThere = game.state.getBattlefield().filter {
                        game.state.getEntity(it)
                            ?.get<com.wingedsheep.engine.state.components.identity.CreatedByComponent>()?.creatorId == provA
                    }
                    stillThere.size shouldBe 3
                }
            }
        }
    }
}
