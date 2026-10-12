package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rootwire Amalgam — The Brothers' War #203
 * {5}
 * Artifact Creature — Golem
 * 5/5
 * Prototype {1}{G} — 2/3
 * {3}{G}{G}, Sacrifice this creature: Create an X/X colorless Golem artifact creature token, where
 * X is three times this creature's power. It gains haste until end of turn. Activate only as a sorcery.
 *
 * X reads the sacrifice-time snapshot of the Amalgam's power (`sacrificedPower()`), so a prototyped
 * 2/3 makes a 6/6 and a pumped one counts the pump. Haste is granted to the created token until end
 * of turn rather than printed on it.
 */
val RootwireAmalgam = card("Rootwire Amalgam") {
    manaCost = "{5}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Golem"
    power = 5
    toughness = 5
    oracleText = "Prototype {1}{G} — 2/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "{3}{G}{G}, Sacrifice this creature: Create an X/X colorless Golem artifact creature token, where X is three times this creature's power. It gains haste until end of turn. Activate only as a sorcery."

    keywordAbility(KeywordAbility.prototype("{1}{G}", 2, 3))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}{G}"), Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            creatureTypes = setOf("Golem"),
            artifactToken = true,
            dynamicPower = DynamicAmounts.sacrificedPower() * 3,
            dynamicToughness = DynamicAmounts.sacrificedPower() * 3,
            imageUri = "https://cards.scryfall.io/normal/front/4/7/474c8cfe-71ce-46c7-8cf4-dfeae3c0e73b.jpg?1783919909",
        ) then
            Effects.GrantKeyword(
                Keyword.HASTE,
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
            )
        timing = TimingRule.SorcerySpeed
        description = "{3}{G}{G}, Sacrifice this creature: Create an X/X colorless Golem artifact creature token, " +
            "where X is three times this creature's power. It gains haste until end of turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "203"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2498b591-4a80-459f-a639-fe4cf8880be2.jpg?1783920037"
        ruling("2022-10-14", "Use Rootwire Amalgam's power as it existed the last time it was on the battlefield (not the power of the creature card in the graveyard) to determine the value of X.")
    }
}
