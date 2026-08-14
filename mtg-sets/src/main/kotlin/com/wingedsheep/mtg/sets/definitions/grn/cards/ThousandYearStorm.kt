package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thousand-Year Storm (GRN #207)
 * {4}{U}{R}  Enchantment
 *
 * Whenever you cast an instant or sorcery spell, copy it for each other instant and sorcery
 * spell you've cast before it this turn. You may choose new targets for the copies.
 *
 * Storm's shape (CR 702.40a) narrowed to instants and sorceries, so it composes from the two
 * pieces that clause is made of rather than a bespoke effect:
 *
 *  - [Effects.CopyTargetSpell] of [EffectTarget.TriggeringEntity] already means "copy that spell,
 *    you may choose new targets for the copy" and already loops per copy, prompting for each one's
 *    targets separately (CR 707.10c).
 *  - `beforeTriggeringSpell` on [DynamicAmounts.spellsCastThisTurn] is the "cast **before it** this
 *    turn" boundary: it counts the cast history only up to the triggering spell's own record, so
 *    the triggering spell doesn't count itself and an instant cast in *response* to this trigger
 *    doesn't retroactively add a copy either.
 *
 * Known gap: if the triggering spell is countered before this ability resolves, the rulings say the
 * copies are still created — the engine can only copy a spell that is still on the stack, so no
 * copies are made in that case. That limitation is shared with every other "copy that spell" trigger
 * (Leyline of Resonance, Double Down, …) and needs last-known-information for stack objects to fix.
 */
val ThousandYearStorm = card("Thousand-Year Storm") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an instant or sorcery spell, copy it for each other instant and " +
        "sorcery spell you've cast before it this turn. You may choose new targets for the copies."

    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.InstantOrSorcery)
        effect = Effects.CopyTargetSpell(
            target = EffectTarget.TriggeringEntity,
            copies = DynamicAmounts.spellsCastThisTurn(
                filter = GameObjectFilter.InstantOrSorcery,
                beforeTriggeringSpell = true
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Dimitar Marinski"
        flavorText = "Ral's storm crackled with mystical detections: Planeswalkers were infiltrating Ravnica."
        imageUri ="https://cards.scryfall.io/normal/front/2/7/270a0863-7d07-43f0-925d-a8ce0383a1cb.jpg?1783934119"

        ruling("2018-10-05", "Spells you've cast that were countered were still cast, and so will add copies when Thousand-Year Storm's ability resolves for later spells in the turn.")
        ruling("2018-10-05", "Thousand-Year Storm's ability will copy any instant or sorcery spell, not just one with targets.")
        ruling("2018-10-05", "Copies are created even if the spell that caused Thousand-Year Storm's ability to trigger has been countered by the time that ability resolves. The copies resolve before the original spell.")
        ruling("2018-10-05", "The copies will have the same targets as the spell they're copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. The new targets must be legal.")
        ruling("2018-10-05", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copies will have the same mode or modes. You can't choose different ones.")
        ruling("2018-10-05", "If the spell that's copied has an X whose value was determined as it was cast, the copies will have the same value of X.")
        ruling("2018-10-05", "If the spell has damage divided as it was cast, the division can't be changed (although the targets receiving that damage still can). The same is true of spells that distribute counters.")
        ruling("2018-10-05", "You can't choose to pay any additional costs for the copies. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
        ruling("2018-10-05", "The copies that Thousand-Year Storm's ability creates are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell (such as that of Thousand-Year Storm itself) won't trigger.")
    }
}
