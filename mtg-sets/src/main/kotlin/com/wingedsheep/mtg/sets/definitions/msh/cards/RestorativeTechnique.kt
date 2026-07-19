package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Restorative Technique — Marvel Super Heroes #183
 * {2}{G} · Sorcery · Common
 *
 * Target player gains 2 life, then searches their library for a basic land card, puts it onto
 * the battlefield tapped, then shuffles. Put a +1/+1 counter on up to one target creature.
 *
 * Two independent targets: a player (index 0) and an optional creature (index 1). The ramp is
 * performed by the *targeted player*, not the caster — a Gather → Select → Move pipeline
 * scoped to them via [Player.TargetPlayer] / [Chooser.TargetPlayer]. The selection is
 * `chooseUpTo(1)` because a player searching a library is never required to find a matching
 * card, and the shuffle happens regardless. `EmitLibrarySearchedEventEffect` fires the
 * "whenever a player searches their library" batch after the search completes.
 */
val RestorativeTechnique = card("Restorative Technique") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target player gains 2 life, then searches their library for a basic land " +
        "card, puts it onto the battlefield tapped, then shuffles. Put a +1/+1 counter on up " +
        "to one target creature."

    spell {
        val player = target("target player", TargetPlayer())
        val creature = target(
            "up to one target creature",
            TargetCreature(optional = true, filter = TargetFilter.Creature)
        )

        effect = Effects.Composite(
            Effects.GainLife(2, player),
            Effects.Pipeline {
                val basics = gather(
                    CardSource.FromZone(Zone.LIBRARY, Player.TargetPlayer, GameObjectFilter.BasicLand),
                    name = "basicLands"
                )
                val found = chooseUpTo(
                    1,
                    from = basics,
                    chooser = Chooser.TargetPlayer,
                    prompt = "Search your library for a basic land card",
                    name = "foundBasic"
                )
                move(
                    found,
                    CardDestination.ToZone(Zone.BATTLEFIELD, Player.TargetPlayer, ZonePlacement.Tapped)
                )
                run(ShuffleLibraryEffect(player))
                run(EmitLibrarySearchedEventEffect)
            },
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Kevin Glint"
        flavorText = "\"Mastery is more than body and mind. You must also know your place in " +
            "the universe.\"\n—Shang-Chi"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbe9102d-b92b-4c32-93f6-5b6dc0fa08e6.jpg?1783902913"
    }
}
