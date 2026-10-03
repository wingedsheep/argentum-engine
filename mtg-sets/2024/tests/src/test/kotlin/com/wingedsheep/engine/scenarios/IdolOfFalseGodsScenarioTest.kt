package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.IdolOfFalseGods
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Idol of False Gods (MH3 #210) — {2} Kindred Artifact — Eldrazi
 *
 *   {1}{C}, {T}: Create a 0/1 colorless Eldrazi Spawn creature token with "Sacrifice this token: Add {C}."
 *   Whenever another Eldrazi you control dies, put a +1/+1 counter on this artifact.
 *   As long as this artifact has eight or more +1/+1 counters on it, it's a 0/0 creature in addition
 *   to its other types and it has annihilator 2.
 */
class IdolOfFalseGodsScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.givePlusOnes(id: EntityId, n: Int) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, n))
        }
    }

    // Strips every artifact's abilities, without itself touching type or P/T.
    private val mutingField = card("Test Muting Field") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility {
            ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Artifact))
        }
    }

    init {
        cardRegistry.register(mutingField)

        context("Idol of False Gods") {

            test("{1}{C}, {T} creates an Eldrazi Spawn token") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withLandsOnBattlefield(1, "Wastes", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!

                game.execute(
                    ActivateAbility(game.player1Id, idol, IdolOfFalseGods.activatedAbilities[0].id)
                ).error shouldBe null
                game.resolveStack()

                game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
            }

            test("the ability needs colorless mana — {C} can't be paid with a Forest") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!

                game.execute(
                    ActivateAbility(game.player1Id, idol, IdolOfFalseGods.activatedAbilities[0].id)
                ).error shouldNotBe null
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
            }

            test("each other Eldrazi you control dying adds a +1/+1 counter; a non-Eldrazi does not") {
                // Eldrazi Ravager's graveyard ability sacrifices two Eldrazi as its cost — a clean
                // way to make two Eldrazi die at once.
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withCardInGraveyard(1, "Eldrazi Ravager")
                    .withCardOnBattlefield(1, "Nulldrifter")
                    .withCardOnBattlefield(1, "Artisan of Kozilek")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!
                val ravager = game.findCardsInGraveyard(1, "Eldrazi Ravager").single()
                val abilityId = cardRegistry.getCard("Eldrazi Ravager")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ravager,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(
                                game.findPermanent("Nulldrifter")!!,
                                game.findPermanent("Artisan of Kozilek")!!
                            )
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("two Eldrazi died, so two counters") { game.plusOnes(idol) shouldBe 2 }
                withClue("with fewer than eight counters it is still not a creature") {
                    game.state.projectedState.isCreature(idol) shouldBe false
                }
            }

            test("an opponent's Eldrazi dying does not add a counter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withCardInGraveyard(2, "Eldrazi Ravager")
                    .withCardOnBattlefield(2, "Nulldrifter")
                    .withCardOnBattlefield(2, "Artisan of Kozilek")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!
                val ravager = game.findCardsInGraveyard(2, "Eldrazi Ravager").single()
                val abilityId = cardRegistry.getCard("Eldrazi Ravager")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = ravager,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(
                                game.findPermanent("Nulldrifter")!!,
                                game.findPermanent("Artisan of Kozilek")!!
                            )
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.plusOnes(idol) shouldBe 0
            }

            test("at seven counters it is not a creature; at eight it is an 8/8 creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!

                game.givePlusOnes(idol, 7)
                game.state.projectedState.isCreature(idol) shouldBe false

                game.givePlusOnes(idol, 1)
                withClue("eight counters animate it as a 0/0 plus eight +1/+1 counters") {
                    game.state.projectedState.isCreature(idol) shouldBe true
                    game.state.projectedState.getPower(idol) shouldBe 8
                    game.state.projectedState.getToughness(idol) shouldBe 8
                }
            }

            test("losing all abilities after it animated leaves it an 8/8 creature (CR 613.6)") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withCardOnBattlefield(2, "Test Muting Field")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!

                game.givePlusOnes(idol, 8)
                withClue("the Layer 4 type grant and its Layer 7b 0/0 survive the Layer 6 ability loss") {
                    game.state.projectedState.isCreature(idol) shouldBe true
                    game.state.projectedState.getPower(idol) shouldBe 8
                    game.state.projectedState.getToughness(idol) shouldBe 8
                }
            }

            test("with eight counters it attacks with annihilator 2") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Idol of False Gods")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val idol = game.findPermanent("Idol of False Gods")!!
                game.givePlusOnes(idol, 8)
                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findPermanents("Forest").first()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Idol of False Gods" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the defending player chooses") { decision.playerId shouldBe game.player2Id }

                game.selectCards(listOf(bears, forest)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanents("Forest") shouldHaveSize 1
            }
        }
    }
}
