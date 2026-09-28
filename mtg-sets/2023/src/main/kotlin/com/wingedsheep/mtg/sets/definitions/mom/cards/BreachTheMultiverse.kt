package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Breach the Multiverse
 * {5}{B}{B}
 * Sorcery
 *
 * Each player mills ten cards. For each player, choose a creature or planeswalker card in that
 * player's graveyard. Put those cards onto the battlefield under your control. Then each creature
 * you control becomes a Phyrexian in addition to its other types.
 *
 * Every player mills first (one iteration each), then a per-player sub-pipeline has *you* pick
 * (`Chooser.SourceController`, since the iterated player is rebound to `Player.You`) one card from
 * that player's graveyard. Picks accumulate into one collection that enters under the caster's
 * control. The Phyrexian type is permanent and applied to every creature you control afterwards.
 */
val BreachTheMultiverse = card("Breach the Multiverse") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player mills ten cards. For each player, choose a creature or planeswalker card in that player's graveyard. Put those cards onto the battlefield under your control. Then each creature you control becomes a Phyrexian in addition to its other types."

    spell {
        effect = Effects.Pipeline {
            run(Effects.ForEachPlayer(players = Player.Each, effects = Patterns.Library.mill(10).effects))
            val (chosen) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val candidates = gather(
                    CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker
                    )
                )
                val pick = chooseExactly(
                    1,
                    from = candidates,
                    chooser = Chooser.SourceController,
                    prompt = "Choose a creature or planeswalker card in this graveyard"
                )
                listOf(pick)
            }
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.AddCreatureType("Phyrexian", EffectTarget.IterationEntity, Duration.Permanent)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Liiga Smilshkalne"
        flavorText = "\"All worlds will know perfection.\"\n—Elesh Norn"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daf51a76-7a57-4462-ae18-a19e817e49e5.jpg?1783917016"
    }
}
