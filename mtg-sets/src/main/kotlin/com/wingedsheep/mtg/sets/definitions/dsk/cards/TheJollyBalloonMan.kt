package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * The Jolly Balloon Man
 * {1}{R}{W}
 * Legendary Creature — Human Clown
 * 1/4
 * Haste
 * {1}, {T}: Create a token that's a copy of another target creature you control, except it's a
 * 1/1 red Balloon creature in addition to its other colors and types and it has flying and
 * haste. Sacrifice it at the beginning of the next end step. Activate only as a sorcery.
 */
val TheJollyBalloonMan = card("The Jolly Balloon Man") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Legendary Creature — Human Clown"
    power = 1
    toughness = 4
    oracleText = "Haste\n{1}, {T}: Create a token that's a copy of another target creature you " +
        "control, except it's a 1/1 red Balloon creature in addition to its other colors and " +
        "types and it has flying and haste. Sacrifice it at the beginning of the next end step. " +
        "Activate only as a sorcery."

    keywords(Keyword.HASTE)

    // "another target creature you control" → TargetOther excludes the source from a
    // controller-scoped creature target. The copy keeps its other colors/types but gains
    // red + Balloon and is fixed at 1/1, with flying and haste added. Haste is a permanent
    // keyword on the copy; since it's sacrificed at the next end step it never outlives the
    // token (same modeling Molten Duplication / Esika's Chariot use).
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val t = target(
            "another target creature you control",
            TargetOther(
                baseRequirement = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Creature.youControl())
                )
            )
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = t,
            overridePower = 1,
            overrideToughness = 1,
            addedColors = setOf(Color.RED),
            addedSubtypes = setOf(Subtype("Balloon")),
            addedKeywords = setOf(Keyword.FLYING, Keyword.HASTE),
            sacrificeAtStep = Step.END,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3c4e2e0-1c0e-475d-a0f4-1be4216c2bad.jpg?1726286682"
    }
}
