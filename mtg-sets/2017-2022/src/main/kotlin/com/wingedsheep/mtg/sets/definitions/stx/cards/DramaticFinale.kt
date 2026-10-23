package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dramatic Finale — Strixhaven: School of Mages #180 (canonical printing)
 * {W/B}{W/B}{W/B}{W/B} · Enchantment
 *
 * Creature tokens you control get +1/+1.
 * Whenever one or more nontoken creatures you control die, create a 2/1 white and black Inkling
 * creature token with flying. This ability triggers only once each turn.
 *
 * The anthem is the Intangible Virtue group [ModifyStats] over creature tokens you control. The
 * death clause is the batched `Triggers.oneOrMore(...).die()` (one fire per simultaneous death
 * batch) capped by `oncePerTurn` for "triggers only once each turn". Token art resolves through
 * STX's synced token printings, as for Inkling Summoning.
 */
val DramaticFinale = card("Dramatic Finale") {
    manaCost = "{W/B}{W/B}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "Creature tokens you control get +1/+1.\n" +
        "Whenever one or more nontoken creatures you control die, create a 2/1 white and black " +
        "Inkling creature token with flying. This ability triggers only once each turn."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.nontoken().youControl()).die()
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Inkling"),
            keywords = setOf(Keyword.FLYING)
        )
        description = "Whenever one or more nontoken creatures you control die, create a 2/1 white " +
            "and black Inkling creature token with flying. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Steve Argyle"
        flavorText = "The enraptured audience barely noticed their programs were suddenly blank."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8ec9be6-c8ad-4a82-8253-c49cdb9443ba.jpg?1783927317"
    }
}
