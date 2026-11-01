package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Beamsplitter Mage (GRN #155)
 * {U}{R}
 * Creature — Vedalken Wizard
 * 2/2
 *
 * Whenever you cast an instant or sorcery spell that targets only this creature, if you control one
 * or more other creatures that spell could target, choose one of those creatures. Copy that spell.
 * The copy targets the chosen creature.
 *
 * The candidate set is the CR 707.10d one (a creature you control that is a legal target for every
 * instance of "target" on the spell, other than the ones it already targets). The intervening "if"
 * (CR 603.4) is checked as the spell is cast by `SpellCastPredicate.CouldTargetOther`, and again on
 * resolution by the copy effect, which copies nothing when no such creature is left.
 */
val BeamsplitterMage = card("Beamsplitter Mage") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Vedalken Wizard"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast an instant or sorcery spell that targets only this creature, if you " +
        "control one or more other creatures that spell could target, choose one of those creatures. " +
        "Copy that spell. The copy targets the chosen creature."

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(
                SpellCastPredicate.TargetsOnlySource,
                SpellCastPredicate.CouldTargetOther(GameObjectFilter.Creature.youControl())
            )
        )
        effect = Effects.CopyForChosenOtherPossibleTarget(candidates = GameObjectFilter.Creature.youControl())
        description = "Whenever you cast an instant or sorcery spell that targets only this creature, if " +
            "you control one or more other creatures that spell could target, choose one of those " +
            "creatures. Copy that spell. The copy targets the chosen creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Svetlin Velinov"
        flavorText = "The Izzet love replicating results."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f1801f2-ea6e-4196-858e-2afc456cf6a0.jpg?1783934141"

        ruling("2018-10-05", "The ability triggers whenever you cast an instant or sorcery spell that targets only Beamsplitter Mage and no other object or player, and only if you control another creature that spell could target. If you cast an instant or sorcery spell that has multiple targets and Beamsplitter Mage is chosen as the target in each instance, Beamsplitter Mage's ability will trigger.")
        ruling("2018-10-05", "If you don't control another creature the spell could target as Beamsplitter Mage's triggered ability resolves, you don't copy that spell.")
        ruling("2018-10-05", "If Beamsplitter Mage leaves the battlefield or becomes an illegal target for the spell that caused its ability to trigger, that spell is still copied.")
        ruling("2018-10-05", "If you copy a spell, you control the copy. It will resolve before the original spell does.")
        ruling("2018-10-05", "The copy is created on the stack, so it's not cast. Abilities that trigger when a player casts a spell (like Beamsplitter Mage's ability itself) won't trigger.")
        ruling("2018-10-05", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. A different mode cannot be chosen.")
        ruling("2018-10-05", "If the spell that's copied has an X whose value was determined as it was cast (like Selective Snare does), the copy has the same value of X.")
    }
}
