package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Éomer, King of Rohan
 * {3}{R}{W}
 * Legendary Creature — Human Noble
 * 2/2
 * Double strike
 * Éomer enters with a +1/+1 counter on it for each other Human you control.
 * When Éomer enters, target player becomes the monarch. Éomer deals damage equal to its power to
 * any target.
 *
 * The counters are Unbreathing Horde's [EntersWithDynamicCounters] over a battlefield count of
 * Humans you control, `notSourceItself()` for "each other". The enters trigger takes two targets
 * (a player for [Effects.BecomeMonarch], then any target) and reads Éomer's power on resolution,
 * counters included.
 */
val EomerKingOfRohan = card("Éomer, King of Rohan") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Noble"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "Éomer enters with a +1/+1 counter on it for each other Human you control.\n" +
        "When Éomer enters, target player becomes the monarch. Éomer deals damage equal to its " +
        "power to any target."

    keywords(Keyword.DOUBLE_STRIKE)

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.count(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Any.withSubtype(Subtype.HUMAN).notSourceItself()
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        val any = target(Targets.Any)
        effect = Effects.BecomeMonarch(player) then
            Effects.DealDamage(DynamicAmounts.sourcePower(), any)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Leonardo Borazio"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f48930a-d7bd-410b-995a-4e2837aabb25.jpg?1783916021"
        ruling("2023-06-16", "The game starts with no monarch. Once an effect makes one player the monarch, the game will have exactly one monarch from that point forward. As a player becomes the monarch, the current monarch (if any) ceases being the monarch.")
        ruling("2023-06-16", "There are two inherent triggered abilities associated with being the monarch. These triggered abilities have no source and are controlled by the player who was the monarch at the time the abilities triggered. The full texts of these abilities are \"At the beginning of the monarch's end step, that player draws a card\" and \"Whenever a creature deals combat damage to the monarch, its controller becomes the monarch.\"")
    }
}
