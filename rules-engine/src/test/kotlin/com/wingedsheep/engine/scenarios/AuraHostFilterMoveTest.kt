package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `MoveCollectionEffect.auraHostFilter` — an Aura card put onto the battlefield without being cast
 * still has its host chosen by the player putting it there (CR 303.4f), but only among permanents
 * that also match the filter; one with no host in that intersection stays where it is (CR 303.4g).
 *
 * Also pins the tracked half of the same move: every host is chosen before anything moves, so the
 * non-Aura cards and the Auras enter together and all of them are published under `storeMovedAs`
 * for a later pipeline step.
 */
class AuraHostFilterMoveTest : ScenarioTestBase() {

    init {
        // "Return all permanent cards from your graveyard to the battlefield, Auras attached to
        //  creatures you control. You gain 1 life for each card returned this way."
        cardRegistry.register(card("Host Filter Recall") {
            manaCost = "{B}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.Pipeline {
                    val cards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Permanent))
                    val returned = moveTracked(
                        cards,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        auraHostFilter = GameObjectFilter.Creature.youControl()
                    )
                    run(Effects.ForEachInCollection(returned, Effects.GainLife(1)))
                }
            }
        })
        // The same move with no host filter: every host the Aura's enchant ability allows.
        cardRegistry.register(card("Unfiltered Recall") {
            manaCost = "{B}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.Pipeline {
                    val cards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Permanent))
                    move(cards, CardDestination.ToZone(Zone.BATTLEFIELD))
                }
            }
        })

        fun game(spell: String, withWildGrowth: Boolean = true) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, spell)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInGraveyard(1, "Holy Strength")
            .withCardInGraveyard(1, "Raging Goblin")
            .apply { if (withWildGrowth) withCardInGraveyard(1, "Wild Growth") }
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("auraHostFilter") {
            test("only hosts matching the filter are offered, and an Aura with none stays put") {
                val game = game("Host Filter Recall")
                val holyStrength = game.findCardsInGraveyard(1, "Holy Strength").single()
                game.castSpell(1, "Host Filter Recall").error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the opponent's Grizzly Bears is a legal 'enchant creature' host, but not a creature you " +
                    "control; Raging Goblin enters with the Aura, so it can't host it either") {
                    decision.legalTargets.values.flatten() shouldBe listOf(giant)
                }
                withClue("a host outside the filter is refused") {
                    (game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error != null) shouldBe true
                }
                game.selectTargets(listOf(giant)).error shouldBe null

                game.state.getEntity(holyStrength)?.get<AttachedToComponent>()?.targetId shouldBe giant
                withClue("Wild Growth enchants a land, and no land is a creature you control (CR 303.4g)") {
                    game.isInGraveyard(1, "Wild Growth") shouldBe true
                }
            }

            test("the tracked move publishes every card that entered") {
                val game = game("Host Filter Recall")
                val lifeBefore = game.getLifeTotal(1)
                game.castSpell(1, "Host Filter Recall").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null

                withClue("Raging Goblin and Holy Strength returned; Wild Growth stayed behind") {
                    game.isOnBattlefield("Raging Goblin") shouldBe true
                    game.getLifeTotal(1) shouldBe lifeBefore + 2
                }
            }

            test("without a filter every host the enchant ability allows is offered") {
                val game = game("Unfiltered Recall", withWildGrowth = false)
                game.castSpell(1, "Unfiltered Recall").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets.values.flatten().toSet() shouldBe
                    setOf(game.findPermanent("Hill Giant")!!, game.findPermanent("Grizzly Bears")!!)
            }
        }
    }
}
