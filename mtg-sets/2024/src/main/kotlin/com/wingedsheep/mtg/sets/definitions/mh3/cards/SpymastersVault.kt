package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spymaster's Vault
 * Land
 *
 * This land enters tapped unless you control a Swamp.
 * {T}: Add {B}.
 * {B}, {T}: Target creature you control connives X, where X is the number of creatures that died
 * this turn.
 *
 * Connive X is CR 701.50d's connive N; with no creature dead this turn it is a connive 0, which
 * does nothing and fires no connive trigger (CR 701.50e). "Creatures that died this turn" counts
 * every player's creatures, so it sums the tracker for you and each opponent (as Khabál Ghoul does).
 */
val SpymastersVault = card("Spymaster's Vault") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "This land enters tapped unless you control a Swamp.\n" +
        "{T}: Add {B}.\n" +
        "{B}, {T}: Target creature you control connives X, where X is the number of creatures that " +
        "died this turn. (Draw X cards, then discard X cards. Put a +1/+1 counter on that creature " +
        "for each nonland card discarded this way.)"

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Land.withSubtype("Swamp"),
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Connive(
            creature,
            DynamicAmounts.creaturesDiedThisTurn(Player.You) +
                DynamicAmounts.creaturesDiedThisTurn(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d5fbb30-abfc-4e79-8ce5-bbb04a241c9f.jpg?1783911234"
    }
}
