package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Outcaster Greenblade
 * {2}{G}
 * Creature — Human Mercenary
 * 1/2
 *
 * When this creature enters, search your library for a basic land card or a Desert card,
 * reveal it, put it into your hand, then shuffle.
 * This creature gets +1/+1 for each Desert you control.
 *
 * The static self-buff is a [GrantDynamicStatsEffect] whose bonus counts Deserts you control;
 * the count is read through projected state so it updates live as Deserts enter/leave.
 */
val OutcasterGreenblade = card("Outcaster Greenblade") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Mercenary"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, search your library for a basic land card or a " +
        "Desert card, reveal it, put it into your hand, then shuffle.\nThis creature gets +1/+1 " +
        "for each Desert you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype(Subtype.DESERT),
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    staticAbility {
        val deserts = DynamicAmount.Count(
            player = Player.You,
            zone = Zone.BATTLEFIELD,
            filter = GameObjectFilter.Land.withSubtype(Subtype.DESERT)
        )
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = deserts,
            toughnessBonus = deserts
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Josu Hernaiz"
        flavorText = "In the shade of his sword, the desert blooms."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9458f0f-5593-4ac9-934c-e215ef8093a7.jpg?1712355958"
    }
}
