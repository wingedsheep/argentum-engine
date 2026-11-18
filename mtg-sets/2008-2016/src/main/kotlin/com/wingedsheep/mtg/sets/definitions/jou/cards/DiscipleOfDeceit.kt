package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Disciple of Deceit — Journey into Nyx #148
 * {U}{B} · Creature — Human Rogue · 1/3
 *
 * Inspired — Whenever this creature becomes untapped, you may discard a nonland card. If you do,
 * search your library for a card with the same mana value as that card, reveal it, put it into
 * your hand, then shuffle.
 *
 * Pipeline composition (Narset of the Ancient Way's optional discard + Neoform's exact-mana-value
 * search): choose up to one nonland card from hand and discard it, keeping it in the "discarded"
 * collection; only if one was discarded (the "if you do" leg) search the library with
 * [GameObjectFilter.manaValueEqualsDynamic] over [DynamicAmounts.manaValueOf] that collection
 * (read off the card in the graveyard; X counts as 0), reveal the find into hand, then shuffle.
 */
val DiscipleOfDeceit = card("Disciple of Deceit") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 3
    oracleText = "Inspired — Whenever this creature becomes untapped, you may discard a nonland card. " +
        "If you do, search your library for a card with the same mana value as that card, reveal it, " +
        "put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.Pipeline {
            val nonlandInHand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Nonland))
            val discarded = chooseUpTo(1, from = nonlandInHand, prompt = "You may discard a nonland card")
            discard(discarded)
            run(Effects.If(
                condition = whenMatches(discarded),
                then = Effects.Pipeline {
                    val searchable = gather(
                        CardSource.FromZone(
                            Zone.LIBRARY,
                            Player.You,
                            GameObjectFilter.Any.manaValueEqualsDynamic(DynamicAmounts.manaValueOf(discarded))
                        ),
                        search = true
                    )
                    val found = chooseUpTo(
                        1,
                        from = searchable,
                        prompt = "Search your library for a card with the same mana value as the discarded card"
                    )
                    toHand(found, revealed = true)
                    run(Effects.ShuffleLibrary())
                    run(EmitLibrarySearchedEventEffect)
                }
            ))
        }
        description = "Inspired — Whenever this creature becomes untapped, you may discard a nonland card. " +
            "If you do, search your library for a card with the same mana value as that card, reveal it, " +
            "put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbfe88fd-9bb9-42af-8134-96fd264b8405.jpg?1783939405"

        ruling("2017-04-18", "The mana value of a split card is determined by the combined mana cost of its two halves.")
        ruling("2014-04-26", "If there's an {X} in the mana cost of the card you discarded or the card you wish to search for, X is 0.")
        ruling("2014-04-26", "Inspired abilities trigger no matter how the creature becomes untapped: by the turn-based action at the beginning of the untap step or by a spell or ability.")
        ruling("2014-04-26", "If an inspired ability triggers during your untap step, the ability will be put on the stack at the beginning of your upkeep. If the ability creates one or more token creatures, those creatures won't be able to attack that turn (unless they gain haste).")
        ruling("2014-04-26", "Inspired abilities don't trigger when the creature enters the battlefield.")
        ruling("2014-04-26", "If the inspired ability includes an optional cost, you decide whether to pay that cost as the ability resolves. You can do this even if the creature leaves the battlefield in response to the ability.")
    }
}
