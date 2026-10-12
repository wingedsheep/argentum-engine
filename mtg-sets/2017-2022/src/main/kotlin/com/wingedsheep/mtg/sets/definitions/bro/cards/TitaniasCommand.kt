package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Titania's Command
 * {4}{G}{G}
 * Sorcery
 *
 * Choose two —
 * • Exile target player's graveyard. You gain 1 life for each card exiled this way.
 * • Search your library for up to two land cards, put them onto the battlefield tapped, then shuffle.
 * • Create two 2/2 green Bear creature tokens.
 * • Put two +1/+1 counters on each creature you control.
 *
 * The life gain counts the cards that actually reached exile (`moveTracked`), not the graveyard's
 * size before the move. Modes resolve in printed order, so Bears made by the third mode get the
 * counters from the fourth.
 */
val TitaniasCommand = card("Titania's Command") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose two —\n" +
        "• Exile target player's graveyard. You gain 1 life for each card exiled this way.\n" +
        "• Search your library for up to two land cards, put them onto the battlefield tapped, then shuffle.\n" +
        "• Create two 2/2 green Bear creature tokens.\n" +
        "• Put two +1/+1 counters on each creature you control."

    spell {
        modal(chooseCount = 2) {
            mode("Exile target player's graveyard. You gain 1 life for each card exiled this way") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
                    val exiled = moveTracked(graveyard, CardDestination.ToZone(Zone.EXILE, player.asPlayer))
                    run(Effects.GainLife(DynamicAmounts.distinctEntitiesIn(exiled)))
                }
            }
            mode("Search your library for up to two land cards, put them onto the battlefield tapped, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Land,
                    count = 2,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                )
            }
            mode("Create two 2/2 green Bear creature tokens") {
                effect = Effects.CreateToken(
                    count = 2,
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Bear"),
                    imageUri = "https://cards.scryfall.io/normal/front/7/7/772dac39-269b-4a35-aad3-320279af833f.jpg?1783919910"
                )
            }
            mode("Put two +1/+1 counters on each creature you control") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/753567c7-c484-4c0d-8128-7710e23c428f.jpg?1783920039"
        ruling(
            "2022-10-14",
            "If you choose the first mode and that player is an illegal target by the time Titania's " +
                "Command would resolve, the spell is removed from the stack and none of its effects happen."
        )
    }
}
