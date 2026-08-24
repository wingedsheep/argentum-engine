package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scarwood Hag
 * {1}{G}
 * Creature — Hag
 * 1/1
 * {G}{G}{G}{G}, {T}: Target creature gains forestwalk until end of turn.
 * {T}: Target creature loses forestwalk until end of turn.
 *
 * A give/take pair on the same keyword, which is why both halves are plain keyword effects over a
 * single creature target rather than anything cleverer. The two share the {T} cost, so only one can
 * be used per untap — the printed card's real constraint, and one the tap cost enforces on its own.
 *
 * The "loses forestwalk" half works on any creature, not only one this Hag pumped: it removes the
 * keyword for the turn wherever it came from, printed or granted.
 */
val ScarwoodHag = card("Scarwood Hag") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hag"
    power = 1
    toughness = 1
    oracleText = "{G}{G}{G}{G}, {T}: Target creature gains forestwalk until end of turn. " +
        "(It can't be blocked as long as defending player controls a Forest.)\n" +
        "{T}: Target creature loses forestwalk until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}{G}{G}"), Costs.Tap)
        target = Targets.Creature
        effect = Effects.GrantKeyword(Keyword.FORESTWALK)
        description = "{G}{G}{G}{G}, {T}: Target creature gains forestwalk until end of turn."
    }

    activatedAbility {
        cost = Costs.Tap
        target = Targets.Creature
        effect = Effects.RemoveKeyword(Keyword.FORESTWALK)
        description = "{T}: Target creature loses forestwalk until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac2655e4-3a4d-4f73-820a-02fab675d42e.jpg?1783947930"
    }
}
