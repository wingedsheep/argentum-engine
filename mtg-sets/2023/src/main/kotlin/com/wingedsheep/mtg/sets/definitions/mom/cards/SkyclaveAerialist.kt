package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skyclave Aerialist // Skyclave Invader — March of the Machine #78.
 * {1}{U} · Creature — Merfolk Scout 2/1 // Creature — Phyrexian Merfolk Scout 2/4
 *
 * Front: Flying. {4}{G/P}: Transform this creature. Activate only as a sorcery.
 * Back: Flying. When this creature transforms into Skyclave Invader, look at the top card of your
 * library. If it's a land card, you may put it onto the battlefield. If you don't put the card onto
 * the battlefield, put it into your hand.
 */
private val SkyclaveAerialistFront = card("Skyclave Aerialist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Scout"
    power = 2
    toughness = 1
    oracleText = "Flying\n{4}{G/P}: Transform this creature. Activate only as a sorcery. " +
        "({G/P} can be paid with either {G} or 2 life.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{4}{G/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Michal Ivan"
        flavorText = "Together with her beloved falcon, she had explored every waterway on Zendikar, " +
            "from the Tazeem highlands to the inland sea."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd1c8e1a-bd38-47a2-a55a-682ebaede960.jpg?1783917029"
    }
}

private val SkyclaveInvader = card("Skyclave Invader") {
    manaCost = ""
    colorIdentity = "UG"
    colorIndicator = "U"
    typeLine = "Creature — Phyrexian Merfolk Scout"
    power = 2
    toughness = 4
    oracleText = "Flying\nWhen this creature transforms into Skyclave Invader, look at the top card of " +
        "your library. If it's a land card, you may put it onto the battlefield. If you don't put " +
        "the card onto the battlefield, put it into your hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (landCards, nonLandCards) = filterSplit(looked, GameObjectFilter.Land)
            val (toBattlefield, landsToHand) = chooseUpToSplit(
                1,
                from = landCards,
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put into your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            toHand(landsToHand)
            toHand(nonLandCards)
        }
        description = "When this creature transforms into Skyclave Invader, look at the top card of " +
            "your library. If it's a land card, you may put it onto the battlefield. If you don't " +
            "put the card onto the battlefield, put it into your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Michal Ivan"
        flavorText = "With her guidance, the oil coursed freely across the world."
        imageUri = "https://cards.scryfall.io/normal/back/c/d/cd1c8e1a-bd38-47a2-a55a-682ebaede960.jpg?1783917029"
    }
}

val SkyclaveAerialist: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SkyclaveAerialistFront,
    backFace = SkyclaveInvader,
)
