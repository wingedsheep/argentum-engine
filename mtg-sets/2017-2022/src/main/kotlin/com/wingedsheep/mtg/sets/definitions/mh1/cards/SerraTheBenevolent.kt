package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LifeLossFloor
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serra the Benevolent — Modern Horizons #26 (canonical printing)
 * {2}{W}{W} · Legendary Planeswalker — Serra · Loyalty 4
 *
 * +2: Creatures you control with flying get +1/+1 until end of turn.
 * −3: Create a 4/4 white Angel creature token with flying and vigilance.
 * −6: You get an emblem with "If you control a creature, damage that would reduce your life total
 *     to less than 1 reduces it to 1 instead."
 *
 * The emblem holds Worship's [LifeLossFloor] as a replacement effect of its own
 * (`CreatePermanentEmblem(ownedReplacementEffects = …)`): it caps only the life lost to damage, so
 * lifelink and damage-dealt triggers still see the full damage and "you lose life" effects are
 * untouched (rulings).
 */
val SerraTheBenevolent = card("Serra the Benevolent") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Serra"
    startingLoyalty = 4
    oracleText = "+2: Creatures you control with flying get +1/+1 until end of turn.\n" +
        "−3: Create a 4/4 white Angel creature token with flying and vigilance.\n" +
        "−6: You get an emblem with \"$SERRA_EMBLEM_TEXT\""

    loyaltyAbility(+2) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
    }

    loyaltyAbility(-3) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/9/1/91808554-23ab-44f9-92a1-87ee7d58cd9a.jpg?1783933235",
        )
    }

    loyaltyAbility(-6) {
        effect = Effects.CreatePermanentEmblem(
            ownedReplacementEffects = listOf(
                LifeLossFloor(
                    floor = 1,
                    restrictions = listOf(Conditions.YouControl(GameObjectFilter.Creature)),
                    appliesTo = EventPattern.LifeLossEvent(player = Player.You),
                )
            ),
            emblemDescription = SERRA_EMBLEM_TEXT,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "26"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88bf4af9-4b14-43e7-9d50-0e6a895cece1.jpg?1783933156"

        ruling("2019-06-14", "Serra's first ability affects only creatures you control with flying at the time it resolves. Creatures you begin to control later in the turn or that gain flying later in the turn won't get +1/+1.")
        ruling("2019-06-14", "Serra's emblem doesn't prevent damage. It only changes the result of damage dealt to you. For example, a creature with lifelink that deals damage to you will still cause its controller to gain life, even if that damage would reduce your life total to less than 1.")
        ruling("2019-06-14", "Serra's emblem doesn't stop loss of life from effects that say that you lose life.")
        ruling("2019-06-14", "If you have less than 1 life and somehow haven't lost the game, damage dealt to you reduces your life total further below 0 (as normal).")
        ruling("2019-06-14", "In a Two-Headed Giant game, damage dealt to you and/or your teammate that would reduce your team's life total to less than 1 reduces it to 1 instead, as long as you control a creature. It doesn't matter whether your teammate controls a creature, even if they're the player being dealt damage.")
        ruling("2019-06-14", "In the Commander variant, combat damage you're dealt by a commander is still tracked, even if it doesn't change your life total.")
    }
}

private const val SERRA_EMBLEM_TEXT =
    "If you control a creature, damage that would reduce your life total to less than 1 reduces it to 1 instead."
