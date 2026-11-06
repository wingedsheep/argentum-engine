package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cut // Ribbons — Amonkhet #223
 *
 * Cut {1}{R} — Sorcery: Cut deals 4 damage to target creature.
 * Ribbons {X}{B}{B} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Each opponent loses X life.
 */
val CutRibbons = card("Cut // Ribbons") {
    layout = CardLayout.SPLIT
    colorIdentity = "BR"

    face("Cut") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        oracleText = "Cut deals 4 damage to target creature."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamage(4, creature)
        }
    }

    face("Ribbons") {
        manaCost = "{X}{B}{B}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Each opponent loses X life."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.LoseLife(DynamicAmounts.xValue(), EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6f61e2b-e93b-4dda-95cf-9d0ff198c0a6.jpg?1783936453"
        ruling("2017-04-18", "In a Two-Headed Giant game, Ribbons causes the opposing team to lose two times X life.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        ruling("2017-04-18", "If you cast the first half of a split card with aftermath during your turn, you'll have priority immediately after it resolves. You can cast the half with aftermath from your graveyard before any player can take any other action if it's legal for you to do so.")
    }
}
