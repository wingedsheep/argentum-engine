package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Skyserpent Seeker — Aetherdrift #224
 * {G}{U} · Creature — Snake · 1/1
 *
 * Flying, deathtouch
 * Exhaust — {4}: Reveal cards from the top of your library until you reveal two land cards. Put
 * those land cards onto the battlefield tapped and the rest on the bottom of your library in a
 * random order. Put a +1/+1 counter on this creature.
 *
 * `gatherUntilMatch(count = 2)` is the reveal walk: it stops after the second land card and hands
 * back both the matched lands and every card revealed along the way (the matches included), so the
 * cards that go to the bottom are `revealed − matched` ([exclude]). If the library runs out before
 * two lands turn up, nothing is put onto the battlefield and everything revealed goes to the
 * bottom — the walk simply ends when the library does.
 *
 * The counter is placed unconditionally: it's a separate sentence, not contingent on finding lands.
 */
val SkyserpentSeeker = card("Skyserpent Seeker") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Snake"
    oracleText = "Flying, deathtouch\n" +
        "Exhaust — {4}: Reveal cards from the top of your library until you reveal two land cards. " +
        "Put those land cards onto the battlefield tapped and the rest on the bottom of your library " +
        "in a random order. Put a +1/+1 counter on this creature. (Activate each exhaust ability only once.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{4}")
        isExhaust = true
        effect = Effects.Pipeline(
            descriptionOverride = "Reveal cards from the top of your library until you reveal two " +
                "land cards. Put those land cards onto the battlefield tapped and the rest on the " +
                "bottom of your library in a random order. Put a +1/+1 counter on this creature."
        ) {
            val walk = gatherUntilMatch(
                filter = GameObjectFilter.Land,
                count = DynamicAmount.Fixed(2)
            )
            reveal(walk.revealed)
            move(
                walk.match,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
            )
            toLibraryBottom(exclude(walk.revealed, walk.match), order = CardOrder.Random)
            run(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dbd9fc0-c0df-4119-a0d3-2e1790998c21.jpg?1783907852"
    }
}
