package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Biting-Palm Ninja — Kamigawa: Neon Dynasty #88 (canonical printing)
 * {2}{B} · Creature — Human Ninja · 3/3
 *
 * Ninjutsu {2}{B}
 * This creature enters with a menace counter on it.
 * Whenever this creature deals combat damage to a player, you may remove a menace counter from it.
 * When you do, that player reveals their hand and you choose a nonland card from it. Exile that card.
 *
 * The "when you do" is a CR 603.12 reflexive trigger. With no menace counter left to remove (already
 * spent, or the Ninja has left the battlefield) the action can't be performed, so the may-prompt is
 * absent and nothing is exiled. "That player" is the damaged player, carried as the triggering player.
 */
val BitingPalmNinja = card("Biting-Palm Ninja") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Ninja"
    power = 3
    toughness = 3
    oracleText = "Ninjutsu {2}{B} ({2}{B}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "This creature enters with a menace counter on it.\n" +
        "Whenever this creature deals combat damage to a player, you may remove a menace counter from " +
        "it. When you do, that player reveals their hand and you choose a nonland card from it. Exile " +
        "that card."

    ninjutsu("{2}{B}")

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.MENACE,
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ReflexiveTrigger(
            action = Effects.RemoveCounters(CounterType.MENACE, 1, EffectTarget.Self),
            optional = true,
            reflexiveEffect = Patterns.Hand.revealHandAndExileChosen(
                target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
            ),
            descriptionOverride = "You may remove a menace counter from this creature. When you do, " +
                "that player reveals their hand and you choose a nonland card from it. Exile that card."
        )
        description = "Whenever this creature deals combat damage to a player, you may remove a menace " +
            "counter from it. When you do, that player reveals their hand and you choose a nonland card " +
            "from it. Exile that card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3017df51-67ae-4c3d-8210-086f83bc2040.jpg?1783923890"
    }
}
