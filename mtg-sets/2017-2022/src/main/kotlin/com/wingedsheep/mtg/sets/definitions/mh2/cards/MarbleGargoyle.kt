package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marble Gargoyle — Modern Horizons 2 #21
 * {2}{W} · Artifact Creature — Gargoyle · 2 / 2
 *
 * Flying
 * {W}: This creature gets +0/+1 until end of turn.
 *
 * The classic defensive gargoyle: a flier that can be pumped out of burn and combat range as long
 * as the white mana holds out.
 *
 * The pump ability names the source, not a target — "this creature", not "target creature" — so
 * the effect uses [EffectTarget.Self] and the ability declares no target requirement at all. It is
 * therefore uncounterable-by-removal in the usual sense: killing the Gargoyle in response leaves
 * the ability on the stack doing nothing, but it never fizzles for lack of a legal target.
 *
 * Each activation is a separate one-shot [Effects.ModifyStats] with the default end-of-turn
 * duration, so the bonuses stack — activating four times makes it a 2/6 for the turn.
 */
val MarbleGargoyle = card("Marble Gargoyle") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Gargoyle"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "{W}: This creature gets +0/+1 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ModifyStats(0, 1, EffectTarget.Self)
        description = "{W}: This creature gets +0/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Drew Tucker"
        flavorText = "\"Once past the stony exterior, the meat is exquisite and can be stuffed with thallids or simmered in a broth of manticore venom.\"\n—Asmoranomardicadaistinaculdacar,\n*The Underworld Cookbook*"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c62efb9-11f2-4f82-af08-4587d58d6e3d.jpg?1783926889"
    }
}
