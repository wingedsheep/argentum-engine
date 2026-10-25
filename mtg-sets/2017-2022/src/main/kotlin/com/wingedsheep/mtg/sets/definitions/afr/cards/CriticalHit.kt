package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Critical Hit
 * {1}{R}
 * Instant
 *
 * Target creature gains double strike until end of turn.
 * When you roll a natural 20, return this card from your graveyard to your hand. (A natural 20 is
 * a roll that displays 20 on the die.)
 *
 * The recursion reads the die's natural result (CR 706.2) through `rollsDie(naturalResult = 20)`,
 * so a modifier that pushes a lower face to 20 doesn't return it, and one that lowers a natural 20
 * still does. It functions only from the graveyard (`triggerZone`).
 */
val CriticalHit = card("Critical Hit") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gains double strike until end of turn.\n" +
        "When you roll a natural 20, return this card from your graveyard to your hand. " +
        "(A natural 20 is a roll that displays 20 on the die.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
    }

    triggeredAbility {
        trigger = Triggers.you.rollsDie(naturalResult = 20)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Francisco Miyara"
        flavorText = "\"How do you kill what's already dead? *That's* how.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2e9aaaf-0b51-4721-b75c-923892a41743.jpg?1783926482"
    }
}
