package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Minas Morgul, Dark Fortress
 * Legendary Land
 *
 * Minas Morgul enters tapped.
 * {T}: Add {B}.
 * {3}{B}, {T}: Put a shadow counter on target creature. For as long as that creature has a shadow
 * counter on it, it's a Wraith in addition to its other types.
 *
 * The shadow counter is a keyword counter (CR 122.1b): the creature has shadow while it carries one.
 * The Wraith type is keyed to the same counter through [Duration.WhileAffectedHasCounter], so removing
 * the counter ends both halves.
 */
val MinasMorgulDarkFortress = card("Minas Morgul, Dark Fortress") {
    typeLine = "Legendary Land"
    colorIdentity = "B"
    oracleText = "Minas Morgul enters tapped.\n" +
        "{T}: Add {B}.\n" +
        "{3}{B}, {T}: Put a shadow counter on target creature. For as long as that creature has a " +
        "shadow counter on it, it's a Wraith in addition to its other types. (A creature with shadow " +
        "can block or be blocked by only creatures with shadow.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.SHADOW, 1, creature) then
            Effects.AddCreatureType("Wraith", creature, Duration.WhileAffectedHasCounter(CounterType.SHADOW))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "514"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50f76652-15e7-4190-842e-db6db6d66e91.jpg?1783914191"
        ruling("2023-11-03", "A permanent with a shadow counter on it has shadow.")
        ruling("2023-11-03", "Once a creature has been blocked, that creature remains blocked and will deal and be dealt combat damage even if it gains or loses shadow or if the blocking creature gains or loses shadow.")
    }
}
