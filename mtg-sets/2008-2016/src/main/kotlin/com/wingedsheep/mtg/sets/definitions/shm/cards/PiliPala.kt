package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Pili-Pala
 * {2}
 * Artifact Creature — Scarecrow
 * 1 / 1
 *
 * Flying
 * {2}, {Q}: Add one mana of any color. ({Q} is the untap symbol.)
 *
 * - The ability produces mana and doesn't target, so it is a mana ability (CR 605.1a):
 *   `manaAbility = true` plus [TimingRule.ManaAbility], the same pairing Shire Scarecrow uses.
 * - `{Q}` is [Costs.Untap] — Pili-Pala must already be **tapped**, and CR 302.6 gates the untap
 *   symbol behind summoning sickness. That is what makes the classic Grand Architect loop need a
 *   cost reducer rather than just two permanents.
 * - [Effects.AddManaOfChoice] with no arguments is exactly "add one mana of any color": it defaults
 *   to `ManaColorSet.AnyColor` and one mana.
 */
val PiliPala = card("Pili-Pala") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "{2}, {Q}: Add one mana of any color. ({Q} is the untap symbol.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Untap)
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{2}, {Q}: Add one mana of any color."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "258"
        artist = "Ron Spencer"
        flavorText = "It wasn't really expected to fly. Then again, it wasn't expected to move, either."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4892c152-1f4a-4616-8e7f-0ca4911e621a.jpg?1783942710"
    }
}
