package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity

/**
 * Prison Break — Marvel's Spider-Man #61
 * {4}{B} · Sorcery
 *
 * Return target creature card from your graveyard to the battlefield with an additional
 * +1/+1 counter on it.
 * Mayhem {3}{B}
 *
 * The reanimated permanent keeps its entity id across the graveyard→battlefield move, so the
 * follow-up counter lands on the same object ([Effects.Move] then [Effects.AddCounters] on the
 * same target reference).
 */
val PrisonBreak = card("Prison Break") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield with an " +
        "additional +1/+1 counter on it.\n" +
        "Mayhem {3}{B} (You may cast this card from your graveyard for {3}{B} if you discarded it " +
        "this turn. Timing rules still apply.)"

    spell {
        val creatureCard = target(
            "target creature card from your graveyard",
            Targets.CreatureCardInYourGraveyard
        )
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creatureCard))
    }

    mayhem("{3}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "John Tyler Christopher"
        flavorText = "\"Anyone who's tired of the slammer, follow us!\"\n—Electro, Max Dillon"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c45a5df-048e-4b73-89c6-5cdaa330319e.jpg?1783905344"
    }
}
