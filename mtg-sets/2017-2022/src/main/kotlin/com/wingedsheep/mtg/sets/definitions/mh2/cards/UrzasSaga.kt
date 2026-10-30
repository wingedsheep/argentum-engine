package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urza's Saga — Modern Horizons 2 #259
 * Enchantment Land — Urza's Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — This Saga gains "{T}: Add {C}."
 * II — This Saga gains "{2}, {T}: Create a 0/0 colorless Construct artifact creature token with
 * 'This token gets +1/+1 for each artifact you control.'"
 * III — Search your library for an artifact card with mana cost {0} or {1}, put it onto the
 * battlefield, then shuffle.
 *
 * Chapters I and II grant the Saga itself lasting abilities (Down in the Valley's
 * `Duration.Permanent` self-grant), kept until it leaves (ruling). Chapter III matches the printed
 * cost, not the mana value: `{U}`, `{X}` and no-cost artifacts can't be found (ruling) —
 * `manaCostIs("{0}", "{1}")`.
 */
val UrzasSaga = card("Urza's Saga") {
    typeLine = "Enchantment Land — Urza's Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — This Saga gains \"{T}: Add {C}.\"\n" +
        "II — This Saga gains \"{2}, {T}: Create a 0/0 colorless Construct artifact creature token " +
        "with 'This token gets +1/+1 for each artifact you control.'\"\n" +
        "III — Search your library for an artifact card with mana cost {0} or {1}, put it onto the " +
        "battlefield, then shuffle."

    sagaChapter(1) {
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = AbilityCost.Tap,
                effect = Effects.AddColorlessMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = EffectTarget.Self,
            duration = Duration.Permanent
        )
    }

    sagaChapter(2) {
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap),
                effect = Effects.CreateToken(
                    power = 0,
                    toughness = 0,
                    colors = emptySet(),
                    creatureTypes = setOf("Construct"),
                    artifactToken = true,
                    staticAbilities = listOf(
                        GrantDynamicStats(
                            filter = GroupFilter.source(),
                            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
                            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
                        )
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7caaf39-8f16-4f1d-bee6-a45674306319.jpg?1783926587"
                ),
                descriptionOverride = "{2}, {T}: Create a 0/0 colorless Construct artifact creature token " +
                    "with \"This token gets +1/+1 for each artifact you control.\""
            ),
            target = EffectTarget.Self,
            duration = Duration.Permanent
        )
    }

    sagaChapter(3) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaCostIs("{0}", "{1}"),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1e0f201-42cb-46a1-901a-65bb4fc18f6c.jpg?1783926791"
    }
}
