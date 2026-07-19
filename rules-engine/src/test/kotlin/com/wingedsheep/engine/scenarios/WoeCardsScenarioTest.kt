package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Wilds of Eldraine batch:
 *
 *  - **Hopeless Nightmare** — ETB (each opponent discards + loses 2), the leaves-for-graveyard
 *    scry trigger, and the `{2}{B}: Sacrifice this enchantment` ability that fires it.
 *  - **Harried Spearguard** — dies → 1/1 black Rat that can't block.
 *  - **Warehouse Tabby** — enchantment-you-control hits the graveyard → Rat; `{1}{B}` deathtouch.
 *  - **Ego Drain** — targeted nonland strip, plus the "if you don't control a Faerie" exile rider
 *    in both directions.
 *  - **Commune with Nature** — look at five, take a creature (or decline), rest to the bottom.
 *  - **Rat Out** — up-to-one-target -1/-1 plus the always-made Rat that can't block.
 *  - **Cursed Courtier** — ETB Cursed Role on itself, dropping it to a 1/1 lifelinker.
 *  - **Voracious Vermin** — ETB Rat, and the "another creature you control dies" +1/+1 counter.
 *  - **Wicked Visitor** — an enchantment you control hitting the graveyard drains each opponent.
 *  - **Knight of Doves** — an enchantment you control hitting the graveyard makes a 1/1 white flying Bird.
 *  - **Gnawing Crescendo** — team +2/+0, and a turn-long delayed trigger that Rats each nontoken death.
 */
class WoeCardsScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val nightmareSacrificeAbility by lazy {
        cardRegistry.requireCard("Hopeless Nightmare").activatedAbilities[0].id
    }

    private val tabbyDeathtouchAbility by lazy {
        cardRegistry.requireCard("Warehouse Tabby").activatedAbilities[0].id
    }

    init {
        context("Hopeless Nightmare") {

            test("enters: each opponent discards a card and loses 2 life") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Hopeless Nightmare")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLife = game.getLifeTotal(2)
                game.castSpell(1, "Hopeless Nightmare").error shouldBe null
                game.resolveStack()

                withClue("the opponent's only card should have been discarded") {
                    game.handSize(2) shouldBe 0
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                game.getLifeTotal(2) shouldBe opponentLife - 2
            }

            test("{2}{B} sacrifices it, and hitting the graveyard scries 2") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hopeless Nightmare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nightmare = game.findPermanent("Hopeless Nightmare")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nightmare,
                        abilityId = nightmareSacrificeAbility
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the sacrifice is the ability's effect, so it goes to the graveyard") {
                    game.isInGraveyard(1, "Hopeless Nightmare") shouldBe true
                }
                withClue("dying from the battlefield puts the scry-2 trigger on the stack") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }

        context("Harried Spearguard") {

            test("dies: creates a 1/1 black Rat that can't block") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Harried Spearguard")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spearguard = game.findPermanent("Harried Spearguard")!!
                game.castSpell(1, "Shock", targetId = spearguard).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Harried Spearguard") shouldBe true
                val rat = game.findPermanent("Rat Token")
                withClue("the dies trigger should have made a Rat token") { rat shouldNotBe null }

                val projected = projector.project(game.state)
                projected.getPower(rat!!) shouldBe 1
                projected.getToughness(rat) shouldBe 1
                withClue("the token's granted static ability makes it unable to block") {
                    game.state.projectedState.cantBlock(rat) shouldBe true
                }
            }
        }

        context("Warehouse Tabby") {

            test("an enchantment you control hitting the graveyard makes a Rat") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Warehouse Tabby")
                    .withCardOnBattlefield(1, "Hopeless Nightmare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nightmare = game.findPermanent("Hopeless Nightmare")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nightmare,
                        abilityId = nightmareSacrificeAbility
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("sacrificing the enchantment should feed the Tabby's trigger") {
                    game.findPermanent("Rat Token") shouldNotBe null
                }
            }

            test("{1}{B} grants deathtouch until end of turn") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Warehouse Tabby")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tabby = game.findPermanent("Warehouse Tabby")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tabby,
                        abilityId = tabbyDeathtouchAbility
                    )
                ).error shouldBe null
                game.resolveStack()

                projector.project(game.state).hasKeyword(tabby, Keyword.DEATHTOUCH) shouldBe true
            }
        }

        context("Ego Drain") {

            test("strips a nonland card and exiles one of yours when you control no Faerie") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Ego Drain")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Ego Drain", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()

                // Choose the opponent's only nonland card — Forest is filtered out of the choice.
                game.hasPendingDecision() shouldBe true
                game.selectCards(game.findCardsInHand(2, "Shock")).error shouldBe null
                game.resolveStack()

                withClue("the chosen nonland card is discarded, the land stays") {
                    game.isInGraveyard(2, "Shock") shouldBe true
                    game.isInHand(2, "Forest") shouldBe true
                }

                // Then the Faerie rider: you control no Faerie, so exile a card from your hand.
                game.hasPendingDecision() shouldBe true
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                withClue("no Faerie → the rider exiles a card from your hand") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
            }

            test("controlling a Faerie skips the exile rider") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Ego Drain")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(1, "Faerie Dreamthief")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Shock")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Ego Drain", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.selectCards(game.findCardsInHand(2, "Shock")).error shouldBe null
                game.resolveStack()

                withClue("the strip half still happens") {
                    game.isInGraveyard(2, "Shock") shouldBe true
                }
                withClue("you control a Faerie → your hand is untouched") {
                    game.hasPendingDecision() shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Grizzly Bears") shouldBe false
                }
            }
        }

        context("Commune with Nature") {

            test("takes a creature from the top five and bottoms the rest") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Commune with Nature")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.librarySize(1)
                game.castSpell(1, "Commune with Nature").error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears")
                withClue("the only creature among the five should be selectable") {
                    bears shouldNotBe emptyList<Any>()
                }
                game.selectCards(bears).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("the other four go to the bottom — only the taken card leaves the library") {
                    game.librarySize(1) shouldBe librarySizeBefore - 1
                }
            }

            test("declining leaves all five in the library") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Commune with Nature")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.librarySize(1)
                game.castSpell(1, "Commune with Nature").error shouldBe null
                game.resolveStack()

                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("\"you may reveal\" — declining is legal even with a creature among them") {
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                    game.librarySize(1) shouldBe librarySizeBefore
                }
            }
        }

        context("Rat Out") {

            test("gives -1/-1 to the target and makes a Rat that can't block") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Rat Out")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Rat Out", targetId = bears).error shouldBe null
                game.resolveStack()

                val projected = projector.project(game.state)
                withClue("2/2 Grizzly Bears takes -1/-1") {
                    projected.getPower(bears) shouldBe 1
                    projected.getToughness(bears) shouldBe 1
                }
                val rat = game.findPermanent("Rat Token")
                withClue("you always make the Rat") { rat shouldNotBe null }
                game.state.projectedState.cantBlock(rat!!) shouldBe true
            }

            test("with no target, the Rat is still created") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Rat Out")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // "Up to one target" — casting with no target is legal.
                game.castSpell(1, "Rat Out").error shouldBe null
                game.resolveStack()

                game.findPermanent("Rat Token") shouldNotBe null
            }
        }

        context("Cursed Courtier") {

            test("enters with a Cursed Role attached, so it's a 1/1 lifelinker") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Cursed Courtier")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cursed Courtier").error shouldBe null
                game.resolveStack()

                val courtier = game.findPermanent("Cursed Courtier")!!
                withClue("the ETB should have created a Cursed Role") {
                    game.findPermanent("Cursed Role") shouldNotBe null
                }
                val projected = projector.project(game.state)
                withClue("the Cursed Role sets the enchanted creature's base P/T to 1/1") {
                    projected.getPower(courtier) shouldBe 1
                    projected.getToughness(courtier) shouldBe 1
                }
                projected.hasKeyword(courtier, Keyword.LIFELINK) shouldBe true
            }
        }

        context("Voracious Vermin") {

            test("enters making a Rat that can't block") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Voracious Vermin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Voracious Vermin").error shouldBe null
                game.resolveStack()

                game.findPermanent("Rat Token") shouldNotBe null
            }

            test("another creature you control dying puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Voracious Vermin")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vermin = game.findPermanent("Voracious Vermin")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Shock", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Shock kills the other creature you control") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                val projected = projector.project(game.state)
                withClue("the 2/1 Vermin gains a +1/+1 counter → 3/2") {
                    projected.getPower(vermin) shouldBe 3
                    projected.getToughness(vermin) shouldBe 2
                }
            }
        }

        context("Wicked Visitor") {

            test("an enchantment you control hitting the graveyard drains each opponent") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wicked Visitor")
                    .withCardOnBattlefield(1, "Hopeless Nightmare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLife = game.getLifeTotal(2)
                val nightmare = game.findPermanent("Hopeless Nightmare")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nightmare,
                        abilityId = nightmareSacrificeAbility
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("sacrificing the enchantment feeds Wicked Visitor's drain") {
                    game.getLifeTotal(2) shouldBe opponentLife - 1
                }
            }
        }

        context("Knight of Doves") {

            test("an enchantment you control hitting the graveyard makes a 1/1 white flying Bird") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Knight of Doves")
                    .withCardOnBattlefield(1, "Hopeless Nightmare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nightmare = game.findPermanent("Hopeless Nightmare")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nightmare,
                        abilityId = nightmareSacrificeAbility
                    )
                ).error shouldBe null
                game.resolveStack()

                val bird = game.findPermanent("Bird Token")
                withClue("sacrificing the enchantment should feed the Knight's trigger") {
                    bird shouldNotBe null
                }
                val projected = projector.project(game.state)
                withClue("the Bird is a 1/1 flier") {
                    projected.getPower(bird!!) shouldBe 1
                    projected.getToughness(bird) shouldBe 1
                    projected.hasKeyword(bird, Keyword.FLYING) shouldBe true
                }
            }
        }

        context("Gnawing Crescendo") {

            test("gives your creatures +2/+0 until end of turn") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Gnawing Crescendo")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gnawing Crescendo").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = projector.project(game.state)
                withClue("2/2 Grizzly Bears becomes 4/2") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 2
                }
            }

            test("a nontoken creature you control dying this turn creates a Rat that can't block") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Gnawing Crescendo")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gnawing Crescendo").error shouldBe null
                game.resolveStack()

                withClue("no creature has died yet, so no Rat") {
                    game.findPermanent("Rat Token") shouldBe null
                }

                // +2/+0 leaves Grizzly Bears at 4/2, so Shock's 2 damage is still lethal.
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Shock", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the death this turn fires the delayed trigger → a Rat token") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    val rat = game.findPermanent("Rat Token")
                    rat shouldNotBe null
                    game.state.projectedState.cantBlock(rat!!) shouldBe true
                }
            }
        }
    }
}
