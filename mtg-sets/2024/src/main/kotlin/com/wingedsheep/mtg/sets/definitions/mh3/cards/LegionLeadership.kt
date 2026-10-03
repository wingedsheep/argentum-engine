package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Legion Leadership {1}{R/W} // Legion Stronghold
 * Instant
 * Until end of turn, double target creature's power and it gains first strike.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {R} or {W}.
 *
 * "Double its power" is +X/+0 where X is the creature's power as the spell resolves (ruling), so a
 * negative power gets more negative — exactly what `ModifyStats(powerOf(creature), 0)` does.
 */
private val LegionLeadershipFront = card("Legion Leadership") {
    manaCost = "{1}{R/W}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Until end of turn, double target creature's power and it gains first strike."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = DynamicAmounts.powerOf(creature),
            toughness = DynamicAmounts.fixed(0),
            target = creature,
        ) then Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Ryan Valle"
        flavorText = "\"The firemane angels are mighty, but their greatest value lies in the fervor their " +
            "presence kindles in the heart of a soldier.\"\n—Agrus Kos"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7676abd9-0a3d-4721-b17b-778d2e3c2e25.jpg?1783911225"
        ruling("2024-06-07", "To double a creature's power, that creature gets +X/+0, where X is that creature's power when Legion Leadership resolves.")
    }
}

private val LegionStrongholdBack = card("Legion Stronghold") {
    typeLine = "Land"
    colorIdentity = "WR"
    oracleText = "This land enters tapped.\n{T}: Add {R} or {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Ryan Valle"
        flavorText = "Boros fortresses bristle with high towers so that, even at rest, Legion soldiers can " +
            "remain in the light of the sun."
        imageUri = "https://cards.scryfall.io/normal/back/7/6/7676abd9-0a3d-4721-b17b-778d2e3c2e25.jpg?1783911225"
    }
}

val LegionLeadership: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = LegionLeadershipFront,
    backFace = LegionStrongholdBack,
)
