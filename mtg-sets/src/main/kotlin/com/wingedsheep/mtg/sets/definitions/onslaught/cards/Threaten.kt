package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Threaten
 * {2}{R}
 * Sorcery
 * Untap target creature and gain control of it until end of turn.
 * That creature gains haste until end of turn.
 */
val Threaten = card("Threaten") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Untap target creature and gain control of it until end of turn. That creature gains haste until end of turn."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.Untap(t),
            Effects.GainControl(t, Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.HASTE, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Mark Brill"
        flavorText = "Goblins' motivational techniques are crude, but effective."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de9676b6-6812-44e5-ad70-f498fbad0e18.jpg?1562947965"
    }
}
