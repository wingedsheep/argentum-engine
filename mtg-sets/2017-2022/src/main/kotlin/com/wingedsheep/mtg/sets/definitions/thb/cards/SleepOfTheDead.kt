package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sleep of the Dead
 * {U}
 * Sorcery
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Escape—{2}{U}, Exile three other cards from your graveyard.
 *
 * Crippling Chill's tap-and-freeze (DOESNT_UNTAP keyed to the affected creature's controller's next
 * untap step, so an already-tapped target is frozen too) plus Satyr's Cunning's escape with a
 * three-card exile. The escaped sorcery returns to the graveyard after resolving (ruling).
 */
val SleepOfTheDead = card("Sleep of the Dead") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Escape—{2}{U}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.UntilAfterAffectedControllersNextUntap())
    }

    keywordAbility(KeywordAbility.escape("{2}{U}", Costs.additional.ExileOtherCards(3)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Kari Christensen"
        flavorText = "Normally, a cerberus sleeps one head at a time."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/4007572b-a6c8-4a56-b1a7-ff099189c9c0.jpg?1783931579"
        ruling(
            "2020-01-24",
            "Sleep of the Dead can target a creature that's already tapped. That creature won't untap " +
                "during its controller's next untap step."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
