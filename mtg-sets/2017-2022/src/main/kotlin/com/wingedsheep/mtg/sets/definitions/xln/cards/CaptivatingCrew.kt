package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val CaptivatingCrew = card("Captivating Crew") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    power = 4
    toughness = 3
    oracleText = "{3}{R}: Gain control of target creature an opponent controls until end of turn. " +
        "Untap that creature. It gains haste until end of turn. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
        description = "{3}{R}: Gain control of target creature an opponent controls until end of turn. " +
            "Untap it. It gains haste until end of turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Winona Nelson"
        flavorText = "\"We sail. We plunder. Then we dance.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c76e66c1-dbe2-4346-b4b3-20e8386e7d76.jpg?1783935748"
    }
}
