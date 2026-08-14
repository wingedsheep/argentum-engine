package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Restless Vinestalk
 * Land
 *
 * This land enters tapped.
 * {T}: Add {G} or {U}.
 * {3}{G}{U}: Until end of turn, this land becomes a 5/5 green and blue Plant creature with trample.
 *   It's still a land.
 * Whenever this land attacks, up to one other target creature has base power and toughness 3/3 until
 *   end of turn.
 *
 * The green-blue member of the Wilds of Eldraine "Restless" creature-land cycle (see
 * [RestlessBivouac]). As with the rest of the cycle the attack trigger is an intrinsic triggered
 * ability of the land, not one granted by the animate ability.
 *
 * "Up to one **other** target creature" — the Vinestalk itself is never a legal target, so it can't
 * shrink its own 5/5 body, and the ability resolves fine with no target chosen. Setting *base* P/T is
 * a Layer 7b set-value effect, so it's applied before (and therefore overwritten by) any +N/+N in
 * Layer 7c: a 1/1 with a +1/+1 counter becomes 4/4, not 3/3.
 */
val RestlessVinestalk = card("Restless Vinestalk") {
    typeLine = "Land"
    colorIdentity = "GU"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G} or {U}.\n" +
        "{3}{G}{U}: Until end of turn, this land becomes a 5/5 green and blue Plant creature with " +
        "trample. It's still a land.\n" +
        "Whenever this land attacks, up to one other target creature has base power and toughness " +
        "3/3 until end of turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{U}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 5,
            keywords = setOf(Keyword.TRAMPLE),
            creatureTypes = setOf("Plant"),
            colors = setOf(Color.GREEN.name, Color.BLUE.name),
            duration = Duration.EndOfTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val creature = target(
            "up to one other target creature",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreature),
        )
        effect = Effects.SetBasePowerAndToughness(
            power = 3,
            toughness = 3,
            target = creature,
            duration = Duration.EndOfTurn,
        )
        description = "Whenever this land attacks, up to one other target creature has base power " +
            "and toughness 3/3 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5f3161d-3f69-4b06-ab73-c31fc0c1520c.jpg?1783915055"

        ruling(
            "2023-09-01",
            "If this becomes a creature because of an effect other than its own ability, its last " +
                "ability will still trigger whenever it attacks."
        )
        ruling(
            "2023-09-01",
            "If this becomes a creature but you haven't controlled it continuously since your most " +
                "recent turn began, you won't be able to activate its mana ability or attack with " +
                "it that turn."
        )
    }
}
