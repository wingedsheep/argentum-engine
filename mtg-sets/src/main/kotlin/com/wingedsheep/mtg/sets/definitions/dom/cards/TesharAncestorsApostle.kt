package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Teshar, Ancestor's Apostle
 * {3}{W}
 * Legendary Creature — Bird Cleric
 * 2/2
 * Flying
 * Whenever you cast a historic spell, return target creature card with mana value 3 or less
 * from your graveyard to the battlefield.
 * (Artifacts, legendaries, and Sagas are historic.)
 */
val TesharAncestorsApostle = card("Teshar, Ancestor's Apostle") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Bird Cleric"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever you cast a historic spell, return target creature card with mana value 3 or less from your graveyard to the battlefield. (Artifacts, legendaries, and Sagas are historic.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastHistoric
        val t = target("target", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3),
                zone = Zone.GRAVEYARD
            )
        ))
        effect = Effects.Move(
            target = t,
            destination = Zone.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Even Amundsen"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6d115b4-51d5-4898-b56c-2729aa428018.jpg?1562745787"
        ruling("2018-04-27", "If the mana cost of a card in your graveyard includes {X}, X is considered to be 0.")
        ruling("2018-04-27", "A card, spell, or permanent is historic if it has the legendary supertype, the artifact card type, or the Saga subtype.")
        ruling("2018-04-27", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
