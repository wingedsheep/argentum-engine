package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Futurist Operative
 * {3}{U}
 * Creature — Human Ninja
 * 3/4
 * As long as this creature is tapped, it's a Human Citizen with base power and toughness 1/1 and
 * can't be blocked.
 * {2}{U}: Untap this creature.
 *
 * One printed static ability spanning three layers, gated on the source being tapped: the creature
 * types are replaced by Human Citizen (Layer 4), its base P/T becomes 1/1 (Layer 7b), and it gains
 * "can't be blocked". Being a static ability, its timestamp is when the creature entered, not when
 * it became tapped (per the 2022-02-18 ruling).
 */
val FuturistOperative = card("Futurist Operative") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ninja"
    power = 3
    toughness = 4
    oracleText = "As long as this creature is tapped, it's a Human Citizen with base power and " +
        "toughness 1/1 and can't be blocked.\n{2}{U}: Untap this creature."

    staticAbility {
        ability = ConditionalStaticAbility(
            condition = Conditions.SourceIsTapped,
            ability = CompositeStaticAbility(
                listOf(
                    TransformPermanent(setSubtypes = setOf("Human", "Citizen"), filter = Filters.Self),
                    SetBasePowerToughnessStatic(1, 1, Filters.Self),
                    CantBeBlocked(Filters.Self),
                )
            ),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Justine Cruz"
        flavorText = "\"The Imperials can't arrest you for illegal tech if you never even existed.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5858176-9f85-403c-8338-48eb55db5c3d.jpg?1783923904"

        ruling(
            "2022-02-18",
            "Effects that set a creature's power, toughness, or creature types will be applied in " +
                "timestamp order. The timestamp of Futurist Operative's first ability is when it " +
                "entered the battlefield, not when it became tapped. Notably, this means the ability " +
                "will not overwrite any effects that set its power and toughness to specific numbers " +
                "after it entered the battlefield. Effects that otherwise modify its power and " +
                "toughness will still apply no matter when they took effect. The same is true for " +
                "+1/+1 counters."
        )
        ruling(
            "2022-02-18",
            "Untapping Futurist Operative once it's been declared as an attacker won't remove it " +
                "from combat."
        )
    }
}
