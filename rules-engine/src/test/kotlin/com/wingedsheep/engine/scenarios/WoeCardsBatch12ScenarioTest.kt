package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.woe.cards.EdgewallInn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the twelfth batch of Wilds of Eldraine cards.
 *
 * Every card in the batch composes primitives that already existed, so these tests only pin the
 * multi-hop invariants each card silently depends on — the places where a plausible-looking
 * composition would still resolve wrong:
 *
 *  - **Beluna Grandsquall // Seek Thrills** — the second `MoveCollectionEffect` re-reads the
 *    `"milled"` collection *after* the mill already drained it to the graveyard, with no selection
 *    step in between; and the "permanent spells" discount must reach an adventurer's front face
 *    while never reaching its Adventure (which is the engine's alternative-cost pricing path doing
 *    the work, not the `Permanent` filter — see the card's KDoc).
 *  - **Callous Sell-Sword // Burn Together** — the counters are self-scoped (`otherOnly = false`
 *    means "only this permanent"), and the intended line is Burn Together first, so the creature
 *    sacrificed to it has to be counted by the time the Sell-Sword itself enters.
 *  - **Decadent Dragon // Expensive Taste** — the stolen cards land in their *owner's* exile, face
 *    down, revealed to the caster only, with a permanent may-play permission for the caster. The
 *    "you may look at them" clause is carried entirely by the gather's reveal surviving the move.
 *  - **Virtue of Persistence // Locthwain Scorn** — reanimation out of an *opponent's* graveyard
 *    under your control, and the first non-creature (Enchantment) face cast out of Adventure exile.
 *  - **Edgewall Inn** — "a card that has an Adventure" is a whole-card characteristic in the
 *    graveyard, so it finds an adventurer card and rejects a plain instant.
 */
