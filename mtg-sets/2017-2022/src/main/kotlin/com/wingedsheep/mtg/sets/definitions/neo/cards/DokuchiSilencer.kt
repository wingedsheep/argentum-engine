package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dokuchi Silencer — Kamigawa: Neon Dynasty #95 (canonical printing)
 * {1}{B} · Creature — Human Ninja · 2/1
 *
 * Ninjutsu {1}{B}
 * Whenever this creature deals combat damage to a player, you may discard a creature card. When you
 * do, destroy target creature or planeswalker that player controls.
 *
 * The "when you do" is a CR 603.12 reflexive trigger: the discard is the optional action, and with no
 * creature card in hand it can't be performed, so nothing is destroyed. "That player" is the damaged
 * player, carried into the reflexive ability as the triggering player, so the target is scoped with
 * `controlledByTriggeringPlayer()` (not merely "an opponent", which differs in multiplayer).
 */
val DokuchiSilencer = card("Dokuchi Silencer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Ninja"
    power = 2
    toughness = 1
    oracleText = "Ninjutsu {1}{B} ({1}{B}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, you may discard a creature card. When " +
        "you do, destroy target creature or planeswalker that player controls."

    ninjutsu("{1}{B}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ReflexiveTrigger(
            action = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Creature),
            optional = true,
            descriptionOverride = "You may discard a creature card. When you do, destroy target creature " +
                "or planeswalker that player controls."
        ) {
            val victim = target(
                TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.controlledByTriggeringPlayer())
            )
            effect = Effects.Destroy(victim)
        }
        description = "Whenever this creature deals combat damage to a player, you may discard a creature " +
            "card. When you do, destroy target creature or planeswalker that player controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a24d5664-e214-466d-bb53-4e8f6f75a009.jpg?1783923888"
    }
}
