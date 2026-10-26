package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.JourneyToEternity
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Journey to Eternity // Atzal, Cave of Eternity (RIX #160).
 *
 *   Front — Enchant creature you control. When enchanted creature dies, return it to the
 *           battlefield under your control, then return this card to the battlefield transformed
 *           under your control.
 *   Back  — {T}: Add one mana of any color. {3}{B}{G}, {T}: Return target creature card from your
 *           graveyard to the battlefield.
 *
 * The rulings this pins: both cards come back even though the Aura was put into the graveyard by
 * state-based action after the creature died, and a creature you control but don't own returns
 * under your control from its owner's graveyard — as does a Journey you control but don't own.
 */
class JourneyToEternityScenarioTest : ScenarioTestBase() {

    private val wipe = card("Wipe Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures and enchantments."
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature or GameObjectFilter.Enchantment) }
    }

    init {
        cardRegistry.register(listOf(wipe))

        context("Journey to Eternity") {

            test("when the enchanted creature dies, it and the Aura return — the Aura as Atzal") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Journey to Eternity", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the creature came back") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
                withClue("the Aura came back on its land face, untapped") {
                    game.isInGraveyard(1, "Journey to Eternity") shouldBe false
                    val atzal = game.findPermanent("Atzal, Cave of Eternity").shouldNotBeNull()
                    game.state.projectedState.getController(atzal) shouldBe game.player1Id
                    game.state.getEntity(atzal)!!.has<TappedComponent>() shouldBe false
                }
            }

            test("when the Aura and the creature are destroyed together, both still return") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Journey to Eternity", "Grizzly Bears")
                    .withCardInHand(1, "Wipe Test Spell")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wipe Test Spell").error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.findPermanent("Atzal, Cave of Eternity").shouldNotBeNull()
            }

            test("cards you control but don't own come back under your control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // owned by P2
                    .withCardAttachedTo(2, "Journey to Eternity", "Grizzly Bears") // owned by P2
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val journey = game.findPermanent("Journey to Eternity")!!
                // Player 1 has taken both; the dies trigger is theirs.
                game.state = game.state
                    .updateEntity(bears) { it.with(ControllerComponent(game.player1Id)) }
                    .updateEntity(journey) { it.with(ControllerComponent(game.player1Id)) }

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val returnedBears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val atzal = game.findPermanent("Atzal, Cave of Eternity").shouldNotBeNull()
                game.state.projectedState.getController(returnedBears) shouldBe game.player1Id
                game.state.projectedState.getController(atzal) shouldBe game.player1Id
            }

            test("Atzal returns a creature card from your graveyard to the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Journey to Eternity", "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val atzal = game.findPermanent("Atzal, Cave of Eternity").shouldNotBeNull()
                val reanimate = JourneyToEternity.backFace!!.activatedAbilities.first { !it.isManaAbility }.id
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = atzal,
                        abilityId = reanimate,
                        targets = listOf(ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.findPermanent("Hill Giant").shouldNotBeNull()
                game.state.getEntity(atzal)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
