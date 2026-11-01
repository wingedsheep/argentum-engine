package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodmist Infiltrator — Ravnica Allegiance #65
 * {2}{B} · Creature — Vampire · 3 / 1
 *
 * Same shape as Vampire Gourmand: an [Effects.MayPay] wrapping the optional sacrifice of
 * another creature, so "If you do" only grants CANT_BE_BLOCKED (until end of turn) when
 * a creature was actually sacrificed.
 */
val BloodmistInfiltrator = card("Bloodmist Infiltrator") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
        "this creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
            "this creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Mitchell Malloy"
        flavorText = "Blood seeks blood, and nothing shall hold back its hunger."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/909489a9-2678-4b6f-9d5e-2c04bb4cbd66.jpg?1783933698"
    }
}
