package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Najal, the Storm Runner
 * {2}{U}{U}{R}
 * Legendary Creature — Efreet Wizard
 * 5/4
 *
 * You may cast sorcery spells as though they had flash.
 * Whenever Najal attacks, you may pay {2}. If you do, when you next cast an instant or sorcery
 * spell this turn, copy it. You may choose new targets for the copy.
 *
 * The flash permission is [GrantFlashToSpellType] filtered to sorceries and scoped to the
 * controller (as Gandalf, Friend of the Shire). The attack trigger is [Effects.MayPay] gating
 * [Effects.CopyNextSpellCast] — the same one-shot "next instant or sorcery this turn" rider as
 * Galvanic Iteration: the next matching cast puts a copy trigger on the stack (the delayed
 * triggered ability), the copy may take new targets, and the rider is cleared at end of turn.
 */
val NajalTheStormRunner = card("Najal, the Storm Runner") {
    manaCost = "{2}{U}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Efreet Wizard"
    power = 5
    toughness = 4
    oracleText = "You may cast sorcery spells as though they had flash.\n" +
        "Whenever Najal attacks, you may pay {2}. If you do, when you next cast an instant or " +
        "sorcery spell this turn, copy it. You may choose new targets for the copy."

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Sorcery,
            controllerOnly = true
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(ManaCost.parse("{2}"), Effects.CopyNextSpellCast(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9017ee70-761b-434e-bc44-705cf14336fa.jpg?1783921281"
        ruling("2022-09-09", "If the spell that's copied is modal, the copy created by Najal's delayed triggered ability will have the same mode or modes. You can't choose different ones.")
        ruling("2022-09-09", "If the spell that's copied has damage divided as it was cast, the division can't be changed (although the targets receiving that damage still can). The same is true of spells that distribute counters.")
        ruling("2022-09-09", "You can't choose to pay any additional costs for the copy created by Najal's delayed triggered ability. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
        ruling("2022-09-09", "The copies that Najal's ability creates are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
    }
}
