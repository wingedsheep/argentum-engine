package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Agent 13, Sharon Carter — Marvel Super Heroes #1
 * {2}{W} · Legendary Creature — Human Spy Hero · 3/2
 *
 * Whenever a creature you control attacks alone, investigate.
 *
 * The trigger watches *any* creature you control (ANY binding + "creature you control" filter),
 * not just Sharon herself — the Squall, SeeD Mercenary / Widow's Walk "attacks alone" shape.
 * Investigate is the CR 701.36 keyword action: create a Clue token
 * ([Effects.Investigate] → predefined "Clue" token).
 */
val Agent13SharonCarter = card("Agent 13, Sharon Carter") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Spy Hero"
    power = 3
    toughness = 2
    oracleText = "Whenever a creature you control attacks alone, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Investigate()
        description = "Whenever a creature you control attacks alone, investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Michael MacRae"
        flavorText = "\"Coulson, I'm in, but the intel was wrong. No, no extraction. This is bigger " +
            "than we thought!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/0/107cb521-9806-47dc-92d8-b43112b63caa.jpg?1783902979"
    }
}
