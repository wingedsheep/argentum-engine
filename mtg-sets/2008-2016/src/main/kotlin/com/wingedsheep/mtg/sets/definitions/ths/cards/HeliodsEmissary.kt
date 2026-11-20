package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heliod's Emissary (Theros #18)
 *
 * "Whenever this creature or enchanted creature attacks" is two triggers (Spiteful Returned shape):
 * the subjects bind differently (SELF vs ATTACHED), and the permanent is only ever one of a creature
 * or a bestowed Aura at a time, so exactly one fires per attack.
 */
val HeliodsEmissary = card("Heliod's Emissary") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Elk"
    power = 3
    toughness = 3
    oracleText = "Bestow {6}{W} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Whenever this creature or enchanted creature attacks, tap target creature an opponent controls.\n" +
        "Enchanted creature gets +3/+3."

    keywordAbility(KeywordAbility.bestow("{6}{W}"))

    // "Whenever this creature ... attacks"
    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature)
    }

    // "... or enchanted creature attacks"
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature)
    }

    staticAbility { ability = ModifyStats(3, 3) }

    metadata {
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49663acb-582e-495a-b45e-5fc5f4949f22.jpg?1783939814"
    }
}
