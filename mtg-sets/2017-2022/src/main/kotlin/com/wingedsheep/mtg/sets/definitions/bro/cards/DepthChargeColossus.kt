package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Depth Charge Colossus
 * {9}
 * Artifact Creature — Dreadnought
 * 9/9
 * Prototype {4}{U}{U} — 6/6
 * This creature doesn't untap during your untap step.
 * {3}: Untap this creature.
 *
 * "Doesn't untap" is the self-suppression flag [AbilityFlag.DOESNT_UNTAP] (cf. Colossus of Sardia);
 * the untap ability is a plain [Effects.Untap] on itself with no timing restriction.
 */
val DepthChargeColossus = card("Depth Charge Colossus") {
    manaCost = "{9}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Dreadnought"
    power = 9
    toughness = 9
    oracleText = "Prototype {4}{U}{U} — 6/6 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "This creature doesn't untap during your untap step.\n" +
        "{3}: Untap this creature."

    keywordAbility(KeywordAbility.prototype("{4}{U}{U}", 6, 6))
    flags(AbilityFlag.DOESNT_UNTAP)

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Untap(EffectTarget.Self)
        description = "{3}: Untap this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34e09173-a92d-4cc3-9d02-bf18ab0850ec.jpg?1783920099"
    }
}
