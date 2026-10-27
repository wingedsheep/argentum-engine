package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cinderheart Giant
 * {5}{R}{R}
 * Creature — Giant Berserker
 * 7/6
 *
 * Trample
 * When this creature dies, it deals 7 damage to a creature an opponent controls chosen at random.
 *
 * The creature isn't targeted (ruling): the pick happens as the trigger resolves — every creature
 * opponents control is gathered and the engine picks one at random, so hexproof doesn't stop it
 * (protection from red still prevents the damage). With no such creature the trigger does nothing.
 */
val CinderheartGiant = card("Cinderheart Giant") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Berserker"
    power = 7
    toughness = 6
    oracleText = "Trample\nWhen this creature dies, it deals 7 damage to a creature an opponent controls " +
        "chosen at random."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature.opponentControls()))
            val victim = chooseRandom(1, from = creatures)
            run(Effects.ForEachInCollection(victim, Effects.DealDamage(7, EffectTarget.IterationEntity)))
        }
        description = "When this creature dies, it deals 7 damage to a creature an opponent controls " +
            "chosen at random."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Viktor Titov"
        flavorText = "What fire giants lack in arcane prowess, they make up for with reckless, explosive rage."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4802e6c-6921-4dac-bf44-7e09d6942b71.jpg?1783928234"
        ruling(
            "2021-02-05",
            "The creature an opponent controls isn't targeted, so it's chosen as the triggered ability " +
                "resolves. A creature with hexproof could be chosen, as could a creature with protection " +
                "from red, although, in the latter case, the damage would be prevented."
        )
    }
}
