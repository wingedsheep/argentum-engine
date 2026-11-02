package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mission Briefing (GRN 44)
 * {U}{U} — Instant
 *
 * Surveil 2, then choose an instant or sorcery card in your graveyard. You may cast it this turn.
 * If that spell would be put into your graveyard, exile it instead.
 *
 * The card is chosen on resolution, after the surveil, so one just surveilled away is eligible;
 * Mission Briefing itself is still on the stack and can't be chosen. The permission is the same
 * "may cast from a zone this turn" grant Mavinda and Radiant Scrollwielder use: the spell is cast
 * for its mana cost with its normal timing, and is exiled instead of going to the graveyard.
 */
val MissionBriefing = card("Mission Briefing") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surveil 2, then choose an instant or sorcery card in your graveyard. You may cast it " +
        "this turn. If that spell would be put into your graveyard, exile it instead. (To surveil 2, " +
        "look at the top two cards of your library, then put any number of them into your graveyard " +
        "and the rest on top of your library in any order.)"

    spell {
        effect = Effects.Surveil(2) then Effects.Pipeline {
            val spells = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, filter = GameObjectFilter.InstantOrSorcery)
            )
            val chosen = chooseExactly(
                1,
                from = spells,
                prompt = "Choose an instant or sorcery card you may cast this turn",
            )
            run(Effects.GrantMayPlayFromExile(
                from = chosen,
                expiry = MayPlayExpiry.EndOfTurn,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02bed34d-67d9-4ae9-b351-dc9a89daeabf.jpg?1783934187"
        ruling("2018-10-05", "The instant or sorcery card you choose may be one that you just surveilled into your graveyard.")
        ruling("2018-10-05", "Mission Briefing doesn't change when you can cast the chosen card. For example, if you choose a sorcery card, you can cast it only during your main phase when the stack is empty.")
        ruling("2018-10-05", "Mission Briefing is still on the stack while you choose an instant or sorcery card in your graveyard. Your Mission Briefing can't be to give yourself that same Mission Briefing.")
        ruling("2018-10-05", "If you cast a spell this way and that card is exiled, it's considered a new object in the zone it's put into. It won't be exiled if it's put into your graveyard later in the turn.")
    }
}
