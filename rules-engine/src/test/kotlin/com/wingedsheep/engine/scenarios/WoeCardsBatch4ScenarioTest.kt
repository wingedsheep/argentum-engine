package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for a batch of Wilds of Eldraine cards implemented together:
 *
 *  - Verdant Outrider ({2}{G} 4/2) — {1}{G} grants "can't be blocked by creatures with power 2
 *    or less" for the turn.
 *  - Succumb to the Cold ({2}{U} instant) — taps *one or two* target creatures an opponent
 *    controls and stuns each of them.
 *  - Skybeast Tracker ({3}{G} 2/4, Reach) — Food on every spell you cast with mana value 5+.
 *  - Savior of the Sleeping ({2}{W} 2/3, Vigilance) — grows whenever an enchantment you control
 *    hits the graveyard from the battlefield.
 *  - Spiteful Hexmage ({B} 3/2) — ETB puts a Cursed Role on target creature you control, which
 *    sets that creature's base P/T to 1/1.
 */
class WoeCardsBatch4ScenarioTest : ScenarioTestBase() {

    init {
        context("Verdant Outrider — activated evasion against small blockers") {
            test("a power-2 creature can't block after the ability resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Verdant Outrider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val outrider = game.findPermanent("Verdant Outrider")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = outrider,
                        abilityId = outriderAbilityId()
                    )
                ).error shouldBe null
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Verdant Outrider" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Verdant Outrider")))
                withClue("2/2 Grizzly Bears has power 2, so the granted restriction stops the block") {
                    block.error shouldNotBe null
                }
            }

            test("a power-3 creature blocks fine even after the activation") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Verdant Outrider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val outrider = game.findPermanent("Verdant Outrider")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = outrider,
                        abilityId = outriderAbilityId()
                    )
                ).error shouldBe null
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Verdant Outrider" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("3/3 Hill Giant is above power 2 and blocks normally") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Verdant Outrider")))
                        .error shouldBe null
                }
            }

            test("without activating, a 2/2 blocks it just fine") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Verdant Outrider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Verdant Outrider" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("no activation means no restriction at all") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Verdant Outrider")))
                        .error shouldBe null
                }
            }
        }

        context("Succumb to the Cold — tap and stun one or two") {
            test("both targets are tapped and each gets a stun counter") {
                val game = coldScenario()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castCold(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                listOf(bears to "Grizzly Bears", giant to "Hill Giant").forEach { (id, name) ->
                    withClue("$name is tapped") {
                        game.state.getEntity(id)?.has<TappedComponent>() shouldBe true
                    }
                    withClue("$name has a stun counter") { game.stunCounters(id) shouldBe 1 }
                }
            }

            test("choosing a single target is legal — 'one or two'") {
                val game = coldScenario()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castCold(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the single chosen target is tapped and stunned") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                    game.stunCounters(bears) shouldBe 1
                }
                withClue("the untargeted creature is untouched") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                    game.stunCounters(giant) shouldBe 0
                }
            }
        }

        context("Skybeast Tracker — Food on expensive casts") {
            test("casting a mana value 5 spell makes a Food, a cheap one does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skybeast Tracker", summoningSickness = false)
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Serra Angel")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Skybeast Tracker has reach") {
                    game.state.projectedState.hasKeyword(
                        game.findPermanent("Skybeast Tracker")!!, Keyword.REACH
                    ) shouldBe true
                }

                game.castSpell(1, "Savannah Lions").error shouldBe null
                game.resolveStack()
                withClue("mana value 1 is below the threshold — no Food") {
                    game.findPermanents("Food").size shouldBe 0
                }

                game.castSpell(1, "Serra Angel").error shouldBe null
                game.resolveStack()
                withClue("Serra Angel is mana value 5 — one Food token") {
                    game.findPermanents("Food").size shouldBe 1
                }
            }
        }

        context("Savior of the Sleeping — grows off dying enchantments") {
            test("an enchantment you control reaching the graveyard adds a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savior of the Sleeping", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Enchantment")
                    .withCardInHand(1, "Naturalize")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val savior = game.findPermanent("Savior of the Sleeping")!!
                withClue("Savior of the Sleeping has vigilance") {
                    game.state.projectedState.hasKeyword(savior, Keyword.VIGILANCE) shouldBe true
                }
                withClue("it starts as a printed 2/3") {
                    game.state.projectedState.getPower(savior) shouldBe 2
                    game.state.projectedState.getToughness(savior) shouldBe 3
                }

                val enchantment = game.findPermanent("Test Enchantment")!!
                game.castSpell(1, "Naturalize", enchantment).error shouldBe null
                game.resolveStack()

                withClue("the destroyed enchantment triggered the Savior") {
                    game.state.projectedState.getPower(savior) shouldBe 3
                    game.state.projectedState.getToughness(savior) shouldBe 4
                }
            }

            test("an opponent's enchantment dying does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savior of the Sleeping", summoningSickness = false)
                    .withCardOnBattlefield(2, "Test Enchantment")
                    .withCardInHand(1, "Naturalize")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val savior = game.findPermanent("Savior of the Sleeping")!!
                val enchantment = game.findPermanent("Test Enchantment")!!
                game.castSpell(1, "Naturalize", enchantment).error shouldBe null
                game.resolveStack()

                withClue("'an enchantment you control' excludes the opponent's") {
                    game.state.projectedState.getPower(savior) shouldBe 2
                    game.state.projectedState.getToughness(savior) shouldBe 3
                }
            }
        }

        context("Spiteful Hexmage — Cursed Role on a creature you control") {
            test("the chosen creature becomes 1/1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Spiteful Hexmage")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Spiteful Hexmage").error shouldBe null
                game.resolveStack() // Hexmage enters -> ETB trigger asks for its target
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("a Cursed Role token exists") {
                    (game.findPermanent("Cursed Role") != null) shouldBe true
                }
                withClue("the Cursed Role sets the enchanted creature's base P/T to 1/1") {
                    game.state.projectedState.getPower(giant) shouldBe 1
                    game.state.projectedState.getToughness(giant) shouldBe 1
                }

                val hexmage = game.findPermanent("Spiteful Hexmage")!!
                withClue("the Hexmage itself is unenchanted here and stays a 3/2") {
                    game.state.projectedState.getPower(hexmage) shouldBe 3
                    game.state.projectedState.getToughness(hexmage) shouldBe 2
                }
            }

            test("the Hexmage is a legal target for its own trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Spiteful Hexmage")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Spiteful Hexmage").error shouldBe null
                game.resolveStack()

                val hexmage = game.findPermanent("Spiteful Hexmage")!!
                game.selectTargets(listOf(hexmage)).error shouldBe null
                game.resolveStack()

                withClue("cursing itself makes the 3/2 a 1/1") {
                    game.state.projectedState.getPower(hexmage) shouldBe 1
                    game.state.projectedState.getToughness(hexmage) shouldBe 1
                }
            }
        }
    }

    private fun outriderAbilityId() =
        cardRegistry.getCard("Verdant Outrider")!!.activatedAbilities.first().id

    private fun coldScenario(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Succumb to the Cold")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castCold(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Succumb to the Cold").first(),
            targets = targets.map { ChosenTarget.Permanent(it) }
        )
    )

    private fun TestGame.stunCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
}
