package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `PlayersCantCastSpells(affected = Player.DefendingPlayer)` — "as long as this creature is
 * attacking, defending player can't cast spells" (Wardscale Dragon). The affected player is the one
 * the source creature is attacking, read live from combat: nobody while it isn't attacking, and in
 * multiplayer only the attacked player, never an opponent it isn't attacking.
 */
class DefendingPlayerCantCastSpellsTest : FunSpec({

    val warden = card("Defender-Hushing Drake") {
        manaCost = "{2}"
        typeLine = "Creature — Drake"
        power = 2
        toughness = 2
        oracleText = "As long as this creature is attacking, defending player can't cast spells."
        staticAbility { ability = PlayersCantCastSpells(affected = Player.DefendingPlayer) }
    }
    val instant = card("Hush Test Glint") { manaCost = "{G}"; typeLine = "Instant"; oracleText = "" }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(warden, instant))
        val players = d.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }

    test("the attacked player can't cast spells while the creature attacks; another opponent and the attacker can") {
        val (d, players) = threePlayer()
        val (attacker, attacked, bystander) = players
        d.activePlayer shouldBe attacker
        val drake = d.putCreatureOnBattlefield(attacker, "Defender-Hushing Drake")
        d.removeSummoningSickness(drake)
        val lockedSpell = d.putCardInHand(attacked, "Hush Test Glint")
        val freeSpell = d.putCardInHand(bystander, "Hush Test Glint")
        val ownSpell = d.putCardInHand(attacker, "Hush Test Glint")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(attacker, listOf(drake), attacked).error shouldBe null
        // Mana empties between steps, so it is given once attackers are declared.
        players.forEach { d.giveMana(it, Color.GREEN, 1) }

        withClue("the attacking player is not the defending player") {
            d.state.priorityPlayerId shouldBe attacker
            d.castActionsFor(attacker, ownSpell) shouldHaveSize 1
        }
        d.passPriority(attacker)

        d.state.priorityPlayerId shouldBe attacked
        withClue("the attacked player is offered no cast, and the handler refuses one") {
            d.castActionsFor(attacked, lockedSpell) shouldHaveSize 0
            d.castSpell(attacked, lockedSpell).outcome shouldNotBe Outcome.Done
        }
        d.passPriority(attacked)

        d.state.priorityPlayerId shouldBe bystander
        withClue("an opponent the creature isn't attacking can still cast spells") {
            d.castActionsFor(bystander, freeSpell) shouldHaveSize 1
        }
    }

    test("no one is locked while the creature isn't attacking") {
        val (d, players) = threePlayer()
        val (controller, opponent, _) = players
        d.putCreatureOnBattlefield(controller, "Defender-Hushing Drake")
        val spell = d.putCardInHand(opponent, "Hush Test Glint")
        d.giveMana(opponent, Color.GREEN, 1)

        d.passPriority(controller)
        d.state.priorityPlayerId shouldBe opponent
        d.castActionsFor(opponent, spell) shouldHaveSize 1
    }
})
