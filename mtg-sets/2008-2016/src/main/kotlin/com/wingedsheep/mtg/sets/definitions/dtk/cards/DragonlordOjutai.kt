package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dragonlord Ojutai {3}{W}{U}
 * Legendary Creature — Elder Dragon
 * 5/4
 *
 * Flying
 * Dragonlord Ojutai has hexproof as long as it's untapped.
 * Whenever Dragonlord Ojutai deals combat damage to a player, look at the top three cards of your
 * library. Put one of them into your hand and the rest on the bottom of your library in any order.
 *
 * The hexproof is Walking Skyscraper's untapped-conditional self grant (it switches off the moment
 * Ojutai taps to attack, unless something gives it vigilance); the combat-damage trigger resolves
 * Anticipate's look-at-three, keep-one pipeline.
 */
val DragonlordOjutai = card("Dragonlord Ojutai") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "Dragonlord Ojutai has hexproof as long as it's untapped.\n" +
        "Whenever Dragonlord Ojutai deals combat damage to a player, look at the top three cards of " +
        "your library. Put one of them into your hand and the rest on the bottom of your library in " +
        "any order."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.SourceIsUntapped
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
        description = "Whenever Dragonlord Ojutai deals combat damage to a player, look at the top three " +
            "cards of your library. Put one of them into your hand and the rest on the bottom of your " +
            "library in any order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a42b2c9c-1650-468e-94d6-e09b3518447f.jpg?1783938573"
    }
}
