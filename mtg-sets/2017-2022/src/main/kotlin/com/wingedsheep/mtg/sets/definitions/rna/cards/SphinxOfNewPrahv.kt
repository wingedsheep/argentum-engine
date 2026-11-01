package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sphinx of New Prahv — Ravnica Allegiance #208
 * {W}{W}{U}{U} · Creature — Sphinx · 4 / 3
 *
 * Flying, vigilance, plus the same targeting tax Syr Elenora carries:
 * [SpellCostTarget.OpponentsCastTargeting] on the source with a {2} generic increase.
 */
val SphinxOfNewPrahv = card("Sphinx of New Prahv") {
    manaCost = "{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Sphinx"
    power = 4
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Spells your opponents cast that target this creature cost {2} more to cast."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Sara Winters"
        flavorText = "Azorius sphinxes are loyal to the precepts of the law, not to any particular guildmaster—even one of their own kind."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9aec56e-50a3-491f-89c6-32907ea3161a.jpg?1783933635"
    }
}
