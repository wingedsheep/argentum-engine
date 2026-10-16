package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Piper Wright, Publick Reporter — Fallout #36
 * {1}{U} · Legendary Creature — Human Detective · 1/2
 *
 * Whenever Piper Wright deals combat damage to a player, investigate that many times.
 * Whenever you sacrifice a Clue, put a +1/+1 counter on target creature you control.
 *
 * "That many" is the combat damage dealt, read off the trigger event. The Clue half is a
 * per-object trigger ("a Clue", CR 603.2c) matching any Clue artifact, token or not, and fires on
 * every sacrifice for any reason — not only a Clue's own draw ability (per the rulings).
 */
val PiperWrightPublickReporter = card("Piper Wright, Publick Reporter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Detective"
    oracleText = "Whenever Piper Wright deals combat damage to a player, investigate that many times. " +
        "(To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: " +
        "Draw a card.\")\n" +
        "Whenever you sacrifice a Clue, put a +1/+1 counter on target creature you control."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Investigate(DynamicAmounts.triggerDamageAmount())
        description = "Whenever Piper Wright deals combat damage to a player, investigate that many times."
    }

    triggeredAbility {
        val creatureYouControl = target(TargetFilter.CreatureYouControl)
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Clue"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creatureYouControl)
        description = "Whenever you sacrifice a Clue, put a +1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Rafater"
        flavorText = "\"You can't stop the press.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/916bf331-455a-4694-93d3-15dcf6971072.jpg?1783912412"

        ruling(
            "2024-03-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token. For example, if you sacrifice a nontoken Clue artifact, such as Candy Trail " +
                "from the Wilds of Eldraine set, Piper Wright's last ability will trigger."
        )
        ruling(
            "2024-03-08",
            "Piper Wright's last ability triggers whenever you sacrifice a Clue for any reason, not " +
                "just to activate a Clue's activated ability."
        )
    }
}
