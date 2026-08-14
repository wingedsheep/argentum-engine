package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restless Bivouac
 * Land
 *
 * This land enters tapped.
 * {T}: Add {R} or {W}.
 * {1}{R}{W}: This land becomes a 2/2 red and white Ox creature until end of turn. It's still a land.
 * Whenever this land attacks, put a +1/+1 counter on target creature you control.
 *
 * The Wilds of Eldraine half of the "Restless" creature-land cycle (the Lost Caverns of Ixalan half
 * is [com.wingedsheep.mtg.sets.definitions.lci.cards.RestlessAnchorage] and friends). As with the
 * rest of the cycle the attack trigger is an *intrinsic* triggered ability of the land, not one
 * granted by the animate ability: a land can only attack while it's a creature, so in practice the
 * trigger is live only after {1}{R}{W} resolves, but per the second Scryfall ruling it also fires if
 * something else animates the land.
 *
 * The +1/+1 counter may go on any creature you control — including the Bivouac itself, which is a
 * creature at that point. Counters are not part of the until-end-of-turn animation, so they simply
 * sit on the land once it stops being a creature and count again the next time it animates.
 */
val RestlessBivouac = card("Restless Bivouac") {
    typeLine = "Land"
    colorIdentity = "RW"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {R} or {W}.\n" +
        "{1}{R}{W}: This land becomes a 2/2 red and white Ox creature until end of turn. It's still a land.\n" +
        "Whenever this land attacks, put a +1/+1 counter on target creature you control."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}{W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Ox"),
            colors = setOf(Color.RED.name, Color.WHITE.name),
            duration = Duration.EndOfTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Whenever this land attacks, put a +1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b85e0aed-bfb2-4aa8-a754-849c4d9a6a58.jpg?1783915056"

        ruling(
            "2023-09-01",
            "Counters on Restless Bivouac remain on it when it stops being a creature. If it becomes " +
                "a creature later, they'll apply to it."
        )
        ruling(
            "2023-09-01",
            "If this becomes a creature because of an effect other than its own ability, its last " +
                "ability will still trigger whenever it attacks."
        )
    }
}
