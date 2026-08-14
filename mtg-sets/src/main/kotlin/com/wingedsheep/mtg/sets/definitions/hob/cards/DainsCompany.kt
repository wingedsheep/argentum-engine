package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dáin's Company — The Hobbit #152
 * {R}{W} · Creature — Dwarf Warrior · Rare
 * 2/2
 *
 * This creature has lifelink as long as you control another Dwarf.
 * When this creature enters, look at the top four cards of your library. You may reveal a Dwarf or
 * Equipment card from among them and put it into your hand. Put the rest on the bottom of your library
 * in a random order.
 *
 * Modeling notes:
 *  - The lifelink clause is the Bolg's Company / Barrow Naughty shape: a `ConditionalStaticAbility`
 *    gated on `YouControl(..., excludeSelf = true)`, so it goes dark the instant the other Dwarf
 *    leaves rather than latching on once.
 *  - The dig is the Boughside Wanderers idiom — gather four, `ChooseUpTo` one ("you may reveal" means
 *    declining stays legal even with hits present), move the pick to hand `revealed = true`, and bottom
 *    the remainder with [CardOrder.Random].
 *  - "A Dwarf or Equipment card" is one `HasAnyOfSubtypes` rather than an `Or` of two predicates: both
 *    halves are subtypes, and the card doesn't care which type line carries them (a Dwarf Equipment
 *    would qualify once, not twice).
 */
val DainsCompany = card("Dáin's Company") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dwarf Warrior"
    power = 2
    toughness = 2
    oracleText = "This creature has lifelink as long as you control another Dwarf.\n" +
        "When this creature enters, look at the top four cards of your library. You may reveal a " +
        "Dwarf or Equipment card from among them and put it into your hand. Put the rest on the " +
        "bottom of your library in a random order."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withSubtype(Subtype.DWARF),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)), storeAs = "looked"),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.HasAnyOfSubtypes(listOf(Subtype.DWARF, Subtype.EQUIPMENT))
                    )
                ),
                storeSelected = "kept",
                storeRemainder = "rest",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom"
            ),
            MoveCollectionEffect(from = "kept", destination = CardDestination.ToZone(Zone.HAND), revealed = true),
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36db4405-8589-481f-b627-f26087488337.jpg?1785236717"
    }
}
