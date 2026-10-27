package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CreaturesAttackYouEvent(attackedPlayer = Player.EnchantedPlayer)` — "whenever enchanted player
 * is attacked" (`Triggers.player(Player.EnchantedPlayer).isAttacked()`). The trigger is the Aura
 * controller's, but only attackers declared against the player the Aura enchants count, and they
 * are captured as the firing's collection like the "you" form's. An Aura enchanting no player
 * never fires.
 */
class EnchantedPlayerIsAttackedTriggerTest : FunSpec({

    /** "Whenever enchanted player is attacked, the attacking player loses 1 life for each attacker." */
    val curse = card("Test Attacked Curse") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura Curse"
        auraTarget = Targets.Player
        triggeredAbility {
            trigger = Triggers.player(Player.EnchantedPlayer).isAttacked()
            effect = Effects.LoseLife(
                DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured),
                EffectTarget.PlayerRef(Player.TriggeringPlayer)
            )
        }
    }

    val walker = card("Test Attacked Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(curse, walker))
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    fun GameTestDriver.ready(player: EntityId): EntityId =
        putCreatureOnBattlefield(player, "Grizzly Bears").also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    test("fires for its controller and captures only the attackers aimed at the enchanted player") {
        val (d, players) = setup()
        val (p1, p2, p3) = players
        val aura = d.putPermanentOnBattlefield(p2, curse.name)
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(p3)) })
        val atCursed = List(2) { d.ready(p1) }
        val atController = d.ready(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(atCursed[0] to p3, atCursed[1] to p3, atController to p2)).error shouldBe null
        withClue("one firing per combat") { d.stackSize shouldBe 1 }
        d.resolveAll()
        withClue("two attackers at the enchanted player; the one at the Aura's controller doesn't count") {
            d.getLifeTotal(p1) shouldBe 18
        }
    }

    test("an attack on the Aura's controller alone is not an attack on the enchanted player") {
        val (d, players) = setup()
        val (p1, p2, p3) = players
        val aura = d.putPermanentOnBattlefield(p2, curse.name)
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(p3)) })
        val bears = d.ready(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2)).error shouldBe null
        d.stackSize shouldBe 0
    }

    test("an attack on only a planeswalker the enchanted player controls doesn't count") {
        val (d, players) = setup()
        val (p1, p2, p3) = players
        val aura = d.putPermanentOnBattlefield(p2, curse.name)
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(p3)) })
        val planeswalker = d.putPermanentOnBattlefield(p3, walker.name)
        d.replaceState(d.state.updateEntity(planeswalker) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        val bears = d.ready(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to planeswalker)).error shouldBe null
        d.stackSize shouldBe 0
    }

    test("an Aura that enchants no player never fires") {
        val (d, players) = setup()
        val (p1, p2, _) = players
        d.putPermanentOnBattlefield(p2, curse.name)
        val bears = d.ready(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2)).error shouldBe null
        d.stackSize shouldBe 0
    }
})
