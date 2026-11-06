package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Destined // Lead — Amonkhet #217
 *
 * Destined {1}{B} — Instant: Target creature gets +1/+0 and gains indestructible until end of turn.
 * Lead {3}{G} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   All creatures able to block target creature this turn do so.
 *
 * Lead is the Lure-for-a-turn shape of Taunting Challenge: it forces only creatures already able to
 * block the target, never grants them the ability to.
 */
val DestinedLead = card("Destined // Lead") {
    layout = CardLayout.SPLIT
    colorIdentity = "BG"

    face("Destined") {
        manaCost = "{1}{B}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+0 and gains indestructible until end of turn."

        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 0, t) then Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
        }
    }

    face("Lead") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "All creatures able to block target creature this turn do so."
        keywords(Keyword.AFTERMATH)

        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.MustBeBlocked(t)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cf5c549-1e2a-4c47-baf7-e608661b3088.jpg?1783936456"
        ruling("2017-04-18", "Lead doesn't give any creatures the ability to block the target creature. It just forces those creatures that are already able to block the creature to do so.")
        ruling("2017-04-18", "As blockers are declared, any creature that's tapped or affected by a spell or ability that says it can't block doesn't block. If there's a cost associated with having the creature block, no player is forced to pay that cost, so it doesn't block if that cost isn't paid.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
