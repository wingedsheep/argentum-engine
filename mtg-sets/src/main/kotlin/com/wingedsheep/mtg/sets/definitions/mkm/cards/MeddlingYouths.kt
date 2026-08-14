package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.YouAttackEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Meddling Youths — Murders at Karlov Manor #219
 * {3}{R}{W} · Creature — Human Detective · 4/5
 *
 * Haste
 * Whenever you attack with three or more creatures, investigate.
 *
 * `YouAttackEvent(minAttackers = 3)` bound ANY — the Seasoned Consultant shape. The Youths need
 * not be among the three attackers, and the trigger fires once per declare-attackers rather than
 * once per attacker.
 */
val MeddlingYouths = card("Meddling Youths") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Detective"
    power = 4
    toughness = 5
    oracleText = "Haste\n" +
        "Whenever you attack with three or more creatures, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = TriggerSpec(YouAttackEvent(minAttackers = 3), TriggerBinding.ANY)
        effect = Effects.Investigate()
        description = "Whenever you attack with three or more creatures, investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Matt Forsyth"
        flavorText = "\"Well, if it isn't mean old Mr. Larchbottom, who's always been so eager to have " +
            "the only dumpling stall in Oxblood Alley.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af12417c-b082-4379-a850-c72e2652c6fb.jpg?1783912842"
    }
}
