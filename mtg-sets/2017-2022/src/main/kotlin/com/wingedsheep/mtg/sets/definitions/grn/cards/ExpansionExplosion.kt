package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Expansion // Explosion (GRN 224) — split-layout instant (CR 709).
 *
 * Expansion {U/R}{U/R} — Instant
 *   Copy target instant or sorcery spell with mana value 4 or less. You may choose new targets for
 *   the copy.
 *
 * Explosion {X}{U}{U}{R}{R} — Instant
 *   Explosion deals X damage to any target. Target player draws X cards.
 *
 * Current Oracle text reads "mana value 4 or less" (the printed card said 2). A spell's mana value on
 * the stack includes its chosen X (2024-01-12 ruling). Explosion's two targets are independent: if one
 * becomes illegal, the other is still affected.
 */
val ExpansionExplosion = card("Expansion // Explosion") {
    layout = CardLayout.SPLIT
    colorIdentity = "UR"

    face("Expansion") {
        manaCost = "{U/R}{U/R}"
        typeLine = "Instant"
        oracleText = "Copy target instant or sorcery spell with mana value 4 or less. You may choose new " +
            "targets for the copy."

        spell {
            val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.manaValueAtMost(4))
            effect = Effects.CopyTargetSpell(spell)
        }
    }

    face("Explosion") {
        manaCost = "{X}{U}{U}{R}{R}"
        typeLine = "Instant"
        oracleText = "Explosion deals X damage to any target. Target player draws X cards."

        spell {
            val damaged = target(Targets.Any)
            val drawer = target(Targets.Player)
            effect = Effects.DealDamage(DynamicAmounts.xValue(), damaged) then
                Effects.DrawCards(DynamicAmounts.xValue(), drawer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0644c92-4d67-475e-8c8e-0e2c493682fb.jpg?1783934112"
        ruling("2024-01-12", "If one of Explosion's two targets becomes illegal, the other is affected as appropriate.")
        ruling("2024-01-12", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2024-01-12", "If the spell that's copied has an X whose value was determined as it was cast (like Banefire does), the copy will have the same value of X.")
        ruling("2024-01-12", "Expansion can copy any instant or sorcery spell with an appropriate mana value, not just one with targets.")
        ruling("2024-01-12", "If a spell has {X} in its mana cost, include the value chosen for that X when determining the mana value of that spell.")
        ruling("2024-01-12", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2022-12-08", "If you copy a spell that's half of a split card, the copy copies that same half. For example, if you copy Assault, the copy is also Assault, not Battery.")
    }
}
