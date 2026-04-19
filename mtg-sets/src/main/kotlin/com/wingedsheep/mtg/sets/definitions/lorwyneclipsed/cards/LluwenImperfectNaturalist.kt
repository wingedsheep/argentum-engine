package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lluwen, Imperfect Naturalist
 * {B/G}{B/G}
 * Legendary Creature — Elf Druid
 * 1/3
 *
 * When Lluwen enters, mill four cards, then you may put a creature or land card
 * from among the milled cards on top of your library.
 * {2}{B/G}{B/G}{B/G}, {T}, Discard a land card: Create a 1/1 black and green Worm
 * creature token for each land card in your graveyard.
 */
val LluwenImperfectNaturalist = card("Lluwen, Imperfect Naturalist") {
    manaCost = "{B/G}{B/G}"
    typeLine = "Legendary Creature — Elf Druid"
    power = 1
    toughness = 3
    oracleText = "When Lluwen enters, mill four cards, then you may put a creature or land card from among the milled cards on top of your library.\n{2}{B/G}{B/G}{B/G}, {T}, Discard a land card: Create a 1/1 black and green Worm creature token for each land card in your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(
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
                    filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                    storeSelected = "kept",
                    showAllCards = true,
                    prompt = "You may put a creature or land card on top of your library",
                    selectedLabel = "Put on top of library",
                    remainderLabel = "Leave in graveyard"
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                )
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B/G}{B/G}{B/G}"),
            Costs.Tap,
            Costs.Discard(GameObjectFilter.Land)
        )
        effect = CreateTokenEffect(
            count = DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Land),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Worm"),
            imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c0ff23-6c20-4637-99d6-6cbca26d75ba.jpg?1767955743"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Evyn Fong"
        flavorText = "\"I know I belong somewhere—just not here.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/127a30a6-c25a-448a-a242-dc04f273a854.jpg?1767862678"
    }
}
