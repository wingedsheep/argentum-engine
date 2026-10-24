package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Verdant Mastery — Strixhaven: School of Mages #146 (canonical printing)
 * {5}{G} · Sorcery
 *
 * You may pay {3}{G} rather than pay this spell's mana cost.
 * Search your library for up to four basic land cards and reveal them. Put one of them onto the
 * battlefield tapped under an opponent's control if the {3}{G} cost was paid. Put two of them onto
 * the battlefield tapped under your control and the rest into your hand. Then shuffle.
 *
 * The {3}{G} is the card's own [SelfAlternativeCost]; [Conditions.SelfAlternativeCostWasPaid]
 * gates the gift. When it was paid, an opponent is chosen as the spell resolves (ruling) and you
 * pick one revealed land to enter tapped under [Player.ChosenOpponent]'s control (you keep owning
 * it). Whatever revealed lands are still in the library then go two-tapped-onto-your-battlefield,
 * rest-to-hand.
 */
val VerdantMastery = card("Verdant Mastery") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You may pay {3}{G} rather than pay this spell's mana cost.\n" +
        "Search your library for up to four basic land cards and reveal them. " +
        "Put one of them onto the battlefield tapped under an opponent's control if the {3}{G} cost was paid. " +
        "Put two of them onto the battlefield tapped under your control and the rest into your hand. Then shuffle."

    selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{3}{G}"))

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(
                4,
                from = searchable,
                prompt = "Search your library for up to four basic land cards"
            )
            reveal(found)
            ifThen(Conditions.SelfAlternativeCostWasPaid) {
                run(Effects.ChooseOpponent("Choose an opponent to put a land onto the battlefield under their control"))
                val gift = chooseExactly(
                    1,
                    from = found,
                    prompt = "Choose a land to put onto the battlefield tapped under your opponent's control",
                    selectedLabel = "Opponent's battlefield (tapped)"
                )
                move(gift, CardDestination.ToZone(Zone.BATTLEFIELD, Player.ChosenOpponent, ZonePlacement.Tapped))
            }
            val remaining = filter(found, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            val (toBattlefield, toHandCards) = chooseExactlySplit(
                2,
                from = remaining,
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Into your hand",
                prompt = "Choose two lands to put onto the battlefield tapped; the rest go to your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toHand(toHandCards)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/197455e5-7929-47b8-ad00-8d3918949eb7.jpg?1783927333"
        ruling("2021-04-16", "If you choose to pay one alternative cost, you can't pay any other alternative costs. For example, if an effect lets you cast a \"Mastery\" spell \"without paying its mana cost,\" you can't also choose to pay its given alternative cost.")
        ruling("2021-04-16", "If an effect increases or decreases the cost of spells you cast, that cost increase or decrease is applied to the alternative cost you chose to pay. In that case, the cost was still paid for the purposes of the effect, even if you paid more or less for it when it was cast.")
        ruling("2021-04-16", "If you copy a \"Mastery\" spell and the alternative cost was paid, the copy will resolve as though the cost was paid.")
        ruling("2021-04-16", "In a multiplayer game, you choose which opponent takes the prescribed action as the spell resolves.")
    }
}
