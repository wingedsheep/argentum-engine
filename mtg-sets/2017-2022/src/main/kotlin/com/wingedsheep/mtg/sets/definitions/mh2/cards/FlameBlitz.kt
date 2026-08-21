package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flame Blitz — Modern Horizons 2 #124
 * {R} · Enchantment
 *
 * At the beginning of your end step, this enchantment deals 5 damage to each planeswalker.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * "Each planeswalker" is untargeted group damage, so it is [Effects.ForEachInGroup] over a
 * [GroupFilter] of every planeswalker on the battlefield — no controller predicate, so the
 * enchantment burns its own controller's walkers too. Inside the loop, `EffectTarget.Self` names
 * the *iterated* permanent (the group member currently being processed), not the enchantment; the
 * damage source stays Flame Blitz because the ability's source is the enchantment.
 *
 * The whole board is hit by one ability resolution, so damage is dealt simultaneously and loyalty
 * is checked once by state-based actions afterwards (CR 704.5i).
 */
val FlameBlitz = card("Flame Blitz") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, this enchantment deals 5 damage to each planeswalker.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker),
            DealDamageEffect(5, EffectTarget.Self)
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Colin Boyer"
        flavorText = "\"Blast it, not again!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a93c6ba8-666b-4c05-8137-8ffa1d5d928b.jpg?1783926845"
    }
}
