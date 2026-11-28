package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wolfbitten Captive // Krallenhorde Killer (Dark Ascension #133)
 *
 * The Innistrad werewolf transform pair with a once-each-turn pump on each face. Each face's ability
 * has its own id (ids are minted per face name), so activating one face's ability doesn't stop the
 * other face's that turn, while the per-permanent tracker survives transforming back.
 */
private val WolfbittenCaptiveFront = card("Wolfbitten Captive") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 1
    toughness = 1
    oracleText = "{1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

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
        collectorNumber = "133"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1303e02a-ef69-4817-bca5-02c74774b811.jpg?1783940805"
        ruling(
            "2011-01-22",
            "You can activate Wolfbitten Captive's ability, let it transform into Krallenhorde Killer, then " +
                "activate Krallenhorde Killer's ability, all on the same turn, and vice versa.",
        )
        ruling(
            "2011-01-22",
            "If you activate one face's ability, and some effect causes the card to transform twice that turn, " +
                "you won't be able to activate that face's ability again that turn.",
        )
    }
}

private val KrallenhordeKiller = card("Krallenhorde Killer") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 2
    toughness = 2
    oracleText = "{3}{G}: This creature gets +4/+4 until end of turn. Activate only once each turn.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.ModifyStats(4, 4, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
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
        collectorNumber = "133"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/back/1/3/1303e02a-ef69-4817-bca5-02c74774b811.jpg?1783940805"
    }
}

val WolfbittenCaptive: CardDefinition =
    CardDefinition.doubleFacedCreature(WolfbittenCaptiveFront, KrallenhordeKiller)
