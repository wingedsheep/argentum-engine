package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dark Dabbling
 * {2}{B}
 * Instant
 *
 * Regenerate target creature. Draw a card.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, also
 * regenerate each other creature you control.
 *
 * Spell mastery is checked as the spell resolves, *after* the draw (a dredged draw can enable it),
 * so the [Conditions.CardsInGraveyardMatchingAtLeast] check sits last in the sequence. The mass
 * regeneration is a per-creature shield (`ForEachInGroup` around `Regenerate(IterationEntity)`, as
 * Loxodon Hierarch does) over the creatures you control minus the target (`otherThanTarget()`), which
 * already has its own shield. An illegal target fizzles the whole spell: no draw, no shields.
 */
val DarkDabbling = card("Dark Dabbling") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Regenerate target creature. Draw a card. (The next time the creature would be " +
        "destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "also regenerate each other creature you control."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Regenerate(t) then
            Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                then = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()).otherThanTarget(),
                    Effects.Regenerate(EffectTarget.IterationEntity)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d5a047-2315-41a5-a78d-6a690622c457.jpg?1783938344"
        ruling("2018-12-07", "If the target creature is an illegal target by the time Dark Dabbling tries to resolve, the ability doesn't resolve. You don't draw a card and no creatures are regenerated.")
        ruling("2018-12-07", "Dark Dabbling's spell mastery ability is a normal spell effect, not a replacement effect. This means that if you replace drawing a card for Dark Dabbling's effect with dredging a card from your graveyard, the cards put into your graveyard this way may enable the additional regeneration.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