class WoeCardsBatch12ScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast the Adventure (secondary) face of an adventurer card in hand — `faceIndex = 0`. */
    private fun TestGame.castAdventure(
        cardName: String,
        targets: List<ChosenTarget> = emptyList()
    ) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, cardName).first(),
            targets = targets,
            faceIndex = 0,
        )
    )

    init {
        context("Beluna Grandsquall // Seek Thrills") {
            test("Seek Thrills mills seven and pulls only the cards that have an Adventure back to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Beluna Grandsquall")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    // Exactly seven cards, so the whole library is milled: two adventurers, five not.
                    .withCardInLibrary(1, "Woodland Acolyte")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Threadbind Clique")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castAdventure("Beluna Grandsquall").error shouldBe null
                game.resolveStack()

                withClue("the whole seven-card library was milled") {
                    game.librarySize(1) shouldBe 0
                }
                withClue("both adventurer cards were pulled out of the milled pile into hand") {
                    game.isInHand(1, "Woodland Acolyte") shouldBe true
                    game.isInHand(1, "Threadbind Clique") shouldBe true
                    game.isInGraveyard(1, "Woodland Acolyte") shouldBe false
                    game.isInGraveyard(1, "Threadbind Clique") shouldBe false
                }
                withClue("everything without an Adventure stays in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
                withClue("the Adventure exiles itself, so the creature is castable later (CR 715.3d)") {
                    game.isInExile(1, "Beluna Grandsquall") shouldBe true
                }
            }

            test("an adventurer permanent spell costs {1} less with Beluna out") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Beluna Grandsquall", summoningSickness = false)
                    .withCardInHand(1, "Woodland Acolyte")
                    // Woodland Acolyte is {2}{W}; two Plains only pay for it at the discounted {1}{W}.
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Woodland Acolyte").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Woodland Acolyte") shouldBe true
            }

            test("without Beluna the same two lands are not enough — the discount is doing the work") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Woodland Acolyte")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Woodland Acolyte").error shouldNotBe null
                game.isOnBattlefield("Woodland Acolyte") shouldBe false
            }

            test("the Adventure half of an adventurer gets no discount — it is not a permanent spell") {
                fun ripTheSeams(plains: Int) = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Beluna Grandsquall", summoningSickness = false)
                    .withCardInHand(1, "Threadbind Clique")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    // Rip the Seams is {2}{W} — three mana, discounted or not.
                    .withLandsOnBattlefield(1, "Plains", plains)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val short = ripTheSeams(plains = 2)
                val bearsShort = short.findPermanent("Grizzly Bears").shouldNotBeNull()
                withClue("two mana must not be enough: Beluna's {1} may not reach an Adventure spell") {
                    short.castAdventure(
                        "Threadbind Clique",
                        listOf(ChosenTarget.Permanent(bearsShort))
                    ).error shouldNotBe null
                    short.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                val full = ripTheSeams(plains = 3)
                val bearsFull = full.findPermanent("Grizzly Bears").shouldNotBeNull()
                withClue("the same cast at full price resolves — the failure above was the cost, not the target") {
                    full.castAdventure(
                        "Threadbind Clique",
                        listOf(ChosenTarget.Permanent(bearsFull))
                    ).error shouldBe null
                    full.resolveStack()
                    full.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }

        context("Callous Sell-Sword // Burn Together") {
            test("Burn Together sacrifices its own creature, and the Sell-Sword then counts it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Callous Sell-Sword")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castAdventure(
                    "Callous Sell-Sword",
                    listOf(ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player2Id))
                ).error shouldBe null
                game.resolveStack()

                withClue("the 3/3 dealt its own power to the other target") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("and was sacrificed afterwards") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                game.isInExile(1, "Callous Sell-Sword") shouldBe true

                game.castSpellFromExile(1, "Callous Sell-Sword").error shouldBe null
                game.resolveStack()

                val sellSword = game.findPermanent("Callous Sell-Sword").shouldNotBeNull()
                withClue("one creature died under your control this turn — the one Burn Together ate") {
                    plusOneCounters(game, sellSword) shouldBe 1
                    power(game, sellSword) shouldBe 3
                    toughness(game, sellSword) shouldBe 3
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                withClue("the replacement is self-scoped: your other creatures get nothing") {
                    plusOneCounters(game, bears) shouldBe 0
                    power(game, bears) shouldBe 2
                }
            }

            test("with nothing dead this turn the Sell-Sword enters as a plain 2/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Callous Sell-Sword")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Callous Sell-Sword").error shouldBe null
                game.resolveStack()

                val sellSword = game.findPermanent("Callous Sell-Sword").shouldNotBeNull()
                plusOneCounters(game, sellSword) shouldBe 0
                power(game, sellSword) shouldBe 2
            }
        }

        context("Decadent Dragon // Expensive Taste") {
            test("the top two cards go to their owner's exile face down, visible and playable only to you") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decadent Dragon")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val topTwo = game.state.getLibrary(game.player2Id).take(2)

                game.castAdventure(
                    "Decadent Dragon",
                    listOf(ChosenTarget.Player(game.player2Id))
                ).error shouldBe null
                game.resolveStack()

                withClue("exile is keyed to the owner, not to the caster") {
                    game.state.getExile(game.player2Id).containsAll(topTwo) shouldBe true
                    game.librarySize(2) shouldBe 1
                }
                topTwo.forEach { stolen ->
                    withClue("each stolen card is face down in exile") {
                        game.state.getEntity(stolen)?.get<FaceDownComponent>() shouldBe FaceDownComponent
                    }
                    withClue("'you may look at' — revealed to the caster and to nobody else") {
                        game.state.getEntity(stolen)?.get<RevealedToComponent>()?.playerIds shouldBe
                            setOf(game.player1Id)
                    }
                    withClue("'and play them for as long as they remain exiled'") {
                        game.state.mayPlayPermissions.any {
                            stolen in it.cardIds && it.controllerId == game.player1Id
                        } shouldBe true
                    }
                }
            }

            test("the Dragon makes a Treasure whenever it attacks") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Decadent Dragon", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.findPermanent("Treasure") shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Decadent Dragon" to 2)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Treasure").shouldNotBeNull()
            }
        }

        context("Virtue of Persistence // Locthwain Scorn") {
            test("your upkeep reanimates a creature card out of an opponent's graveyard under your control") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Virtue of Persistence")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                    game.resolveStack()
                }

                withClue("'from a graveyard' is any graveyard, and control follows the enchantment") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
            }

            test("Locthwain Scorn kills a 2/2, gains 2 life, and the enchantment is cast from exile after") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Virtue of Persistence")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 9)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castAdventure(
                    "Virtue of Persistence",
                    listOf(ChosenTarget.Permanent(bears))
                ).error shouldBe null
                game.resolveStack()

                withClue("-3/-3 kills the 2/2 and the life gain is unconditional") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.getLifeTotal(1) shouldBe 22
                }
                game.isInExile(1, "Virtue of Persistence") shouldBe true

                game.castSpellFromExile(1, "Virtue of Persistence").error shouldBe null
                game.resolveStack()

                withClue("a non-creature front face is cast out of Adventure exile like any other") {
                    game.isOnBattlefield("Virtue of Persistence") shouldBe true
                    game.isInExile(1, "Virtue of Persistence") shouldBe false
                }
            }
        }

        context("Edgewall Inn") {
            fun inn() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Edgewall Inn", summoningSickness = false)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Woodland Acolyte")
                .withCardInGraveyard(1, "Doom Blade")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("the sacrifice ability returns a card that has an Adventure from your graveyard") {
                val game = inn()
                val innId = game.findPermanent("Edgewall Inn").shouldNotBeNull()
                val acolyte = game.findCardsInGraveyard(1, "Woodland Acolyte").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = innId,
                        abilityId = EdgewallInn.activatedAbilities[1].id,
                        targets = listOf(ChosenTarget.Card(acolyte, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("an adventurer card in the graveyard 'has an Adventure' even showing its front face") {
                    game.isInHand(1, "Woodland Acolyte") shouldBe true
                }
                withClue("the Inn sacrificed itself as a cost") {
                    game.isOnBattlefield("Edgewall Inn") shouldBe false
                }
            }

            test("a plain instant in the graveyard is not a legal target") {
                val game = inn()
                val innId = game.findPermanent("Edgewall Inn").shouldNotBeNull()
                val bolt = game.findCardsInGraveyard(1, "Doom Blade").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = innId,
                        abilityId = EdgewallInn.activatedAbilities[1].id,
                        targets = listOf(ChosenTarget.Card(bolt, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldNotBe null
                withClue("the ability never went on the stack, so nothing was paid") {
                    game.isInGraveyard(1, "Doom Blade") shouldBe true
                    game.isOnBattlefield("Edgewall Inn") shouldBe true
                }
            }

            test("the mana ability adds the color chosen as the Inn entered, not a fresh choice") {
                val game = inn()
                val innId = game.findPermanent("Edgewall Inn").shouldNotBeNull()
                game.state = game.state.updateEntity(innId) { c ->
                    c.with(
                        CastChoicesComponent(
                            chosen = mapOf(ChoiceSlot.COLOR to ChoiceValue.ColorChoice(Color.RED))
                        )
                    )
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = innId,
                        abilityId = EdgewallInn.activatedAbilities[0].id,
                    )
                ).error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                withClue("red was the recorded choice") {
                    pool.red shouldBe 1
                    pool.white shouldBe 0
                    pool.colorless shouldBe 0
                }
            }
        }
    }
}
