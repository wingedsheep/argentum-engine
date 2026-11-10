package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CategoryPickPurpose
import com.wingedsheep.sdk.scripting.references.Player

/**
 * To the Slaughter — Shadows over Innistrad #139
 * {2}{B} · Instant
 *
 * Target player sacrifices a creature or planeswalker of their choice.
 * Delirium — If there are four or more card types among cards in your graveyard, instead that
 * player sacrifices a creature and a planeswalker of their choice.
 *
 * Delirium is checked on resolution ([Effects.If]); without it this is the Umbral Juke edict.
 *
 * With delirium the player must give up two *different* permanents — one a creature, the other a
 * planeswalker — whenever they can (2016-04-08 ruling: a single creature planeswalker can't answer
 * both). A two-category `chooseOnePerCategory` over Creature and Planeswalker would let that one
 * permanent cover both, so the pick is two steps instead:
 *
 * 1. Any creature or planeswalker they control.
 * 2. From the rest, a permanent that completes the pair: a creature if the first pick is a
 *    noncreature planeswalker, any creature or planeswalker if it is both, otherwise (a
 *    nonplaneswalker creature) a planeswalker.
 *
 * Every first pick still leaves a legal second pick whenever two can be sacrificed at all, so the
 * player is never steered into losing only one, and every legal pair is reachable. Nothing moves
 * until both picks are in; `exclude(pool, exclude(rest, second))` is the union of the two picks, so
 * both are sacrificed at once. Types are read off projected state, so an animated planeswalker
 * counts as both.
 */
val ToTheSlaughter = card("To the Slaughter") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target player sacrifices a creature or planeswalker of their choice.\n" +
        "Delirium — If there are four or more card types among cards in your graveyard, instead that " +
        "player sacrifices a creature and a planeswalker of their choice."

    spell {
        val player = target(Targets.Player)
        effect = Effects.If(
            condition = Conditions.Delirium(),
            then = Effects.Pipeline {
                val pool = gather(
                    CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.CreatureOrPlaneswalker)
                )
                val first = chooseOnePerCategory(
                    pool,
                    listOf(GameObjectFilter.CreatureOrPlaneswalker),
                    purpose = CategoryPickPurpose.SACRIFICE,
                )
                val rest = exclude(pool, first)
                ifNotEmpty(first, filter = GameObjectFilter.Planeswalker.notCreature()) {
                    val second = chooseOnePerCategory(
                        rest,
                        listOf(GameObjectFilter.Creature),
                        purpose = CategoryPickPurpose.SACRIFICE,
                    )
                    sacrifice(exclude(pool, exclude(rest, second)))
                } orElse {
                    ifNotEmpty(first, filter = GameObjectFilter.Creature and GameObjectFilter.Planeswalker) {
                        val second = chooseOnePerCategory(
                            rest,
                            listOf(GameObjectFilter.CreatureOrPlaneswalker),
                            purpose = CategoryPickPurpose.SACRIFICE,
                        )
                        sacrifice(exclude(pool, exclude(rest, second)))
                    } orElse {
                        // A nonplaneswalker creature, or nothing at all (then `rest` is empty too).
                        val second = chooseOnePerCategory(
                            rest,
                            listOf(GameObjectFilter.Planeswalker),
                            purpose = CategoryPickPurpose.SACRIFICE,
                        )
                        sacrifice(exclude(pool, exclude(rest, second)))
                    }
                }
            },
            otherwise = Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, target = player),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Christine Choi"
        flavorText = "\"Where has little Gossamer gone?\"\n—Thaniel, Gatstaf shepherd"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a645c8d-7cfb-466b-9fd0-e1c26bf6f652.jpg?1783937761"
        ruling(
            "2016-04-08",
            "If the delirium ability applies, that player must sacrifice a creature object and a planeswalker " +
                "object if able. In the unusual case that the player controls a single permanent that's both a " +
                "creature and a planeswalker, that player must sacrifice another creature or planeswalker " +
                "(unless they control no other creatures and no other planeswalkers)."
        )
        ruling(
            "2016-04-08",
            "Some delirium abilities that appear on instants and sorceries use the word \"instead.\" These " +
                "spells have an upgraded effect when they resolve if there are four or more card types among " +
                "cards in your graveyard. They check that number only while they're resolving and don't count " +
                "themselves, since they aren't in your graveyard yet. You only get the upgraded effect, not both " +
                "effects."
        )
    }
}
