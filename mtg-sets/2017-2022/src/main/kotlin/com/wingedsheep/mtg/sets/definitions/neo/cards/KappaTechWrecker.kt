package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kappa Tech-Wrecker — Kamigawa: Neon Dynasty #198 (canonical printing)
 * {1}{G} · Creature — Turtle Ninja · 1/3
 *
 * Ninjutsu {1}{G}
 * This creature enters with a deathtouch counter on it.
 * Whenever this creature deals combat damage to a player, you may remove a deathtouch counter from it.
 * When you do, exile target artifact or enchantment that player controls.
 *
 * The "when you do" is a CR 603.12 reflexive trigger, shaped like Biting-Palm Ninja's counter removal
 * and Dokuchi Silencer's targeted follow-up. With no deathtouch counter left to remove the action can't
 * be performed, so nothing is exiled. "That player" is the damaged player, carried as the triggering
 * player, so the target is scoped with `controlledByTriggeringPlayer()`.
 */
val KappaTechWrecker = card("Kappa Tech-Wrecker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Turtle Ninja"
    power = 1
    toughness = 3
    oracleText = "Ninjutsu {1}{G} ({1}{G}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "This creature enters with a deathtouch counter on it.\n" +
        "Whenever this creature deals combat damage to a player, you may remove a deathtouch counter " +
        "from it. When you do, exile target artifact or enchantment that player controls."

    ninjutsu("{1}{G}")

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.DEATHTOUCH,
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ReflexiveTrigger(
            action = Effects.RemoveCounters(CounterType.DEATHTOUCH, 1, EffectTarget.Self),
            optional = true,
            descriptionOverride = "You may remove a deathtouch counter from this creature. When you do, " +
                "exile target artifact or enchantment that player controls."
        ) {
            val victim = target(
                TargetFilter(GameObjectFilter.ArtifactOrEnchantment.controlledByTriggeringPlayer())
            )
            effect = Effects.Exile(victim)
        }
        description = "Whenever this creature deals combat damage to a player, you may remove a deathtouch " +
            "counter from it. When you do, exile target artifact or enchantment that player controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3a7bc69-4500-4e7e-94e4-67b85597bd82.jpg?1783923845"
    }
}
