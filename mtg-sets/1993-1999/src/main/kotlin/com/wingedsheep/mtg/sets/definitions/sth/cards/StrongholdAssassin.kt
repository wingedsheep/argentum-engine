package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Stronghold Assassin
 * {1}{B}{B}
 * Creature — Phyrexian Zombie Assassin
 * 2/1
 * {T}, Sacrifice a creature: Destroy target nonblack creature.
 *
 * Two cost atoms in printed order — [Costs.Composite] of [Costs.Tap] (the `{T}` symbol, so
 * summoning sickness applies) and [Costs.Sacrifice] over `GameObjectFilter.Creature`. The target
 * is a [TargetCreature] narrowed with `TargetFilter.Creature.notColor(BLACK)`, which reads the
 * creature's *current* colours off projected state, and [Effects.Destroy] is the facade for the
 * `byDestruction` move to the graveyard (regeneration still applies — nothing here forbids it).
 */
val StrongholdAssassin = card("Stronghold Assassin") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Zombie Assassin"
    power = 2
    toughness = 1
    oracleText = "{T}, Sacrifice a creature: Destroy target nonblack creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Matthew D. Wilson"
        flavorText = "The assassin sees only throats and hears only heartbeats."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc0f043c-efb3-4392-ae25-b3ec180b0cb2.jpg"
    }
}
