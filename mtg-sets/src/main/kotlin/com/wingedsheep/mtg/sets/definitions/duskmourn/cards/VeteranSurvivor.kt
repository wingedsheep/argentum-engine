package com.wingedsheep.mtg.sets.definitions.duskmourn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.SourceIsTapped
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Veteran Survivor
 * {W}
 * Creature — Human Survivor
 * 2/1
 *
 * Survival — At the beginning of your second main phase, if this creature is tapped,
 * exile up to one target card from a graveyard.
 * As long as there are three or more cards exiled with this creature, it gets +3/+3
 * and has hexproof.
 */
val VeteranSurvivor = card("Veteran Survivor") {
    manaCost = "{W}"
    typeLine = "Creature — Human Survivor"
    power = 2
    toughness = 1
    oracleText = "Survival — At the beginning of your second main phase, if this creature is tapped, exile up to one target card from a graveyard.\nAs long as there are three or more cards exiled with this creature, it gets +3/+3 and has hexproof. (It can't be the target of spells or abilities your opponents control.)"

    // Survival — At the beginning of your second main phase, if this creature is tapped,
    // exile up to one target card from a graveyard (linked to this creature).
    triggeredAbility {
        trigger = Triggers.YourPostcombatMain
        triggerCondition = SourceIsTapped
        optional = true
        val card = target("card in a graveyard", Targets.CardInGraveyard)
        effect = MoveToZoneEffect(
            target = card,
            destination = Zone.EXILE,
            linkToSource = true
        )
    }

    val threeOrMoreExiled = Compare(
        DynamicAmount.CardsInLinkedExile,
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(3)
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 3, StaticTarget.SourceCreature),
            condition = threeOrMoreExiled
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, StaticTarget.SourceCreature),
            condition = threeOrMoreExiled
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39368ea2-f665-40b1-b042-c0182f7c6df0.jpg?1726286008"
    }
}
