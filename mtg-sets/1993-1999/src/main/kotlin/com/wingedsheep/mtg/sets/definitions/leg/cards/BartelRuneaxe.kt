package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeTargetedBy
import com.wingedsheep.sdk.scripting.UntargetableBy

/**
 * Bartel Runeaxe — Legends #222
 * {3}{B}{R}{G} · Legendary Creature — Giant Warrior · 6/5
 *
 * Vigilance
 * Bartel Runeaxe can't be the target of Aura spells.
 *
 * Same shape as Tetsuo Umezawa: [CantBeTargetedBy] with [UntargetableBy.SpellsWithSubtype] — only
 * Aura *spells* are barred.
 */
val BartelRuneaxe = card("Bartel Runeaxe") {
    manaCost = "{3}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Giant Warrior"
    power = 6
    toughness = 5
    oracleText = "Vigilance\nBartel Runeaxe can't be the target of Aura spells."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = CantBeTargetedBy(UntargetableBy.SpellsWithSubtype(Subtype.AURA))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Andi Rusu"
        flavorText = "Thundering down from Hammerheim, no foe could slow Bartel's charge."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1a42691-98bb-4234-9b56-085e6677f3e4.jpg?1783948040"
    }
}
