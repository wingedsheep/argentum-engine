package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * White Tiger, Ava Ayala — Marvel Super Heroes #196 (uncommon)
 * {1}{G} · Legendary Creature — Human Hero · 2/2
 *
 * Power-up — {5}{G}: Put a +1/+1 counter on White Tiger and create The Tiger God, a legendary
 * 4/4 green Cat God creature token with "The Tiger God can't be blocked by more than one
 * creature." (Activate each power-up ability only once. Reduce the cost by her mana cost if she
 * entered this turn.)
 *
 * The set's one *named* token, so it carries `name = "The Tiger God"` alongside
 * `legendary = true` — the name is what the legend rule keys on, and without it two White Tigers
 * would happily leave two Tiger Gods on the battlefield.
 *
 * Its printed ability is the ordinary [CantBeBlockedByMoreThan] static with `maxBlockers = 1`,
 * passed through `staticAbilities` so it lives on the token rather than being granted to it.
 *
 * `{5}{G}` − `{1}{G}` = `{4}`.
 */
val WhiteTigerAvaAyala = card("White Tiger, Ava Ayala") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Hero"
    oracleText = "Power-up — {5}{G}: Put a +1/+1 counter on White Tiger and create The Tiger God, " +
        "a legendary 4/4 green Cat God creature token with \"The Tiger God can't be blocked by " +
        "more than one creature.\" (Activate each power-up ability only once. Reduce the cost by " +
        "her mana cost if she entered this turn.)"
    power = 2
    toughness = 2

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{G}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf(Subtype.CAT.value, Subtype.GOD.value),
                name = "The Tiger God",
                legendary = true,
                staticAbilities = listOf(CantBeBlockedByMoreThan(maxBlockers = 1)),
                imageUri = "https://cards.scryfall.io/normal/front/4/d/4d1ca2ed-c987-4f92-ad7b-991d7a64d145.jpg?1783902800"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Jennifer Hrabota Lesser"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1589e2e-32a8-48b8-93eb-a9af344e7084.jpg?1783902908"
    }
}
