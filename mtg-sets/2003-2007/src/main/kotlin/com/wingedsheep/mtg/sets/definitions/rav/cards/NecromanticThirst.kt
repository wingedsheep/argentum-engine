package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Necromantic Thirst
 * {2}{B}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature deals combat damage to a player, you may return target creature
 * card from your graveyard to your hand.
 *
 * The trigger reads one object further out than the Aura itself — [TriggerBinding.ATTACHED] hangs
 * the damage watcher on the enchanted creature rather than on the enchantment, so an unattached
 * Aura never fires. The target is mandatory (chosen as the ability goes on the stack, which per
 * the printed ruling is *after* creatures dealt lethal damage in the same combat have hit the
 * graveyard, so the enchanted creature itself can be the target); only the return is optional.
 */
val NecromanticThirst = card("Necromantic Thirst") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature deals combat damage to a player, you may return target " +
        "creature card from your graveyard to your hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.May(Effects.ReturnToHand(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Brandon Kitkouski"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a46880c-a9f4-452d-8b91-51d8aa97cbd2.jpg?1783943667"
        ruling(
            "2005-10-01",
            "The target is chosen just after any creatures dealt lethal damage at the same time " +
                "that the enchanted creature dealt damage have been put into the graveyard. That " +
                "might include the enchanted creature itself, if it had trample and was blocked, " +
                "for example."
        )
    }
}
