package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bronze Cudgels — Kamigawa: Neon Dynasty #240 (canonical printing)
 * {1} · Artifact — Equipment
 *
 * {2}: Until end of turn, equipped creature gets +X/+0, where X is the number of times this
 * ability has resolved this turn.
 * Equip {1}
 *
 * The ability counts itself first, so its first resolution gives +1/+0, its second +2/+0 (a
 * separate bonus, +3/+0 in total), and so on. X locks in as each bonus is applied.
 */
val BronzeCudgels = card("Bronze Cudgels") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "{2}: Until end of turn, equipped creature gets +X/+0, where X is the number of times " +
        "this ability has resolved this turn.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    activatedAbility {
        cost = Costs.Mana(ManaCost.parse("{2}"))
        effect = IncrementAbilityResolutionCountEffect then
            Effects.ModifyStats(
                DynamicAmounts.abilityResolutionsThisTurn(),
                DynamicAmounts.fixed(0),
                EffectTarget.EquippedCreature,
            )
        description = "{2}: Until end of turn, equipped creature gets +X/+0, where X is the number of " +
            "times this ability has resolved this turn."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"These were made when Michiko was young. Wield them wisely.\"\n—Chiyoko, Jukai elder"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45fa226c-cc31-4c22-a167-40985706557b.jpg?1783923827"
    }
}
