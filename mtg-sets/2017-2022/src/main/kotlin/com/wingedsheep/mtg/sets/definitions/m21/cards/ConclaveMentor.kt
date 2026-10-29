package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement

/**
 * Conclave Mentor
 * {G}{W}
 * Creature — Centaur Cleric
 * 2/2
 *
 * If one or more +1/+1 counters would be put on a creature you control, that many plus one +1/+1
 * counters are put on that creature instead.
 * When this creature dies, you gain life equal to its power.
 *
 * The replacement is Hardened Scales' (default `ModifyCounterPlacement` recipient: a creature you
 * control, whoever places the counters). The dies trigger reads the Mentor's last-known power.
 */
val ConclaveMentor = card("Conclave Mentor") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Centaur Cleric"
    power = 2
    toughness = 2
    oracleText = "If one or more +1/+1 counters would be put on a creature you control, that many plus one " +
        "+1/+1 counters are put on that creature instead.\n" +
        "When this creature dies, you gain life equal to its power."

    replacementEffect(ModifyCounterPlacement(modifier = 1))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
        description = "When this creature dies, you gain life equal to its power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Raoul Vitale"
        flavorText = "\"I have faith in your potential.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a328a93a-e720-46d5-a190-cc65d1c90cea.jpg?1783930665"
        ruling("2020-06-23", "If a creature you control would enter the battlefield with a number of +1/+1 counters on it, it enters with that many plus one instead.")
        ruling("2020-06-23", "If you control two Conclave Mentors, the number of +1/+1 counters put on a creature is two plus the original number. Three Conclave Mentors add three, and so on.")
        ruling("2020-06-23", "Use Conclave Mentor's power as it last existed on the battlefield to determine how much life you gain.")
    }
}
