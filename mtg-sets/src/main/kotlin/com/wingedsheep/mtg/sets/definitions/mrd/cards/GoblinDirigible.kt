package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Dirigible — Mirrodin #177 (canonical printing, only printing)
 * {6} · Artifact Creature — Construct · 4/4
 *
 * Flying
 * This creature doesn't untap during your untap step.
 * At the beginning of your upkeep, you may pay {4}. If you do, untap this creature.
 *
 * The Brass Man / Colossus of Sardia shape, with a bigger toll: [AbilityFlag.DOESNT_UNTAP] takes
 * it out of the untap step, and a [Triggers.YourUpkeep] trigger offers the buy-back via
 * [MayPayManaEffect] — a mandatory trigger with an optional payment, so declining is a legal
 * choice each upkeep and the Dirigible simply stays tapped. The untap is [EffectTarget.Self],
 * so the trigger does nothing if the creature has already left the battlefield.
 */
val GoblinDirigible = card("Goblin Dirigible") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "This creature doesn't untap during your untap step.\n" +
        "At the beginning of your upkeep, you may pay {4}. If you do, untap this creature."

    keywords(Keyword.FLYING)
    flags(AbilityFlag.DOESNT_UNTAP)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = MayPayManaEffect(ManaCost.parse("{4}"), Effects.Untap(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2ed2990-e5bf-4567-9a41-1846108a8aeb.jpg?1783944520"
    }
}
