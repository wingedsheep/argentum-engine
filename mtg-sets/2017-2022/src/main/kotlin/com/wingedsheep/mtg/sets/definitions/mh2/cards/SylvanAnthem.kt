package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sylvan Anthem — Modern Horizons 2 #176
 * {G}{G} · Enchantment
 *
 * Green creatures you control get +1/+1.
 * Whenever a green creature you control enters, scry 1.
 *
 * The lord is a plain [ModifyStats] static over a [GroupFilter] and deliberately does **not** pass
 * `excludeSelf`: the anthem cards that do (Wilt-Leaf Liege) only need it because they are
 * themselves creatures and their text says "other". Sylvan Anthem is an enchantment, so it can
 * never be in its own affected set anyway.
 *
 * The scry trigger uses [TriggerBinding.ANY] for the same reason the Oracle text says "a green
 * creature you control" rather than "*another* green creature": there is no self to exclude.
 * (Contrast Ivy Lane Denizen, whose printed "another" is [TriggerBinding.OTHER].) The green check
 * rides the trigger's [GameObjectFilter], which is evaluated against projected state, so a
 * creature made green by another continuous effect as it enters still triggers.
 */
val SylvanAnthem = card("Sylvan Anthem") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Green creatures you control get +1/+1.\n" +
        "Whenever a green creature you control enters, scry 1."

    // Green creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN).youControl())
        )
    }

    // Whenever a green creature you control enters, scry 1.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withColor(Color.GREEN).youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Patterns.Library.scry(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Franz Vohwinkel"
        flavorText = "The harsher the winter, the more glorious the spring.\n—Yavimaya saying"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a119edc2-9e0f-43d1-a13d-25827f86e3e3.jpg?1783926824"
    }
}
