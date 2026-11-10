package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sage of Ancient Lore // Werewolf of Ancient Hunger — both faces carry a characteristic-defining
 * P/T (CR 604.3): the front counts its controller's hand, the back every player's hand combined.
 */
private val SageOfAncientLoreFront = card("Sage of Ancient Lore") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman Werewolf"
    oracleText = "Sage of Ancient Lore's power and toughness are each equal to the number of cards in your hand.\n" +
        "When this creature enters, draw a card.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."
    dynamicStats(DynamicAmounts.cardsInYourHand())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, 0
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94c5f7fe-9ca6-4040-bf7c-062ab22e6a02.jpg?1783937728"
        ruling(
            "2016-04-08",
            "If you have no cards in hand after Sage of Ancient Lore enters the battlefield, it will be put " +
                "into your graveyard before its first triggered ability resolves. You'll draw a card, but " +
                "Sage of Ancient Lore will already be gone."
        )
        ruling(
            "2016-04-08",
            "The abilities that transform a Werewolf back and forth look at the entire previous turn, even if " +
                "the Werewolf with that ability wasn't on the battlefield for some or all of that turn."
        )
    }
}

private val WerewolfOfAncientHunger = card("Werewolf of Ancient Hunger") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    oracleText = "Vigilance, trample\n" +
        "Werewolf of Ancient Hunger's power and toughness are each equal to the total number of cards in all players' hands.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)
    dynamicStats(DynamicAmounts.count(Player.Each, Zone.HAND))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/back/9/4/94c5f7fe-9ca6-4040-bf7c-062ab22e6a02.jpg?1783937728"
        ruling(
            "2016-04-08",
            "To trigger the Werewolf's back face's transform ability, a single player must have cast two or " +
                "more spells during the previous turn. If multiple players each cast just one spell during the " +
                "previous turn, the ability won't trigger."
        )
    }
}

val SageOfAncientLore: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SageOfAncientLoreFront,
    backFace = WerewolfOfAncientHunger,
)
