package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Winter, Cursed Rider — Aetherdrift #228
 * {U}{B} · Legendary Creature — Human Warlock · 3/2
 *
 * Ward—Pay 2 life.
 * Artifacts you control have "Ward—Pay 2 life."
 * Exhaust — {2}{U}{B}, {T}, Exile X artifact cards from your graveyard: Each other nonartifact
 * creature gets -X/-X until end of turn. (Activate each exhaust ability only once.)
 *
 * Two distinct instances of ward, printed separately and modelled separately: the intrinsic
 * [KeywordAbility.wardLife] on Winter, and a [GrantWard] lord over the artifacts. Per the printed
 * ruling, an effect that makes Winter an artifact gives it a *second* instance of ward — which is
 * exactly what falls out of the lord filter matching Winter once it's an artifact, since the
 * filter carries no `excludeSelf`.
 *
 * The exhaust ability's X is not a mana {X} — the printed cost has none. It is bound entirely by
 * [Costs.ExileXFromGraveyard]: activating pauses for a selection over the artifact cards in your
 * graveyard, and however many you pick *is* X, read back here as [DynamicAmount.XValue]. Picking
 * none is legal and makes X zero. The -X/-X is the negation of that same X, applied through
 * [Effects.ForEachInGroup] with `excludeSelf` for the printed "each **other** nonartifact
 * creature" — a board-wide sweep across all players, not just yours, and one Winter itself dodges
 * (it is a nonartifact creature).
 */
private val OtherNonartifactCreatures =
    GroupFilter(GameObjectFilter.Creature.nonartifact(), excludeSelf = true)

val WinterCursedRider = card("Winter, Cursed Rider") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 2
    oracleText = "Ward—Pay 2 life.\n" +
        "Artifacts you control have \"Ward—Pay 2 life.\"\n" +
        "Exhaust — {2}{U}{B}, {T}, Exile X artifact cards from your graveyard: Each other " +
        "nonartifact creature gets -X/-X until end of turn. (Activate each exhaust ability only once.)"

    keywordAbility(KeywordAbility.wardLife(2))

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Life(2),
            filter = GroupFilter(GameObjectFilter.Artifact.youControl())
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}{B}"),
            Costs.Tap,
            Costs.ExileXFromGraveyard(GameObjectFilter.Artifact)
        )
        isExhaust = true
        val negX = DynamicAmount.Multiply(DynamicAmount.XValue, -1)
        effect = Effects.ForEachInGroup(
            OtherNonartifactCreatures,
            Effects.ModifyStats(negX, negX, EffectTarget.Self)
        )
        description = "Exhaust — {2}{U}{B}, {T}, Exile X artifact cards from your graveyard: " +
            "Each other nonartifact creature gets -X/-X until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02d46d0e-3161-45b5-a49e-5cd592c67ddd.jpg?1783907852"
        ruling("2025-02-07", "Multiple instances of ward each trigger separately.")
        ruling(
            "2025-02-07",
            "If an effect causes Winter to become an artifact, its second ability will cause it " +
                "to gain a second instance of ward."
        )
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves " +
                "the battlefield and returns to the battlefield, it becomes a new object so its " +
                "exhaust ability can be activated again."
        )
    }
}
