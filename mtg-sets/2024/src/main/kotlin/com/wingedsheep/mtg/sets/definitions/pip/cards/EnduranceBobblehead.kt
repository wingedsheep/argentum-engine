package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Endurance Bobblehead — X (Bobbleheads you control) is the dynamic target cap, read once as the
 * ability is activated and its targets are chosen; what happens to Bobbleheads afterwards doesn't
 * change the chosen targets.
 */
val EnduranceBobblehead = card("Endurance Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{3}, {T}: Up to X target creatures you control get +1/+0 and gain indestructible until end of turn, where X is the number of Bobbleheads you control as you activate this ability. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        description = "Up to X target creatures you control get +1/+0 and gain indestructible until end of turn (X = Bobbleheads you control)."
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        targets(
            TargetFilter.CreatureYouControl,
            optional = true,
            dynamicMaxCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count(),
        )
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Alex Brock"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8768789b-c414-4e35-bff3-d06a69dccff6.jpg?1783912380"
        ruling("2024-03-08", "You choose how many targets Endurance Bobblehead’s last ability has as you activate the ability. Once you do, it doesn’t matter what happens to any Bobbleheads you control.")
    }
}
