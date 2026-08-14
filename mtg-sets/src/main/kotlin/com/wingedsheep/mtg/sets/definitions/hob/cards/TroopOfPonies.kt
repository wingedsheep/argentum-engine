package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Troop of Ponies
 * {2}
 * Creature — Horse
 * 2/1
 * {2}, {T}, Sacrifice this creature: Search your library for up to two basic land cards, reveal
 * them, put one onto the battlefield tapped and the other into your hand, then shuffle.
 *
 * The Cultivate split needs two selection steps, not one: pick up to two basics, then pick which
 * of the found cards enters tapped — the remainder goes to hand. A single
 * `Patterns.Library.searchLibrary(destination = HAND, entersTapped = true)` would silently send
 * *both* cards to hand and drop the tapped land entirely, so the pipeline is spelled out here
 * (same shape as Bloomvine Regent's Claim Territory). Finding only one card still works: the
 * second select is "exactly one" of what was found, so that card can go either way.
 */
val TroopOfPonies = card("Troop of Ponies") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Creature — Horse"
    oracleText = "{2}, {T}, Sacrifice this creature: Search your library for up to two basic land cards, reveal them, put one onto the battlefield tapped and the other into your hand, then shuffle."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    storeSelected = "found",
                    prompt = "Search your library for up to two basic land cards"
                ),
                SelectFromCollectionEffect(
                    from = "found",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "toBattlefield",
                    storeRemainder = "toHand",
                    selectedLabel = "Onto the battlefield tapped",
                    remainderLabel = "Into your hand",
                    prompt = "Choose which basic land enters the battlefield tapped; the other goes to your hand."
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    revealed = true
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true
                ),
                ShuffleLibraryEffect()
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Christina Kraus"
        flavorText = "The sun had only just turned west when they started on the path to Mirkwood, and till evening it lay golden on the land about them."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b4b1c59-bcec-4779-9e27-0e6f9feb4e11.jpg?1785639568"
    }
}
