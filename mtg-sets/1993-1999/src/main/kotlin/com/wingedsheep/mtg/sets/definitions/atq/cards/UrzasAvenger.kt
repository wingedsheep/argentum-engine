package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urza's Avenger
 * {6}
 * Artifact Creature — Shapeshifter
 * 4/4
 * {0}: This creature gets -1/-1 and gains your choice of banding, flying, first strike, or trample
 * until end of turn.
 *
 * Composed from existing modal + grant primitives — no engine work. The {0} activated ability
 * resolves a [ModalEffect.chooseOne] over the four keyword options; each mode is a
 * [Effects.Composite] of the shared -1/-1 ([Effects.ModifyStats], default Duration.EndOfTurn on the
 * source) and a [Effects.GrantKeyword] for that mode's keyword (also EOT). Each activation is an
 * independent floating effect, so repeated activations stack the -X/-X and grant multiple keywords
 * for the turn (CR 613 layers). `countsAsModalSpell = false` marks this as a non-spell modal
 * ability.
 */
val UrzasAvenger = card("Urza's Avenger") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    power = 4
    toughness = 4
    oracleText = "{0}: This creature gets -1/-1 and gains your choice of banding, flying, first " +
        "strike, or trample until end of turn."

    activatedAbility {
        cost = Costs.Free
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "This creature gets -1/-1 and gains banding until end of turn",
                effect = Effects.ModifyStats(-1, -1, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.BANDING, EffectTarget.Self),
            ),
            EffectChoice(
                label = "This creature gets -1/-1 and gains flying until end of turn",
                effect = Effects.ModifyStats(-1, -1, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            ),
            EffectChoice(
                label = "This creature gets -1/-1 and gains first strike until end of turn",
                effect = Effects.ModifyStats(-1, -1, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self),
            ),
            EffectChoice(
                label = "This creature gets -1/-1 and gains trample until end of turn",
                effect = Effects.ModifyStats(-1, -1, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            )
        ))
        description = "{0}: This creature gets -1/-1 and gains your choice of banding, flying, " +
            "first strike, or trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Amy Weber"
        flavorText = "Unable to settle on just one design, Urza decided to create one versatile being."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/448e1811-fb16-4390-ac22-b7066a4a019c.jpg?1562909193"
    }
}
