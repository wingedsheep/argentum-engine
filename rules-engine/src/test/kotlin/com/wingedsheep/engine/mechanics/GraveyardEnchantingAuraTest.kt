package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zone-spanning Aura attachment and a mutable enchant restriction — the reanimation-Aura idiom,
 * exercised through Animate Dead ("Enchant creature card in a graveyard … it loses 'enchant creature
 * card in a graveyard' and gains 'enchant creature put onto the battlefield with this Aura.' Return
 * enchanted creature card to the battlefield under your control and attach this Aura to it.").
 *
 * Rules pinned here:
 *  - CR 303.4a / 608.3c: the Aura spell targets a card in a graveyard and enters attached to it.
 *  - CR 303.4c / 704.5m: the enchant state-based action keeps an Aura attached to a card off the
 *    battlefield while that card matches its enchant ability, and puts it into the graveyard once
 *    the card has left that zone (CR 701.3d: leaving the zone unattaches it).
 *  - CR 603.4: "if it's on the battlefield" is an intervening if — nothing returns if the Aura is gone.
 *  - The gained enchant ability admits only the object put onto the battlefield (CR 400.7), so the
 *    Aura can't be moved to another creature, and protection from black keeps it from attaching at
 *    all — it goes to the graveyard and its delayed trigger sacrifices the creature (Animate Dead
 *    rulings, 2016-06-08).
 */
class GraveyardEnchantingAuraTest : ScenarioTestBase() {

    private fun TestGame.castAnimateDeadOn(cardId: EntityId, ownerId: EntityId) {
        val aura = state.getHand(player1Id).single { cardName(it) == "Animate Dead" }
        execute(CastSpell(player1Id, aura, listOf(ChosenTarget.Card(cardId, ownerId, Zone.GRAVEYARD)))).error shouldBe null
    }

    private fun TestGame.cardName(id: EntityId): String? =
        state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name

    private fun board(creature: String, vararg extraHand: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Animate Dead")
        .apply { extraHand.forEach { withCardInHand(1, it) } }
        .withCardInGraveyard(2, creature)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Tormod's Crypt")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the server offers the Aura spell with the creature cards in every graveyard as its targets") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            val aura = game.state.getHand(game.player1Id).single { game.cardName(it) == "Animate Dead" }
            val cast = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == aura }
            val targets = cast.targetRequirements?.flatMap { it.validTargets } ?: cast.validTargets.orEmpty()
            targets shouldBe listOf(giant)
        }

        test("the Aura enters attached to the graveyard card and the enchant SBA leaves it there") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDeadOn(giant, game.player2Id)

            // Both players pass: the Aura spell resolves, its enters trigger goes on the stack and
            // state-based actions are checked before anyone gets priority.
            game.passPriority()
            game.passPriority()

            val aura = game.findPermanent("Animate Dead")!!
            game.state.stack.size shouldBe 1
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe giant
            (giant in game.state.getGraveyard(game.player2Id)) shouldBe true
            game.state.getEntity(giant)?.get<AttachmentsComponent>()?.attachedIds shouldBe listOf(aura)
        }

        test("the return swaps the enchant ability: the Aura enchants only the creature it put onto the battlefield") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDeadOn(giant, game.player2Id)
            game.resolveStack()

            val aura = game.findPermanent("Animate Dead")!!
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe giant
            game.state.getEntity(aura)?.get<GainedEnchantRestrictionComponent>() shouldNotBe null
            game.state.projectedState.getPower(giant) shouldBe 2
            game.state.projectedState.getToughness(giant) shouldBe 3

            // "Attempting to move Animate Dead to another creature won't work."
            val bears = game.findPermanent("Grizzly Bears")!!
            AttachmentMover.canAttach(game.state, services.predicateEvaluator, cardRegistry, aura, bears) shouldBe false
            AttachmentMover.canAttach(game.state, services.predicateEvaluator, cardRegistry, aura, giant) shouldBe true
        }

        test("the card leaving the graveyard in response unattaches the Aura; it is graveyarded and nothing returns") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDeadOn(giant, game.player2Id)
            game.passPriority()
            game.passPriority()
            game.state.stack.size shouldBe 1

            game.passPriority() // player 1 passes with the enters trigger on the stack
            game.execute(ActivateAbility(
                playerId = game.player2Id,
                sourceId = game.findPermanent("Tormod's Crypt")!!,
                abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player2Id))
            )).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Animate Dead") shouldBe false
            game.isInGraveyard(1, "Animate Dead") shouldBe true
        }

        test("an Aura spell whose graveyard target is gone doesn't resolve") {
            val game = board("Hill Giant")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDeadOn(giant, game.player2Id)
            game.passPriority()
            game.execute(ActivateAbility(
                playerId = game.player2Id,
                sourceId = game.findPermanent("Tormod's Crypt")!!,
                abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player2Id))
            )).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Animate Dead") shouldBe false
            game.isInGraveyard(1, "Animate Dead") shouldBe true
        }

        test("protection from black: the Aura can't attach, goes to the graveyard, and the creature is sacrificed") {
            val game = board("White Knight")
            val knight = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDeadOn(knight, game.player2Id)
            game.resolveStack()

            game.isOnBattlefield("Animate Dead") shouldBe false
            game.isInGraveyard(1, "Animate Dead") shouldBe true
            game.isOnBattlefield("White Knight") shouldBe false
            game.isInGraveyard(2, "White Knight") shouldBe true
        }

        test("the creature dying first takes the Aura with it and the leave trigger finds nothing to sacrifice") {
            val game = board("Hill Giant", "Terror")
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDeadOn(giant, game.player2Id)
            game.resolveStack()
            game.isOnBattlefield("Hill Giant") shouldBe true

            game.castSpell(1, "Terror", giant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Animate Dead") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.stack.size shouldBe 0
        }
    }
}
