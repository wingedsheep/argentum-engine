package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fdn.cards.ChandraFlameshaper
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra, Flameshaper (FDN, {5}{R}{R}, Loyalty 6).
 *
 *   +2: Add {R}{R}{R}. Exile the top three cards of your library. Choose one. You may play that
 *       card this turn.
 *   +1: Create a token that's a copy of target creature you control, except it has haste and
 *       "At the beginning of the end step, sacrifice this token."
 *   −4: Chandra deals 8 damage divided as you choose among any number of target creatures and/or
 *       planeswalkers.
 *
 * The −4 is the engine-interesting half: it's the first *activated* ability to divide damage, so
 * the division rides on the `ActivateAbility` action and is locked onto the stack object at
 * activation (CR 601.2d) rather than being asked for at resolution. The tests below pin that
 * timing, the printed "illegal targets lose their share, the rest keep theirs" ruling, and the
 * target-count cap that keeps the division satisfiable.
 */
class ChandraFlameshaperScenarioTest : ScenarioTestBase() {

    private val plusTwo = ChandraFlameshaper.activatedAbilities[0].id
    private val plusOne = ChandraFlameshaper.activatedAbilities[1].id
    private val minusFour = ChandraFlameshaper.activatedAbilities[2].id

    init {
        context("Chandra, Flameshaper") {

            test("+2 adds {R}{R}{R}, exiles three, and grants play permission to the chosen card only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = chandra,
                        abilityId = plusTwo,
                    )
                ).error shouldBe null

                withClue("the printed ruling: the +2 is NOT a mana ability — it uses the stack") {
                    game.state.stack.size shouldBe 1
                }

                game.resolveStack()

                // "Choose one" — exactly the three freshly exiled cards are on offer.
                val choice = game.getPendingDecision()
                withClue("resolution paused to choose one of the exiled cards: $choice") {
                    (choice is SelectCardsDecision) shouldBe true
                }
                choice as SelectCardsDecision
                choice.options.size shouldBe 3

                val chosen = choice.options.first()
                game.selectCards(listOf(chosen)).error shouldBe null
                game.resolveStack()

                withClue("added {R}{R}{R}") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 3
                }
                withClue("three cards left the library for exile, one is still there") {
                    game.librarySize(1) shouldBe 1
                    game.state.getExile(game.player1Id).size shouldBe 3
                }
                withClue("only the chosen card may be played — the other two stay stranded in exile") {
                    val playable = game.state.mayPlayPermissions.flatMap { it.cardIds }.toSet()
                    playable shouldBe setOf(chosen)
                }
                withClue("+2 put two loyalty counters on Chandra") {
                    loyalty(game, chandra) shouldBe 8
                }
            }

            test("+1 copies a creature you control with haste, and the token is sacrificed at the end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = chandra,
                        abilityId = plusOne,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                val copies = game.findPermanents("Grizzly Bears")
                withClue("the original plus its token copy") {
                    copies.size shouldBe 2
                }
                val token = copies.first { it != bears }
                withClue("the copy is a 2/2 Grizzly Bears with haste bolted on") {
                    game.state.projectedState.getPower(token) shouldBe 2
                    game.state.projectedState.getToughness(token) shouldBe 2
                    game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
                }
                withClue("the original didn't gain haste — only the copy did") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }
                withClue("+1 put one loyalty counter on Chandra") {
                    loyalty(game, chandra) shouldBe 7
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("'at the beginning of the end step, sacrifice this token' fired") {
                    game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
                }
            }

            test("−4 divides 8 damage among three targets exactly as announced") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                    .withCardOnBattlefield(2, "Savannah Lions")   // 2/1
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withCardOnBattlefield(2, "Centaur Courser")  // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)
                val lions = game.findPermanent("Savannah Lions")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = chandra,
                        abilityId = minusFour,
                        targets = listOf(
                            ChosenTarget.Permanent(lions),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(courser),
                        ),
                        damageDistribution = mapOf(lions to 1, bears to 2, courser to 5),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("1 to the 2/1, 2 to the 2/2, 5 to the 3/3 — all lethal, no distribute prompt") {
                    game.hasPendingDecision() shouldBe false
                    game.findPermanent("Savannah Lions") shouldBe null
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.findPermanent("Centaur Courser") shouldBe null
                }
                withClue("−4 removed four loyalty counters") {
                    loyalty(game, chandra) shouldBe 2
                }
            }

            test("−4 rejects a division that doesn't spend exactly 8, or that starves a target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser))

                withClue("a division summing to less than 8 is illegal") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = chandra,
                            abilityId = minusFour,
                            targets = targets,
                            damageDistribution = mapOf(bears to 2, courser to 3),
                        )
                    ).error shouldNotBe null
                }
                withClue("every chosen target must get at least 1 damage (CR 601.2d)") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = chandra,
                            abilityId = minusFour,
                            targets = targets,
                            damageDistribution = mapOf(bears to 0, courser to 8),
                        )
                    ).error shouldNotBe null
                }
                withClue("the division must cover exactly the chosen targets") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = chandra,
                            abilityId = minusFour,
                            targets = targets,
                            damageDistribution = mapOf(bears to 8),
                        )
                    ).error shouldNotBe null
                }
                withClue("no illegal activation went through — Chandra still has her 6 loyalty") {
                    loyalty(game, chandra) shouldBe 6
                }
            }

            test("−4: a target that leaves in response loses its share; survivors keep theirs") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                    .withCardOnBattlefield(2, "Grizzly Bears")     // 2/2 — removed in response
                    .withCardOnBattlefield(2, "Force of Nature")   // 5/5 — survives 2, dies to 8
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)
                val bears = game.findPermanent("Grizzly Bears")!!
                val force = game.findPermanent("Force of Nature")!!

                // Announce 6 to the Bears, 2 to the 5/5. The division is locked in now.
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = chandra,
                        abilityId = minusFour,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(force)),
                        damageDistribution = mapOf(bears to 6, force to 2),
                    )
                ).error shouldBe null

                // The opponent removes the Bears while the ability waits on the stack.
                game.state = game.state.moveToZone(
                    bears,
                    ZoneKey(game.player2Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player2Id, Zone.GRAVEYARD),
                )

                game.resolveStack()

                withClue("the Bears' 6 damage is simply not dealt — it is NOT re-aimed at the 5/5") {
                    game.hasPendingDecision() shouldBe false
                    game.findPermanent("Force of Nature") shouldNotBe null
                    game.damageOn(force) shouldBe 2
                }
            }

            test("−4 can put all 8 damage on a single planeswalker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                    .withCardOnBattlefield(2, "Kaito, Cunning Infiltrator")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)
                val kaito = game.findPermanent("Kaito, Cunning Infiltrator")!!
                seedLoyalty(game, kaito, 3)

                // A single target needs no division — the whole total lands on it.
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = chandra,
                        abilityId = minusFour,
                        targets = listOf(ChosenTarget.Permanent(kaito)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("8 damage stripped Kaito's 3 loyalty and he died to the 0-loyalty SBA") {
                    game.findPermanent("Kaito, Cunning Infiltrator") shouldBe null
                }
            }

            test("−4 offers at most 8 targets, since each one needs a damage point") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flameshaper")
                repeat(10) { builder.withCardOnBattlefield(2, "Grizzly Bears") }
                val game = builder
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flameshaper")!!
                seedLoyalty(game, chandra, 6)

                val minusFourAction = game.getLegalActions(1).single {
                    it.actionInfoAbilityId() == minusFour
                }
                withClue("ten Bears plus Chandra herself are legal targets ('creatures and/or planeswalkers', any controller), but 8 damage can only be split 8 ways") {
                    minusFourAction.validTargets?.size shouldBe 11
                    minusFourAction.targetCount shouldBe 8
                }
                withClue("the client is told to collect a division of 8, min 1 each") {
                    minusFourAction.requiresDamageDistribution shouldBe true
                    minusFourAction.totalDamageToDistribute shouldBe 8
                    minusFourAction.minDamagePerTarget shouldBe 1
                }
            }
        }
    }

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder drops permanents straight onto the battlefield without running the
        // "enters with its starting loyalty" step, so seed it explicitly.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)
            ?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
            ?.amount ?: 0

    private fun com.wingedsheep.engine.view.LegalActionInfo.actionInfoAbilityId(): AbilityId? =
        (action as? ActivateAbility)?.abilityId
}
