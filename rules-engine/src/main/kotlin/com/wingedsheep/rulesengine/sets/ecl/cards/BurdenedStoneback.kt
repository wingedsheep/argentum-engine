package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.AddCountersEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object BurdenedStoneback {
    val definition = CardDefinition.creature(
        name = "Burdened Stoneback",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.GIANT, Subtype.WARRIOR),
        power = 4,
        toughness = 4,
        oracleText = "This creature enters with two -1/-1 counters on it.\n" +
                "{1}{W}, Remove a counter from this creature: Target creature gains indestructible until end of turn. " +
                "Activate only as a sorcery.",
        metadata = ScryfallMetadata(
            collectorNumber = "8",
            rarity = Rarity.UNCOMMON,
            artist = "Carl Critchlow",
            imageUri = "https://cards.scryfall.io/normal/front/3/2/3278b8d0-3d2b-4d3d-bbf1-fd9b714b53ed.jpg",
            scryfallId = "3278b8d0-3d2b-4d3d-bbf1-fd9b714b53ed",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Burdened Stoneback") {
        // ETB: Enter with two -1/-1 counters
        triggered(
            trigger = OnEnterBattlefield(),
            effect = AddCountersEffect(
                counterType = "-1/-1",
                count = 2,
                target = EffectTarget.Self
            )
        )

        // Activated ability: {1}{W}, Remove a counter: Target creature gains indestructible until EOT
        // Activate only as a sorcery
        activated(
            cost = AbilityCost.Composite(
                listOf(
                    AbilityCost.Mana(white = 1, generic = 1),
                    AbilityCost.RemoveCounter(counterType = "any", count = 1)
                )
            ),
            effect = GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.INDESTRUCTIBLE,
                target = EffectTarget.TargetCreature
            ),
            timing = TimingRestriction.SORCERY
        )
    }
}