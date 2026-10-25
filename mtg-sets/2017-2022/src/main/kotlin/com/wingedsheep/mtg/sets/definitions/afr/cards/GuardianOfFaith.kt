package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian of Faith
 * {1}{W}{W}
 * Creature — Spirit Knight
 * 3/2
 * Flash
 * Vigilance
 * When this creature enters, any number of other target creatures you control phase out.
 * (Treat them and anything attached to them as though they don't exist until their controller's
 * next turn.)
 *
 * The targets are chosen as the trigger goes on the stack (zero or more, never the Guardian
 * itself); on resolution each still-legal target phases out, carrying its Auras and Equipment.
 */
val GuardianOfFaith = card("Guardian of Faith") {
    manaCost = "{1}{W}{W}"
    typeLine = "Creature — Spirit Knight"
    power = 3
    toughness = 2
    oracleText = "Flash\nVigilance\n" +
        "When this creature enters, any number of other target creatures you control phase out. " +
        "(Treat them and anything attached to them as though they don't exist until their " +
        "controller's next turn.)"

    keywords(Keyword.FLASH, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreatureYouControl, unlimited = true)
        effect = Effects.ForEachTarget(Effects.PhaseOut(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08e3dda1-a1d3-48c9-8c81-da7eae20ac8a.jpg?1783926532"
        ruling("2021-07-23", "An attacking or blocking creature that phases out is removed from combat.")
        ruling(
            "2021-07-23",
            "Permanents phase back in during their controller's untap step, immediately before that " +
                "player untaps their permanents. Creatures that phase in this way are able to attack " +
                "and pay a cost of {T} during that turn."
        )
        ruling(
            "2021-07-23",
            "Phasing out doesn't cause any \"leaves the battlefield\" abilities to trigger. Similarly, " +
                "phasing in won't cause any \"enters the battlefield\" abilities to trigger."
        )
    }
}
