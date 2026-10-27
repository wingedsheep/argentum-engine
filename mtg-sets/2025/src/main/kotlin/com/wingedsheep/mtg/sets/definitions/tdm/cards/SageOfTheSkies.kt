package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sage of the Skies — Tarkir: Dragonstorm #22
 * {2}{W} · Creature — Human Monk · 2/3
 *
 * When you cast this spell, if you've cast another spell this turn, copy this spell.
 * (The copy becomes a token.)
 * Flying, lifelink
 *
 * The cast trigger fires from the stack via `Triggers.self.isCast()`. The intervening
 * "if" (CR 603.4) is `Conditions.YouCastSpellsThisTurn(atLeast = 2)`: the spell itself is already
 * counted when its own cast trigger is checked, so "two or more" means "you've cast another spell
 * this turn". `Effects.CopyTargetSpell(TriggeringEntity)` copies the triggering spell (the same
 * primitive Taigam, Master Opportunist uses) — since Sage is a permanent spell the copy resolves
 * into a token (CR 707.10f), and because a copy is put on the stack rather than cast (CR 707.10),
 * it does not re-trigger this ability.
 */
val SageOfTheSkies = card("Sage of the Skies") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 3
    oracleText = "When you cast this spell, if you've cast another spell this turn, copy this " +
        "spell. (The copy becomes a token.)\n" +
        "Flying, lifelink"

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.YouCastSpellsThisTurn(atLeast = 2)
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
        description = "When you cast this spell, if you've cast another spell this turn, copy this spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Justyna Dura"
        flavorText = "\"Gravity is just a suggestion.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ade6918-6d1d-448d-ab56-93996051e9a9.jpg?1743204040"
    }
}
