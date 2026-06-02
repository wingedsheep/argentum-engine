package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val TownGreeter = card("Town Greeter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, mill four cards. You may put a land card from among them into your hand. If you put a Town card into your hand this way, you gain 2 life. (To mill four cards, a player puts the top four cards of their library into their graveyard.)"

    val townFilter = GameObjectFilter.Land.withSubtype("Town")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "milled"
                ),
                MoveCollectionEffect(
                    from = "milled",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                SelectFromCollectionEffect(
                    from = "milled",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Land,
                    storeSelected = "selected",
                    showAllCards = true,
                    prompt = "You may put a land card into your hand",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Leave in graveyard"
                ),
                MoveCollectionEffect(
                    from = "selected",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                ConditionalEffect(
                    condition = Conditions.CollectionContainsMatch("selected", townFilter),
                    effect = Effects.GainLife(2)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Hayaken-Sarena"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49cd4efa-4df4-4257-9a42-60330f7781e2.jpg?1748706545"
    }
}
