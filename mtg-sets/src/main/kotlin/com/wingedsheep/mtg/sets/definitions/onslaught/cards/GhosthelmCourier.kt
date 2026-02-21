package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Ghosthelm Courier
 * {2}{U}
 * Creature — Human Wizard
 * 2/1
 * You may choose not to untap Ghosthelm Courier during your untap step.
 * {2}{U}, {T}: Target Wizard creature gets +2/+2 and gains shroud for as long as Ghosthelm Courier remains tapped.
 */
val GhosthelmCourier = card("Ghosthelm Courier") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "You may choose not to untap Ghosthelm Courier during your untap step.\n{2}{U}, {T}: Target Wizard creature gets +2/+2 and gains shroud for as long as Ghosthelm Courier remains tapped."

    keywords(Keyword.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Wizard"))
        ))
        effect = ModifyStatsEffect(2, 2, t, Duration.WhileSourceTapped()) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.SHROUD, t, Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/large/front/c/d/cd6cc30a-9ed4-4f36-95cb-6f0a2b8dce02.jpg?1562943472"
    }
}
