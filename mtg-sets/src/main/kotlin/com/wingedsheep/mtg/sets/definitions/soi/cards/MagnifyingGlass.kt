package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Magnifying Glass — Shadows over Innistrad #258
 * {3} · Artifact
 *
 * {T}: Add {C}.
 * {4}, {T}: Investigate.
 *
 * Canonical definition lives in Shadows over Innistrad, the earliest real printing (2016-04-08),
 * where Clues debuted. Reprinted in Murders at Karlov Manor — see MKM `MagnifyingGlassReprint`.
 *
 * The two abilities share the tap symbol, so they compete: each turn the Glass either ramps or
 * (much later) starts turning mana into Clues, never both. Only the first is a mana ability — the
 * investigate line costs {4} on top of the tap and uses the stack, so it can be responded to,
 * which is why `manaAbility`/`TimingRule.ManaAbility` is set on the first activation and not the
 * second.
 */
val MagnifyingGlass = card("Magnifying Glass") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n" +
        "{4}, {T}: Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this " +
        "token: Draw a card.\")"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "258"
        artist = "Dan Murayama Scott"
        flavorText = "Knight-Inquisitors of Saint Raban delve deep into mysteries best left unexplored."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a708d5-f757-4fcf-a167-5b5920c6adeb.jpg?1783937707"

        ruling(
            "2016-04-08",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
        ruling(
            "2016-04-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token."
        )
    }
}
