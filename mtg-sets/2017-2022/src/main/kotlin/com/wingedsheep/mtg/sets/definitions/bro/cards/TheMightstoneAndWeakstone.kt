package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Mightstone and Weakstone
 * {5}
 * Legendary Artifact — Powerstone
 * When The Mightstone and Weakstone enters, choose one —
 * • Draw two cards.
 * • Target creature gets -5/-5 until end of turn.
 * {T}: Add {C}{C}. This mana can't be spent to cast nonartifact spells.
 * (Melds with Urza, Lord Protector.)
 *
 * The meld itself is Urza, Lord Protector's activated ability; this half carries only the
 * reminder text.
 */
val TheMightstoneAndWeakstone = card("The Mightstone and Weakstone") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Powerstone"
    oracleText = "When The Mightstone and Weakstone enters, choose one —\n" +
        "• Draw two cards.\n" +
        "• Target creature gets -5/-5 until end of turn.\n" +
        "{T}: Add {C}{C}. This mana can't be spent to cast nonartifact spells.\n" +
        "(Melds with Urza, Lord Protector.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Draw two cards.") {
                    effect = Effects.DrawCards(2)
                },
                mode("Target creature gets -5/-5 until end of turn.") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.ModifyStats(-5, -5, creature)
                }
            )
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(
            2,
            restriction = ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT)),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}{C}. This mana can't be spent to cast nonartifact spells."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02aea379-b444-46a3-82f4-3038f698d4f4.jpg?1783920017"
    }
}
