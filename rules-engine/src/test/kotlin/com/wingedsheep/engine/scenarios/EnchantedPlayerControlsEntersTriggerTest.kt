package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A zone-change trigger whose filter is scoped by `ControllerPredicate.ControlledByReferencedPlayer`
 * — here `controlledByEnchantedPlayer()`, "whenever a creature enchanted player controls enters"
 * (Trespasser's Curse). The referenced player is resolved off the trigger's source Aura, so only
 * creatures entering under the enchanted player's control fire it: not the Aura controller's, and
 * not a third player's.
 */
class EnchantedPlayerControlsEntersTriggerTest : FunSpec({

    val curse = card("Test Trespass Curse") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura Curse"
        auraTarget = Targets.Player
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.controlledByEnchantedPlayer()).enters()
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EnchantedPlayer)) then Effects.GainLife(1)
        }
    }

    val flashBear = card("Test Flash Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.FLASH)
    }

    test("only a creature entering under the enchanted player's control fires the trigger") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(curse, flashBear))
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1

        val aura = d.putPermanentOnBattlefield(p1, curse.name)
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(p3)) })

        // Each player puts a creature onto the stack; p1 first, then p2 and p3 at instant speed.
        for (player in listOf(p1, p2, p3)) {
            val bear = d.putCardInHand(player, flashBear.name)
            d.giveMana(player, Color.GREEN, 1)
            d.castSpell(player, bear).error shouldBe null
            if (player != p3) d.passPriority(player)
        }
        var guard = 0
        while (d.stackSize > 0 && guard++ < 30) d.passPriority(d.state.priorityPlayerId!!)

        withClue("enchanted player drained once, for their own creature only") { d.getLifeTotal(p3) shouldBe 19 }
        withClue("Aura controller gains once") { d.getLifeTotal(p1) shouldBe 21 }
        withClue("a third player's creature doesn't fire it") { d.getLifeTotal(p2) shouldBe 20 }
    }
})
