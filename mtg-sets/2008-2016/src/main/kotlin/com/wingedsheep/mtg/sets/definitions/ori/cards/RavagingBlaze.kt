package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravaging Blaze
 * {X}{R}{R}
 * Instant
 *
 * Ravaging Blaze deals X damage to target creature.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * Ravaging Blaze also deals X damage to that creature's controller.
 *
 * Only the creature is targeted; its controller is read off it at resolution
 * ([EffectTarget.TargetController]). If the creature has become an illegal target the spell
 * doesn't resolve at all, so neither damage happens. Spell mastery is a resolution-time
 * [Conditions.CardsInGraveyardMatchingAtLeast] over instant and/or sorcery cards; Ravaging Blaze
 * itself is still on the stack then, so it never counts.
 */
val RavagingBlaze = card("Ravaging Blaze") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Ravaging Blaze deals X damage to target creature.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "Ravaging Blaze also deals X damage to that creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature) then Effects.If(
            condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
            then = Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.TargetController),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Aleksi Briclot"
        flavorText = "In time, not even the fire monks could keep up with Chandra."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d9404b2-f0ea-4a31-bc7b-6748574c57d3.jpg?1783938326"
        ruling("2015-06-22", "Ravaging Blaze targets only the creature. It doesn't target any player, even if the spell mastery ability applies.")
        ruling("2015-06-22", "If the creature becomes an illegal target by the time Ravaging Blaze tries to resolve, Ravaging Blaze won't resolve and none of its effects will happen. No damage will be dealt to the creature or its controller.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
