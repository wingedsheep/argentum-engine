package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hit It!
 * {1}{R}{G}
 * Enchantment
 *
 * Creatures you control have haste.
 * When this enchantment enters, look at the top five cards of your library. Put a land card from among them onto the battlefield tapped and the rest into your graveyard.
 */
val HitIt = card("Hit It!") {
    manaCost = "{1}{R}{G}"
    typeLine = "Enchantment"
    colorIdentity = "GR"
    oracleText = "Creatures you control have haste.\nWhen this enchantment enters, look at the top five cards of your library. Put a land card from among them onto the battlefield tapped and the rest into your graveyard."
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl)
    }
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Land,
            prompt = "Put a land card from among them onto the battlefield tapped",
            selection = SelectionMode.ChooseExactly(DynamicAmounts.fixed(1)),
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Aaron J. Riley"
        flavorText = "\"I learned the best way to get into a cold stream was to jump right in.\"\n—Captain Christopher Pike"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6266506f-fabf-4b53-8f93-6e2115a3e5a4.jpg?1792508342"
    }
}

