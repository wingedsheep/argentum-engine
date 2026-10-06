package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Spitfire
 * {2}{R}
 * Creature — Elemental
 * 1/3
 *
 * Flying
 * Whenever an opponent is dealt noncombat damage, this creature gets +3/+0 until end of turn.
 *
 * "An opponent is dealt damage" from any source is War Elemental's observer shape
 * (`Triggers.a().dealsDamage(Recipient.Opponent)`), narrowed to noncombat damage. It fires once
 * per damage event per damaged opponent, matching the rulings.
 */
val ChandrasSpitfire = card("Chandra's Spitfire") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever an opponent is dealt noncombat damage, this creature gets +3/+0 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a().dealsDamage(Recipient.Opponent, damageType = DamageType.NonCombat)
        effect = Effects.ModifyStats(3, 0, EffectTarget.Self)
        description = "Whenever an opponent is dealt noncombat damage, this creature gets +3/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Justin Sweet"
        flavorText = "\"I've lit most everything on fire—trees, rocks, even the water. Now it's time to burn the clouds.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84a460bb-276f-45f7-b0da-8457eb3192eb.jpg?1783941809"

        ruling(
            "2019-07-12",
            "Combat damage is the damage that's dealt automatically by attacking and blocking creatures. " +
                "Any other damage is noncombat damage, even if it's dealt during a combat phase by an " +
                "attacking or blocking creature."
        )
        ruling(
            "2019-07-12",
            "The last ability of Chandra's Spitfire triggers just once for each event in which an opponent " +
                "is dealt noncombat damage, regardless of how much damage that player is dealt."
        )
        ruling(
            "2019-07-12",
            "In a multiplayer game, if a source deals damage to multiple opponents at the same time, the " +
                "last ability of Chandra's Spitfire will trigger as many times as there are opponents who " +
                "were dealt damage."
        )
    }
}
