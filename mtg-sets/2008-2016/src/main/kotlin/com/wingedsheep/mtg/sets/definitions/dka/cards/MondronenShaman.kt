package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
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
 * Mondronen Shaman // Tovolar's Magehunter (Dark Ascension #98)
 *
 * The Innistrad werewolf transform pair (Hinterland Hermit's shape); the back's punisher is a
 * `Triggers.anOpponent.casts()` trigger dealing 2 to the caster ([Player.TriggeringPlayer]).
 */
private val MondronenShamanFront = card("Mondronen Shaman") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman Werewolf"
    power = 3
    toughness = 2
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Mike Sass"
        flavorText = "\"Tovolar, my master. Gather the howlpack, for the Hunter's Moon is nigh and Thraben's walls grow weak.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b150d71f-11c9-40d6-a461-4967ef437315.jpg?1783940821"
    }
}

private val TovolarsMagehunter = card("Tovolar's Magehunter") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 5
    oracleText = "Whenever an opponent casts a spell, this creature deals 2 damage to that player.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Mike Sass"
        imageUri = "https://cards.scryfall.io/normal/back/b/1/b150d71f-11c9-40d6-a461-4967ef437315.jpg?1783940821"
        ruling(
            "2011-01-22",
            "Tovolar's Magehunter's first triggered ability will resolve before the spell that caused it to " +
                "trigger. If this causes the player who cast that spell to lose the game, that spell won't resolve.",
        )
    }
}

val MondronenShaman: CardDefinition =
    CardDefinition.doubleFacedCreature(MondronenShamanFront, TovolarsMagehunter)
