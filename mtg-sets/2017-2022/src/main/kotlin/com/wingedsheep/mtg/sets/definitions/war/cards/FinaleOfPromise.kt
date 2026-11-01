package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Finale of Promise — War of the Spark #127 (canonical printing)
 * {X}{R}{R} · Sorcery
 *
 * You may cast up to one target instant card and/or up to one target sorcery card from your
 * graveyard each with mana value X or less without paying their mana costs. If a spell cast this
 * way would be put into your graveyard, exile it instead. If X is 10 or more, copy each of those
 * spells twice. You may choose new targets for the copies.
 *
 * - Two optional target slots, each capped at mana value X by `manaValueAtMostDynamic(xValue)` —
 *   checked when targets are chosen and again on resolution (Dance of the Manse's shape).
 * - The targets are cast through the capped free-cast loop (`maxCasts = 2`, Invoke Calamity's
 *   primitive), which offers the cards one at a time, so the caster picks the order and may skip
 *   either; the loop stamps the exile-instead rider on each card it casts.
 * - "Those spells" are the targets that left the graveyard: targets minus what is still there. A
 *   card the caster declined, or that couldn't be cast, is still in the graveyard and drops out.
 * - X ≥ 10: the caster picks which spell is copied first (its two copies go on the stack first and
 *   so resolve after the other's — "put the copies on the stack in any order"), then each spell is
 *   copied twice with new targets allowed. Copies are created, not cast.
 */
val FinaleOfPromise = card("Finale of Promise") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You may cast up to one target instant card and/or up to one target sorcery card " +
        "from your graveyard each with mana value X or less without paying their mana costs. If a " +
        "spell cast this way would be put into your graveyard, exile it instead. If X is 10 or more, " +
        "copy each of those spells twice. You may choose new targets for the copies."

    spell {
        target(
            TargetFilter(
                GameObjectFilter.Instant.ownedByYou().manaValueAtMostDynamic(DynamicAmounts.xValue()),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )
        target(
            TargetFilter(
                GameObjectFilter.Sorcery.ownedByYou().manaValueAtMostDynamic(DynamicAmounts.xValue()),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )

        fun promise(copyTwice: Boolean) = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            run(
                Effects.CastUpToNFromCollectionWithoutPayingCost(
                    targeted,
                    maxCasts = 2,
                    insteadOfGraveyard = AfterResolveDestination.EXILE,
                )
            )
            if (copyTwice) {
                val stillInGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
                val castThisWay = exclude(targeted, minus = stillInGraveyard)
                val (copiedFirst, copiedSecond) = chooseExactlySplit(
                    1,
                    from = castThisWay,
                    prompt = "Choose the spell to copy first (its copies resolve after the other's)",
                )
                fun copyEachTwice(spells: CollectionSlot) = Effects.ForEachInCollection(
                    spells,
                    Effects.CopyTargetSpell(
                        target = EffectTarget.IterationEntity,
                        copies = DynamicAmounts.fixed(2),
                    ),
                )
                run(copyEachTwice(copiedFirst))
                run(copyEachTwice(copiedSecond))
            }
        }

        effect = Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 10),
            then = promise(copyTwice = true),
            otherwise = promise(copyTwice = false),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "127"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/811b2dda-e1b7-4a46-83cc-5cdc17554836.jpg?1783933429"
        ruling("2019-05-03", "As Finale of Promise resolves, first you cast the target instant card and/or the target sorcery card in either order. Then, if X is 10 or more, you copy each of those twice and put the copies on the stack in any order. The copies will resolve before the original spells.")
        ruling("2019-05-03", "A split card that's an instant and a sorcery card may be the target of Finale of Promise twice. However, once you cast that card once, it can't be cast a second time.")
        ruling("2019-05-03", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2019-05-03", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, such as that of Spark Harvest, you must pay those to cast the card.")
        ruling("2019-05-03", "If the spell that's copied is modal (that is, it says \"Choose one—\" or the like), the copies will have the same mode or modes. You can't choose different ones.")
        ruling("2019-05-03", "If the spell has damage divided as it was cast, the division can't be changed (although the targets receiving that damage still can). The same is true of spells that distribute counters.")
        ruling("2019-05-03", "You can't choose to pay any additional costs for the copies. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
        ruling("2019-05-03", "The copies that Finale of Promise creates are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
    }
}
