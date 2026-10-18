package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Falling Star
 * {2}{R}
 * Sorcery
 * Flip Falling Star onto the playing area from a height of at least one foot. Falling Star deals
 * 3 damage to each creature it lands on. Tap all creatures dealt damage by Falling Star. If Falling
 * Star doesn't turn completely over at least once during the flip, it has no effect.
 *
 * The flip is Chaos Orb's dexterity mini-game
 * ([com.wingedsheep.sdk.scripting.effects.FlipOntoBattlefieldEffect]), thrown from the stack. Its
 * touched collection is empty unless the card turned over completely, which is the "no effect"
 * clause. Only creatures it rests on count (ruling), tokens included. The tap reads the spell's own
 * damage record, so a creature whose damage was prevented isn't tapped.
 */
val FallingStar = card("Falling Star") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Flip Falling Star onto the playing area from a height of at least one foot. Falling Star deals " +
        "3 damage to each creature it lands on. Tap all creatures dealt damage by Falling Star. If Falling Star " +
        "doesn't turn completely over at least once during the flip, it has no effect."

    spell {
        effect = Effects.Pipeline {
            val landedOn = flipOntoBattlefield(GameObjectFilter.Creature)
            run(Effects.ForEachInCollection(landedOn, Effects.DealDamage(3, EffectTarget.IterationEntity)))
        } then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.wasDealtDamageBySourceThisTurn()),
            Effects.Tap(EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2b9983e-20d4-4d12-9e2c-ec6d9a345787.jpg?1783948056"
        ruling("2004-10-04", "It must flip like a coin and not like a Frisbee.")
        ruling("2004-10-04", "Only cards touched when it stops moving are affected. Not ones touched while it is moving.")
    }
}
