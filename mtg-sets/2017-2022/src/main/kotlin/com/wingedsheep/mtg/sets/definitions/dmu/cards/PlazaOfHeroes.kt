package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Plaza of Heroes
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a legendary spell.
 * {T}: Add one mana of any color among legendary permanents you control.
 * {3}, {T}, Exile this land: Target legendary creature gains hexproof and indestructible until end of turn.
 *
 * The restricted any-color mana carries [ManaRestriction.LegendarySpellsOnly] (enforced by
 * `ManaPool` at spend time, as on Untaidake). The third ability reads the projected colors of
 * legendary permanents you control, like Mox Amber; with none (or only colorless ones) it adds
 * nothing.
 */
val PlazaOfHeroes = card("Plaza of Heroes") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a legendary spell.\n" +
        "{T}: Add one mana of any color among legendary permanents you control.\n" +
        "{3}, {T}, Exile this land: Target legendary creature gains hexproof and indestructible until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1, restriction = ManaRestriction.LegendarySpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfColorAmong(GameObjectFilter.Permanent.legendary().youControl())
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.ExileSelf)
        val t = target(TargetFilter.Creature.legendary())
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, t) then Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2cfcf67-f83c-43af-9e2d-5513fcdde835.jpg?1783921260"
    }
}
