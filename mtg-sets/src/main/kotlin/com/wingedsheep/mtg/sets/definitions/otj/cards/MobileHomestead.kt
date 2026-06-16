package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mobile Homestead
 * {2}
 * Artifact — Vehicle
 * 3/3
 *
 * This Vehicle has haste as long as you control a Mount.
 * Whenever this Vehicle attacks, look at the top card of your library. If it's a land
 * card, you may put it onto the battlefield tapped.
 * Crew 2
 *
 * Conditional haste mirrors Tribal Golem (GrantKeyword + ControlCreatureOfType). The
 * attack trigger mirrors Fecund Greenshell's top-card pipeline, minus the "otherwise to
 * hand" leg — Mobile Homestead leaves a non-land (or a declined land) on top of the library.
 */
val MobileHomestead = card("Mobile Homestead") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    oracleText = "This Vehicle has haste as long as you control a Mount.\n" +
        "Whenever this Vehicle attacks, look at the top card of your library. If it's a land " +
        "card, you may put it onto the battlefield tapped.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 3

    // This Vehicle has haste as long as you control a Mount.
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.source())
        condition = Conditions.ControlCreatureOfType(Subtype("Mount"))
    }

    // Whenever this Vehicle attacks, look at the top card of your library.
    // If it's a land card, you may put it onto the battlefield tapped.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                // Look at top 1 card.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "looked",
                ),
                // Keep only land cards as candidates.
                FilterCollectionEffect(
                    from = "looked",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                    storeMatching = "landCards",
                ),
                // You may put the land onto the battlefield tapped; otherwise it stays on top.
                SelectFromCollectionEffect(
                    from = "landCards",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "toBattlefield",
                    selectedLabel = "Put onto the battlefield tapped",
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                ),
            )
        )
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5fa82e4-0b77-498a-bec0-52764d24957a.jpg?1712356276"
    }
}
