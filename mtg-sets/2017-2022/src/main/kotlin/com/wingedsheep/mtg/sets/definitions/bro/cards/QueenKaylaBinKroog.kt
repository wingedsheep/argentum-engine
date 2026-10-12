package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Queen Kayla bin-Kroog
 * {1}{R}{W}
 * Legendary Creature — Human Noble
 * 2/3
 * {4}, {T}: Discard all the cards in your hand, then draw that many cards. You may choose an
 * artifact or creature card with mana value 1 you discarded this way, then do the same for
 * artifact or creature cards with mana values 2 and 3. Return those cards to the battlefield.
 * Activate only as a sorcery.
 *
 * Modelling:
 *  - The whole hand is gathered first and that collection *is* "the cards discarded this way":
 *    per the 2022-10-14 ruling a card a replacement effect exiles instead of putting into the
 *    graveyard (madness) still counts, so the pool is not narrowed to the graveyard. The draw
 *    reads the gathered collection's size.
 *  - Three independent "up to one" picks from that pool, one per mana value 1 / 2 / 3, each
 *    restricted to artifact or creature cards — every pick may be declined.
 *  - The ruling has all chosen cards enter simultaneously, so the three picks are folded into one
 *    collection before a single move: peel each pick off the discarded pool, then the union is
 *    the pool minus what's left.
 */
val QueenKaylaBinKroog = card("Queen Kayla bin-Kroog") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Noble"
    oracleText = "{4}, {T}: Discard all the cards in your hand, then draw that many cards. You may " +
        "choose an artifact or creature card with mana value 1 you discarded this way, then do the " +
        "same for artifact or creature cards with mana values 2 and 3. Return those cards to the " +
        "battlefield. Activate only as a sorcery."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Pipeline {
            val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
            run(Effects.DrawCards(discarded.count))

            val artifactOrCreature = GameObjectFilter.Artifact or GameObjectFilter.Creature
            val one = chooseUpTo(
                1, from = discarded, filter = artifactOrCreature.manaValue(1),
                prompt = "You may choose an artifact or creature card with mana value 1 you discarded",
                selectedLabel = "Return to the battlefield",
            )
            val two = chooseUpTo(
                1, from = discarded, filter = artifactOrCreature.manaValue(2),
                prompt = "You may choose an artifact or creature card with mana value 2 you discarded",
                selectedLabel = "Return to the battlefield",
            )
            val three = chooseUpTo(
                1, from = discarded, filter = artifactOrCreature.manaValue(3),
                prompt = "You may choose an artifact or creature card with mana value 3 you discarded",
                selectedLabel = "Return to the battlefield",
            )

            val unchosen = exclude(exclude(exclude(discarded, one), two), three)
            val chosen = exclude(discarded, unchosen)
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        timing = TimingRule.SorcerySpeed
        description = "{4}, {T}: Discard all the cards in your hand, then draw that many cards. You " +
            "may choose an artifact or creature card with mana value 1, 2, and 3 you discarded this " +
            "way. Return those cards to the battlefield. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/685c9767-eaa7-4e16-b245-816181e2fc36.jpg?1783920027"
        ruling(
            "2022-10-14",
            "From among the discarded cards, you may choose an artifact or creature card with mana " +
                "value 1, you may choose an artifact or creature card with mana value 2, and you may " +
                "choose an artifact or creature card with mana value 3. Then put all the chosen cards " +
                "onto the battlefield simultaneously."
        )
        ruling(
            "2022-10-14",
            "If a replacement effect causes cards you discard this way to be exiled rather than go to " +
                "the graveyard, Queen Kayla bin-Kroog's ability can still find those cards in exile. " +
                "You can still put cards discarded this way onto the battlefield with Queen Kayla's ability."
        )
    }
}
