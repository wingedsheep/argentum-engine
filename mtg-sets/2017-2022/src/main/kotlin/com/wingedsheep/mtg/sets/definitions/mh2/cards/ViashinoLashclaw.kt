package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Viashino Lashclaw — Modern Horizons 2 #146
 * {1}{R} · Creature — Lizard Warrior · 2 / 2
 *
 * {T}, Discard a card: Creatures you control gain haste until end of turn.
 *
 * The cost is [Costs.DiscardCard] — discard *any* card from hand — not `Costs.DiscardSelf`,
 * which is the cycling-style "discard this card".
 *
 * "Creatures you control gain haste" is a group grant, not a targeted one: [Effects.ForEachInGroup]
 * iterates the battlefield group and [EffectTarget.Self] inside the body names the *iterated*
 * creature, so each one picks up its own until-end-of-turn haste grant. The set is locked in on
 * resolution, so a creature that enters afterwards this turn does not get haste (CR 611.2c).
 */
val ViashinoLashclaw = card("Viashino Lashclaw") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    power = 2
    toughness = 2
    oracleText = "{T}, Discard a card: Creatures you control gain haste until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
        )
        description = "Creatures you control gain haste until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Caio Monteiro"
        flavorText = "\"Victory is the greatest motivator of all. So sayeth the bey.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3457e346-a5de-4624-b577-f59d4c186537.jpg?1783926836"
    }
}
