package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Merry, Esquire of Rohan
 * {R}{W}
 * Legendary Creature — Halfling Knight
 * 2/2
 *
 * Haste
 * Merry has first strike as long as it's equipped.
 * Whenever you attack with Merry and another legendary creature, draw a card.
 */
val MerryEsquireOfRohan = card("Merry, Esquire of Rohan") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Halfling Knight"
    power = 2
    toughness = 2
    oracleText = "Haste\nMerry has first strike as long as it's equipped.\nWhenever you attack with Merry and another legendary creature, draw a card."

    keywords(Keyword.HASTE)

    // "Merry has first strike as long as it's equipped."
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.FIRST_STRIKE.name,
            filter = GroupFilter.source()
        )
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
    }

    // "Whenever you attack with Merry and another legendary creature, draw a card."
    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Exists(
            player = Player.You,
            zone = Zone.BATTLEFIELD,
            filter = GameObjectFilter.Creature.legendary().attacking(),
            excludeSelf = true
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Tomas Duchek"
        flavorText = "Then suddenly Merry felt it at last, beyond doubt: a change. Wind was in his face!"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7c5fe73-1684-4edc-9f9b-976b2246d5ea.jpg?1686969894"
    }
}
