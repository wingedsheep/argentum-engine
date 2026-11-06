package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Onward // Victory — Amonkhet #218
 *
 * Onward {2}{R} — Instant: Target creature gets +X/+0 until end of turn, where X is its power.
 * Victory {2}{W} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Target creature gains double strike until end of turn.
 *
 * X is read once, as Onward resolves ([Effects.ModifyStats] evaluates the amount when it creates the
 * floating effect), so later power changes don't alter the bonus — matching the 2017-04-18 ruling.
 * Same composition as Berserk's pump.
 */
val OnwardVictory = card("Onward // Victory") {
    layout = CardLayout.SPLIT
    colorIdentity = "RW"

    face("Onward") {
        manaCost = "{2}{R}"
        typeLine = "Instant"
        oracleText = "Target creature gets +X/+0 until end of turn, where X is its power."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(
                power = DynamicAmounts.powerOf(creature),
                toughness = DynamicAmounts.fixed(0),
                target = creature
            )
        }
    }

    face("Victory") {
        manaCost = "{2}{W}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Target creature gains double strike until end of turn."
        keywords(Keyword.AFTERMATH)

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/517b32e4-4b34-431f-8f3b-98a6cffc245a.jpg?1783936455"
        ruling("2017-04-18", "The value of X is determined only as Onward resolves. It won't change later in the turn if other effects modify the creature's power.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        ruling("2017-04-18", "If you cast the first half of a split card with aftermath during your turn, you'll have priority immediately after it resolves. You can cast the half with aftermath from your graveyard before any player can take any other action if it's legal for you to do so.")
    }
}
