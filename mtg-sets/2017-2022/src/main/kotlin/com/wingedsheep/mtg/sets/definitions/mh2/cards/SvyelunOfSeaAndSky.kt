package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Svyelun of Sea and Sky
 * {1}{U}{U}
 * Legendary Creature — Merfolk God
 * 3/4
 * Svyelun has indestructible as long as you control at least two other Merfolk.
 * Whenever Svyelun attacks, draw a card.
 * Other Merfolk you control have ward {1}.
 *
 * The bare tribal noun "Merfolk" names every Merfolk *permanent*, so both the indestructible
 * condition and the ward grant filter on [GameObjectFilter.Permanent]. Svyelun is itself a
 * Merfolk, hence "other" on both: it never counts toward its own two, and never gets ward
 * from itself.
 */
val SvyelunOfSeaAndSky = card("Svyelun of Sea and Sky") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk God"
    power = 3
    toughness = 4
    oracleText = "Svyelun has indestructible as long as you control at least two other Merfolk.\n" +
        "Whenever Svyelun attacks, draw a card.\n" +
        "Other Merfolk you control have ward {1}. (Whenever another Merfolk you control becomes " +
        "the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.YouControlOtherAtLeast(
                2,
                GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK)
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Mana("{1}"),
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK).youControl()).other()
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "69"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6f9ece1-669a-47c9-96c3-1e1dbf87421c.jpg?1783926868"
        ruling(
            "2021-06-18",
            "Damage dealt to creatures remains on those creatures until the cleanup step or until an " +
                "effect removes that damage. If you control Svyelun of Sea and Sky with at least 4 damage " +
                "and two other Merfolk and one of those Merfolk leaves the battlefield (or stops being a " +
                "Merfolk), Svyelun will be destroyed."
        )
        ruling(
            "2021-06-18",
            "If a player casts a spell that targets multiple permanents their opponent controls with ward, " +
                "each of those ward abilities will trigger. If that player doesn't pay for all of them, the " +
                "spell will be countered."
        )
    }
}
