package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Radagast the Brown
 * {2}{G}{G}
 * Legendary Creature — Avatar Wizard
 * 2/5
 *
 * Whenever Radagast or another nontoken creature you control enters, look at the top X cards
 * of your library, where X is that creature's mana value. You may reveal a creature card that
 * doesn't share a creature type with a creature you control from among those cards and put it
 * into your hand. Put the rest on the bottom of your library in a random order.
 *
 * The trigger fires for Radagast itself too — `TriggerBinding.ANY` over a "nontoken creature
 * you control" filter. X is the *triggering* creature's mana value
 * (`DynamicAmounts.triggeringManaValue()`), so it reads off whichever creature just entered.
 * The reveal-to-hand uses the new `GameObjectFilter.notSharingCreatureTypeWithPermanentYouControl`
 * predicate composed into `Patterns.Library.lookAtTopRevealMatchingToHand`.
 */
val RadagastTheBrown = card("Radagast the Brown") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 2
    toughness = 5
    oracleText = "Whenever Radagast or another nontoken creature you control enters, look at the " +
        "top X cards of your library, where X is that creature's mana value. You may reveal a " +
        "creature card that doesn't share a creature type with a creature you control from among " +
        "those cards and put it into your hand. Put the rest on the bottom of your library in a " +
        "random order."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().nontoken(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmounts.triggeringManaValue(),
            filter = GameObjectFilter.Creature
                .notSharingCreatureTypeWithPermanentYouControl(GameObjectFilter.Creature),
            prompt = "You may reveal a creature card that doesn't share a creature type with a " +
                "creature you control and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "184"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3988120-ebbe-4d24-9bb4-8c5331a14034.jpg?1686969557"
    }
}
