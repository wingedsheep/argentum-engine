package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirror Box
 * {3}
 * Artifact
 * The "legend rule" doesn't apply to permanents you control.
 * Each legendary creature you control gets +1/+1.
 * Each nontoken creature you control gets +1/+1 for each other creature you control with the same
 * name as that creature.
 *
 * The last ability is Alpha Status's per-affected-creature count on the name axis: for each
 * nontoken creature it lands on, count the creatures you control sharing a name with
 * [EffectTarget.AffectedEntity], `excludeSelf` dropping that creature itself ("other"). Tokens are
 * counted but never pumped. Both sides are restricted to face-up creatures — a face-down creature
 * has no name (ruling), and the projection carries no name for it, so without `faceUp()` the
 * name predicate would fall back to the hidden card's printed name.
 */
val MirrorBox = card("Mirror Box") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "The \"legend rule\" doesn't apply to permanents you control.\n" +
        "Each legendary creature you control gets +1/+1.\n" +
        "Each nontoken creature you control gets +1/+1 for each other creature you control with " +
        "the same name as that creature."

    staticAbility {
        ability = LegendRuleDoesNotApplyTo(GameObjectFilter.Permanent)
    }

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.legendary().youControl()))
    }

    staticAbility {
        val sameNamedOthers = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.faceUp().sharingNameWith(EffectTarget.AffectedEntity),
            excludeSelf = true,
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Creature.nontoken().faceUp().youControl()),
            powerBonus = sameNamedOthers,
            toughnessBonus = sameNamedOthers,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d507daa3-3f16-4ab1-81ea-794e5bb488fc.jpg?1783923824"

        ruling("2022-02-18", "A face-down creature has no name, so it doesn't have the same name as anything else.")
        ruling("2022-02-18", "Although token creatures won't get a bonus from Mirror Box's last ability, tokens that are copies of nontoken creatures improve the bonus those creatures get.")
        ruling("2022-02-18", "While the \"legend rule\" doesn't apply to permanents you control, you can control any number of legendary permanents with the same name and none of them will be put into the graveyard.")
        ruling("2022-02-18", "If you control more than one legendary permanent with the same name and the \"legend rule\" begins applying again (perhaps because Mirror Box leaves the battlefield), you'll immediately have to comply with the rule and put all but one of those permanents into the graveyard.")
    }
}
