package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mysterious Limousine
 * {3}{W}{W}
 * Artifact — Vehicle
 * 4/4
 *
 * Whenever this Vehicle enters or attacks, exile up to one other target creature until this Vehicle
 * leaves the battlefield. If a creature is put into exile this way, return each other card exiled
 * with this Vehicle to the battlefield under its owner's control.
 * Crew 2
 *
 * "Enters **or** attacks" is two trigger conditions on one printed line, authored as two triggered
 * abilities sharing [limousineSwap] (the Skysovereign, Consul Flagship shape).
 *
 * The swap is one pipeline over the Vehicle's linked-exile pile:
 *  1. snapshot the pile *before* exiling — those are the "other" cards;
 *  2. [Effects.ExileUntilLeaves] the target (a no-op when no target was chosen, the target became
 *     illegal, or the Vehicle already left the battlefield — the "until" template);
 *  3. re-read the pile and subtract the snapshot: what's left is the creature exiled *this way*;
 *  4. only if that is non-empty, return the snapshot to the battlefield under its owners' control.
 * So choosing no target (or a fizzled one) leaves the previously exiled cards where they are.
 *
 * The leaves-the-battlefield half of "until this Vehicle leaves" is the usual linked return.
 */
private fun limousineSwap(victim: EffectTarget): Effect = Effects.Pipeline {
    val previouslyExiled = gather(CardSource.FromLinkedExile())
    run(Effects.ExileUntilLeaves(victim))
    val nowExiled = gather(CardSource.FromLinkedExile())
    val exiledThisWay = exclude(nowExiled, previouslyExiled)
    ifNotEmpty(exiledThisWay) {
        move(
            previouslyExiled,
            CardDestination.ToZone(Zone.BATTLEFIELD),
            underOwnersControl = true
        )
    }
}

val MysteriousLimousine = card("Mysterious Limousine") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    oracleText = "Whenever this Vehicle enters or attacks, exile up to one other target creature until this Vehicle leaves the battlefield. If a creature is put into exile this way, return each other card exiled with this Vehicle to the battlefield under its owner's control.\n" +
        "Crew 2"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter.OtherCreature, optional = true)
        effect = limousineSwap(victim)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val victim = target(TargetFilter.OtherCreature, optional = true)
        effect = limousineSwap(victim)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a58cb742-0ba7-4795-9772-25d4db0bc4ce.jpg?1783923155"
    }
}
