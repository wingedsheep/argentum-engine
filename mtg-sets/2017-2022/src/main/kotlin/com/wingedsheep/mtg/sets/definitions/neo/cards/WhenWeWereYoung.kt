package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * When We Were Young — Kamigawa: Neon Dynasty #43 (canonical printing)
 * {3}{W} · Instant
 *
 * Up to two target creatures each get +2/+2 until end of turn. If you control an artifact and an
 * enchantment, those creatures also gain lifelink until end of turn.
 *
 * The control check runs on resolution as two independent existence checks, so a single artifact
 * enchantment satisfies it (2022-02-18 rulings).
 */
val WhenWeWereYoung = card("When We Were Young") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Up to two target creatures each get +2/+2 until end of turn. If you control an artifact " +
        "and an enchantment, those creatures also gain lifelink until end of turn."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))
        ) then Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Artifact),
                Conditions.YouControl(GameObjectFilter.Enchantment),
            ),
            then = Effects.ForEachTarget(
                Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.ContextTarget(0))
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Eric Deschamps"
        flavorText = "Since Imperial custom strictly forbade the Emperor to reveal her name—even to Kaito, her " +
            "closest childhood friend—she had to suffer him calling her \"your emperorness\" for much of their " +
            "shared youth."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6db14cb8-9ff8-41ee-a733-4f1130e73b29.jpg?1783923909"
        ruling(
            "2022-02-18",
            "“Those creatures” refers to the target creatures, not the artifact and the enchantment."
        )
        ruling("2022-02-18", "The artifact and the enchantment may be the same permanent.")
        ruling(
            "2022-02-18",
            "Whether you control an artifact and an enchantment is checked as When We Were Young resolves."
        )
    }
}
