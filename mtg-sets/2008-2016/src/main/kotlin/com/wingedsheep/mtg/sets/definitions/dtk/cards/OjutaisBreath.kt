package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ojutai's Breath
 * {2}{U}
 * Instant
 *
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)
 *
 * The same two-step construction as Crippling Chill: [Effects.Tap], then the
 * [AbilityFlag.DOESNT_UNTAP] grant held for [Duration.UntilAfterAffectedControllersNextUntap].
 * That duration is keyed to the *affected* creature's controller, not this spell's — which is what
 * "its controller's next untap step" says, and what makes the freeze land correctly when the
 * creature belongs to someone whose untap step comes before yours.
 *
 * The second line is the bare [Keyword.REBOUND]; `StackResolver` reads it off `cardDef.keywords`.
 */
val OjutaisBreath = card("Ojutai's Breath") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                t,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af51e5a1-7d46-4dad-a25c-6767cbd03dff.jpg?1783938604"
    }
}
