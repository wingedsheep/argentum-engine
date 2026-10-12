package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Argivian Avenger — The Brothers' War #232
 * {6} · Artifact Creature — Shapeshifter · 5/5
 * {1}: Until end of turn, this creature gets -1/-1 and gains your choice of flying, vigilance,
 * deathtouch, or haste.
 *
 * "Your choice of" is not a mode: it's picked on resolution (Steel Seraph's shape), so the
 * ability is the -1/-1 followed by a [Effects.ChooseAction] over the four keyword grants, all
 * until end of turn on the source.
 */
val ArgivianAvenger = card("Argivian Avenger") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Shapeshifter"
    power = 5
    toughness = 5
    oracleText = "{1}: Until end of turn, this creature gets -1/-1 and gains your choice of " +
        "flying, vigilance, deathtouch, or haste."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(-1, -1, EffectTarget.Self) then Effects.ChooseAction(
            listOf(
                EffectChoice(label = "Flying", effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)),
                EffectChoice(label = "Vigilance", effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)),
                EffectChoice(label = "Deathtouch", effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)),
                EffectChoice(label = "Haste", effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self))
            )
        )
        description = "{1}: Until end of turn, this creature gets -1/-1 and gains your choice of " +
            "flying, vigilance, deathtouch, or haste."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Milivoj Ćeran"
        flavorText = "Unable to settle on just one versatile being, Urza decided to create myriad adaptable machines."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28e3bf5a-0a90-46d3-bcfe-9a47df41b72b.jpg?1783920022"
    }
}
