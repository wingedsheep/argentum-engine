package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unrooted Ancestor — Tarkir: Dragonstorm #96
 * {2}{B} · Creature — Spirit Cleric · 3/2
 *
 * Flash
 * {1}, Sacrifice another creature: This creature gains indestructible until end of turn. Tap it.
 *
 * The activated ability grants indestructible to itself until end of turn, then taps it. The
 * sacrifice cost excludes the source ([Costs.SacrificeAnother]).
 */
val UnrootedAncestor = card("Unrooted Ancestor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit Cleric"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "{1}, Sacrifice another creature: This creature gains indestructible until end of turn. Tap it."

    keywords(Keyword.FLASH)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.Composite(
            listOf(
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
                Effects.Tap(EffectTarget.Self)
            )
        )
        description = "This creature gains indestructible until end of turn. Tap it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6394b125-21a8-4439-9958-94b76684138e.jpg?1743204347"
    }
}
