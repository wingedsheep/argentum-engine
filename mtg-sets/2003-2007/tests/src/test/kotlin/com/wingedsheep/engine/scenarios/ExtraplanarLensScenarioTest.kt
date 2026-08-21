package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.ExtraplanarLens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Extraplanar Lens (MRD #169) — "Imprint — When this artifact enters, you may exile target land you
 * control. Whenever a land with the same name as the exiled card is tapped for mana, its controller
 * adds one mana of any type that land produced."
 *
 * What these tests pin is the `SharesNameWith(LinkedExiledCard)` filter, in the four states that can
 * get it wrong: the name matches, the name doesn't, nothing was imprinted at all (the filter has to
 * fail closed rather than match every land), and — the one most implementations get backwards — the
 * matching land belongs to an **opponent**, who then gets the bonus themselves. The printed card is
 * symmetric; only the choice of which basic to exile makes it one-sided.
 */
class ExtraplanarLensScenarioTest : FunSpec({

    // Built once, during spec construction: `TestCards.all` forces a ClassGraph scan of the
    // whole card corpus, and paying that inside the first test body puts it under the per-test
    // timeout — which is what makes a single-spec run flake on a loaded machine.
    val cards = TestCards.all + ExtraplanarLens

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Cast the Lens from hand so its imprint trigger fires, target [land] (when accepting), and
     * answer the "you may" with [accept].
     */
    fun GameTestDriver.castLens(land: EntityId, accept: Boolean) {
        val inHand = putCardInHand(player1, "Extraplanar Lens")
        giveColorlessMana(player1, 3)
        castSpell(player1, inHand).error shouldBe null

        // An `optional` trigger that also targets is asked in that order: the "you may" first
        // (`TriggerProcessor.processMayThenTargetTrigger` unwraps the consent gate *before* putting
        // the ability on the stack), then its target. So the first yes/no is the imprint gate.
        //
        // Stop once the ability has left the stack — the exile happens on resolution, and passing
        // beyond it would carry the turn over and take priority away from the player who then needs
        // to tap a land.
        repeat(24) {
            val decision = state.pendingDecision
            when {
                decision is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                decision is ChooseTargetsDecision ->
                    submitTargetSelection(player1, listOf(land)).error shouldBe null
                decision != null -> error("unexpected decision ${decision::class.simpleName}")
                stackSize == 0 && findPermanent(player1, "Extraplanar Lens") != null -> return
                else -> bothPass()
            }
        }
        error("the Lens never finished resolving")
    }

    /** Tap [land] for mana, going through the real mana-ability path so the bonus can attach. */
    fun GameTestDriver.tapForMana(player: EntityId, land: EntityId, cardName: String) {
        val manaAbility = cardRegistry.getCard(cardName)!!
            .script.activatedAbilities.first { it.isManaAbility }
        submit(ActivateAbility(player, land, manaAbility.id)).error shouldBe null
    }

    fun GameTestDriver.manaPool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("a land with the same name as the exiled card produces an extra mana of its own type") {
        val d = driver()
        val exiled = d.putLandOnBattlefield(d.player1, "Forest")
        d.castLens(exiled, accept = true)

        withClue("the Forest was exiled with the Lens") {
            d.getExileCardNames(d.player1) shouldBe listOf("Forest")
        }

        val forest = d.putLandOnBattlefield(d.player1, "Forest")
        d.tapForMana(d.player1, forest, "Forest")

        withClue("one from the Forest plus the Lens's mirrored bonus") {
            d.manaPool(d.player1).green shouldBe 2
        }
    }

    test("a land whose name doesn't match the exiled card gets no bonus") {
        val d = driver()
        val exiled = d.putLandOnBattlefield(d.player1, "Forest")
        d.castLens(exiled, accept = true)

        val island = d.putLandOnBattlefield(d.player1, "Island")
        d.tapForMana(d.player1, island, "Island")

        withClue("the filter is by name, not by 'any land you control'") {
            d.manaPool(d.player1).blue shouldBe 1
            d.manaPool(d.player1).green shouldBe 0
        }
    }

    test("an opponent's matching land gets the bonus, and it goes to that opponent") {
        // "Whenever a land with the same name as the exiled card is tapped for mana, ITS CONTROLLER
        // adds …" — the printed card has no "you control" anywhere, so it doubles for everyone
        // running the same basic. Encoding the filter as `youControl()` would silently turn a
        // symmetric card into a one-sided one, and this is the test that would catch it.
        val d = driver()
        val exiled = d.putLandOnBattlefield(d.player1, "Forest")
        d.castLens(exiled, accept = true)

        val opponentForest = d.putLandOnBattlefield(d.player2, "Forest")
        // A mana ability needs priority, and it is player 1's turn — hand it over.
        d.passPriority(d.player1)
        d.state.priorityPlayerId shouldBe d.player2
        d.tapForMana(d.player2, opponentForest, "Forest")

        withClue("the bonus is added to the tapping player's pool, not the Lens controller's") {
            d.manaPool(d.player2).green shouldBe 2
            d.manaPool(d.player1).green shouldBe 0
        }
    }

    test("declining the imprint leaves the doubling half inert") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player1, "Forest")
        d.castLens(land, accept = false)

        withClue("nothing was exiled") {
            d.getExileCardNames(d.player1) shouldBe emptyList()
        }

        d.tapForMana(d.player1, land, "Forest")

        withClue("an empty pile gives the name filter nothing to match — it must fail closed") {
            d.manaPool(d.player1).green shouldBe 1
        }
    }
})
