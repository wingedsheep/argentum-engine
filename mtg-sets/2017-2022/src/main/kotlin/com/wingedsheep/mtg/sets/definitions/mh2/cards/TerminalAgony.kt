package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Terminal Agony — Modern Horizons 2 #215
 * {2}{B}{R} · Sorcery
 *
 * Destroy target creature.
 * Madness {B}{R} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)
 *
 * [Effects.Destroy] is a `MoveToZoneEffect` to the graveyard with `byDestruction = true`, which is
 * what lets indestructible, regeneration and totem armor see the destruction rather than a plain
 * zone change (CR 701.7). No "can't be regenerated" rider is printed, so the plain form is right.
 *
 * Madness (CR 702.35) turns a four-mana sorcery into two-mana instant-speed removal whenever
 * something discards it — the madness cast happens while the reflexive trigger resolves, so the
 * sorcery timing restriction never applies. `CardBuilder.build()` derives the printed
 * `Keyword.MADNESS` from `madness(...)`.
 */
val TerminalAgony = card("Terminal Agony") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature.\n" +
        "Madness {B}{R} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.Destroy(t)
    }

    madness("{B}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Lucas Graciano"
        flavorText = "His mouth melted to slag, yet somehow he kept screaming."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/314e94ad-0e12-48bb-aae1-2c842943114a.jpg?1783926810"
    }
}
