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
 * Agility Bobblehead — X (Bobbleheads you control) is the dynamic target cap, read once as the
 * ability is activated and its targets are chosen; what happens to Bobbleheads afterwards doesn't
 * change the chosen targets.
 */
val AgilityBobblehead = card("Agility Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{3}, {T}: Up to X target creatures you control each gain haste until end of turn and can't be blocked this turn except by creatures with haste, where X is the number of Bobbleheads you control as you activate this ability."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        description = "Up to X target creatures you control gain haste and can't be blocked this turn except by creatures with haste (X = Bobbleheads you control)."
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        targets(
            TargetFilter.CreatureYouControl,
            optional = true,
            dynamicMaxCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count(),
        )
        effect = Effects.ForEachTarget(
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
            Effects.GrantCantBeBlockedExceptBy(
                EffectTarget.ContextTarget(0),
                GameObjectFilter.Creature.withKeyword(Keyword.HASTE),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cff8b0c5-5457-432a-8c27-f193c9922aec.jpg?1783912381"
        ruling("2024-03-08", "You choose how many targets Agility Bobblehead’s last ability has as you activate the ability. Once you do, it doesn’t matter what happens to any Bobbleheads you control.")
        ruling("2024-03-08", "Activating Agility Bobblehead’s last ability targeting a creature that has already been blocked by a creature without haste won’t cause that creature to become unblocked.")
    }
}
