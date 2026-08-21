package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rishadan Dockhand — Modern Horizons 2 #59
 * {U} · Creature — Merfolk · 1 / 2
 *
 * Islandwalk (This creature can't be blocked as long as defending player controls an Island.)
 * {1}, {T}: Tap target land.
 *
 * Islandwalk is engine-live evasion read by the block-legality rules, so the bare
 * [Keyword.ISLANDWALK] enum is the whole first line — no lowering needed.
 *
 * The activated ability taps *any* land, not just an opponent's: [Targets.Land] carries no
 * controller predicate, matching the Oracle wording. Tapping is [Effects.Tap] on the bound
 * target rather than a cost-side tap; the `{T}` in the cost belongs to the Dockhand itself.
 */
val RishadanDockhand = card("Rishadan Dockhand") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    power = 1
    toughness = 2
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
        "{1}, {T}: Tap target land."

    keywords(Keyword.ISLANDWALK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val land = target("target land", Targets.Land)
        effect = Effects.Tap(land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Manuel Castañón"
        flavorText = "It's not hard to find work in Rishada, so long as you've got a strong back and don't ask too many questions."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc488a4c-2885-4727-8317-da93aee8fced.jpg?1783926871"
    }
}
