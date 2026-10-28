package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

/**
 * Afterlife from the Loam — Tarkir: Dragonstorm Commander #25
 * {5}{B}{B}{B} · Sorcery
 *
 * Delve
 * For each player, choose up to one target creature card in that player's graveyard. Put those
 * cards onto the battlefield under your control. They're Zombies in addition to their other types.
 *
 * "For each player, up to one target … in that player's graveyard" is one target declaration
 * distributed over graveyard owners ([OwnerDistribution.UP_TO_ONE], Mysterious Stranger's shape),
 * so zero targets is legal and no graveyard yields two. The surviving targets are gathered and moved
 * together — they enter the battlefield simultaneously under your control (ownership is unchanged,
 * The Rise of Sozin's reanimation) — and only the cards that actually arrived gain the Zombie type,
 * permanently.
 */
val AfterlifeFromTheLoam = card("Afterlife from the Loam") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "For each player, choose up to one target creature card in that player's graveyard. Put those " +
        "cards onto the battlefield under your control. They're Zombies in addition to their other types."

    keywords(Keyword.DELVE)

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature, zone = Zone.GRAVEYARD),
            ownerDistribution = OwnerDistribution.UP_TO_ONE,
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val returned = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(
                Effects.ForEachInCollection(
                    returned,
                    Effects.AddCreatureType("Zombie", EffectTarget.IterationEntity, Duration.Permanent),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9361b548-a8d9-4e57-98cf-0c464742e1c9.jpg?1783907179"
    }
}
