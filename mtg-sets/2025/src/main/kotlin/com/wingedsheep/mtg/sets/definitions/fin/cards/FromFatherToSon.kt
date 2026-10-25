package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * From Father to Son
 * {1}{W}
 * Sorcery
 * Search your library for a Vehicle card, reveal it, and put it into your hand. If this spell
 *   was cast from a graveyard, put that card onto the battlefield instead. Then shuffle.
 * Flashback {4}{W}{W}{W}
 *
 * A Gather → Select → Move tutor whose *destination* is chosen at resolution by where the spell
 * was cast from. The only flashback-legal way to cast this from the graveyard is its own
 * flashback cost, so [WasCastFromZone] (GRAVEYARD) cleanly distinguishes the two printed
 * destinations: the found card goes to the battlefield on a graveyard cast, otherwise to hand.
 * Both branches reveal the card (the move's `revealed` flag) and the shuffle happens after,
 * matching "Then shuffle." "Search for ... a card" is a may-find tutor, so the selection is
 * [SelectionMode.ChooseUpTo] 1 (you may fail to find).
 */
val FromFatherToSon = card("From Father to Son") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Search your library for a Vehicle card, reveal it, and put it into your hand. " +
        "If this spell was cast from a graveyard, put that card onto the battlefield instead. Then shuffle.\n" +
        "Flashback {4}{W}{W}{W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    zone = Zone.LIBRARY,
                    player = Player.You,
                    filter = GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE)
                ),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search for a Vehicle card",
                selectedLabel = "Reveal it"
            )
            run(Effects.If(
                condition = WasCastFromZone(Zone.GRAVEYARD),
                then = Effects.Pipeline { move(found, CardDestination.ToZone(Zone.BATTLEFIELD), revealed = true) },
                otherwise = Effects.Pipeline { toHand(found, revealed = true) }
            ))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    keywordAbility(KeywordAbility.flashback("{4}{W}{W}{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Jeremy Chong"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c730a3b-334e-466b-bb9b-4b41fce2af6d.jpg?1748705832"
    }
}
