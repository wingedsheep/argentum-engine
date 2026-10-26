package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scion of the Ur-Dragon {W}{U}{B}{R}{G}
 * Legendary Creature — Dragon Avatar
 * 4/4
 *
 * Flying
 * {2}: Search your library for a Dragon permanent card and put it into your graveyard. If you do,
 * Scion of the Ur-Dragon becomes a copy of that card until end of turn. Then shuffle.
 *
 * The search is an inline gather → chooseUpTo(1) pipeline (a search may find nothing). The card is
 * moved with `moveTracked`, so "if you do" is a test on the cards that actually reached the
 * graveyard rather than on the selection. The copy is Vesuvan Drifter's single-permanent shape —
 * `EachPermanentBecomesCopyOfTarget(affected = Self, duration = EndOfTurn, sourceFromAnyZone = true)`
 * — reading the copiable values off the card in the graveyard; it is baked into Scion's card
 * component, so it loses this ability while it's a copy (2006-09-25 ruling), and exiling the
 * graveyard card afterward changes nothing (the other 2006-09-25 ruling).
 */
val ScionOfTheUrDragon = card("Scion of the Ur-Dragon") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon Avatar"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "{2}: Search your library for a Dragon permanent card and put it into your graveyard. " +
        "If you do, Scion of the Ur-Dragon becomes a copy of that card until end of turn. " +
        "Then shuffle."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.Pipeline {
            val dragons = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)
                ),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = dragons,
                prompt = "Search your library for a Dragon permanent card to put into your graveyard"
            )
            val milled = moveTracked(found, CardDestination.ToZone(Zone.GRAVEYARD))
            run(
                Effects.If(
                    condition = whenMatches(milled),
                    then = Effects.EachPermanentBecomesCopyOfTarget(
                        target = milled.asTarget,
                        affected = EffectTarget.Self,
                        duration = Duration.EndOfTurn,
                        sourceFromAnyZone = true,
                    ),
                )
            )
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "Search your library for a Dragon permanent card and put it into your " +
            "graveyard. If you do, Scion of the Ur-Dragon becomes a copy of that card until end " +
            "of turn. Then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Jim Murray"
        flavorText = "\"I am the blood of the ur-dragon, coursing through all dragonkind.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efb6f85b-60e9-4191-b557-ec952b7f22fc.jpg?1783943200"

        ruling("2006-09-25", "Removing the other Dragon card from your graveyard has no effect on Scion of the Ur-Dragon.")
        ruling("2006-09-25", "When Scion of the Ur-Dragon becomes a copy of another Dragon, it loses its copy ability for the rest of the turn.")
    }
}
