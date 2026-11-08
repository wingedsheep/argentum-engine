package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blessed Alliance — Eldritch Moon #13.
 *
 * Savage Alliance's escalate shape ({2} per extra mode). "Up to two target creatures" is one
 * mode-local requirement taking zero to two creatures, each untapped via ForEachTargetOf
 * (Quandrix Command's idiom). The edict doesn't target the creature: the targeted opponent picks
 * one of their attacking creatures on resolution (Wing Shards / Entrapment Maneuver).
 */
val BlessedAlliance = card("Blessed Alliance") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Escalate {2} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or more —\n" +
        "• Target player gains 4 life.\n" +
        "• Untap up to two target creatures.\n" +
        "• Target opponent sacrifices an attacking creature of their choice."

    spell {
        modal(chooseCount = 3, minChooseCount = 1, additionalManaCostPerExtraMode = "{2}") {
            mode("Target player gains 4 life.") {
                val player = target(Targets.Player)
                effect = Effects.GainLife(4, player)
            }
            mode("Untap up to two target creatures.") {
                val creatures = targets(TargetFilter.Creature, count = 2, optional = true)
                effect = Effects.ForEachTargetOf(creatures, Effects.Untap(EffectTarget.IterationEntity))
            }
            mode("Target opponent sacrifices an attacking creature of their choice.") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Sacrifice(GameObjectFilter.Creature.attacking(), 1, opponent)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5805eab-9a32-4c0c-9015-7bdb74ad7634.jpg?1783937526"
        ruling("2016-07-13", "The targeted opponent chooses which of their attacking creatures to sacrifice, not you.")
        ruling("2016-07-13", "Untapping an attacking creature doesn't cause it to be removed from combat.")
        ruling("2016-07-13", "If one target of an escalate spell becomes illegal, the other targets will still be affected. If all of the targets become illegal, the spell won't resolve.")
    }
}
