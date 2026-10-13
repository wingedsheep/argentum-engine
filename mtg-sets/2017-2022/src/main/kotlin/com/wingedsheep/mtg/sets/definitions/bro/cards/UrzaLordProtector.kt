package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Urza, Lord Protector
 * {1}{W}{U}
 * Legendary Creature — Human Artificer
 * 2/4
 * Artifact, instant, and sorcery spells you cast cost {1} less to cast.
 * {7}: If you both own and control Urza, Lord Protector and an artifact named The Mightstone and
 * Weakstone, exile them, then meld them into Urza, Planeswalker. Activate only as a sorcery.
 *
 * The discount is one `ModifySpellCost` over "artifact or instant or sorcery", so a spell that is
 * both (an artifact instant) is discounted {1} once; `ReduceGeneric` never touches colored mana
 * (2022-10-14 ruling). The meld ability can be activated without the Mightstone — [Effects.Meld]
 * re-checks the own-and-control condition on resolution and does nothing if it fails.
 */
val UrzaLordProtector = card("Urza, Lord Protector") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 4
    oracleText = "Artifact, instant, and sorcery spells you cast cost {1} less to cast.\n" +
        "{7}: If you both own and control Urza, Lord Protector and an artifact named The Mightstone " +
        "and Weakstone, exile them, then meld them into Urza, Planeswalker. Activate only as a sorcery."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{7}")
        effect = Effects.Meld(GameObjectFilter.Artifact.named(MIGHTSTONE), into = "Urza, Planeswalker")
        timing = TimingRule.SorcerySpeed
        description = "{7}: If you both own and control Urza, Lord Protector and an artifact named " +
            "The Mightstone and Weakstone, exile them, then meld them into Urza, Planeswalker."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "225"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8aefe8bd-216a-4ec1-9362-3f9dbf7fd083.jpg?1783920024"
        ruling("2022-10-14", "Urza, Lord Protector's first ability can't reduce the colored mana costs of spells you cast.")
    }
}

private const val MIGHTSTONE = "The Mightstone and Weakstone"
