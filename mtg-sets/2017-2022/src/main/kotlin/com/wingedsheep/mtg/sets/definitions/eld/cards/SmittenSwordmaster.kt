package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smitten Swordmaster // Curry Favor
 * {1}{B}
 * Creature — Human Knight
 * 2/1
 * Lifelink
 *
 * Adventure: Curry Favor — {B}, Sorcery — Adventure
 * You gain X life and each opponent loses X life, where X is the number of Knights you control.
 *
 * X is read on resolution from projected state (Knights you control on the battlefield); you gain X
 * once and each opponent loses X — not a drain, so the gain doesn't depend on life actually lost.
 */
val SmittenSwordmaster = card("Smitten Swordmaster") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 1
    oracleText = "Lifelink"

    keywords(Keyword.LIFELINK)

    adventure("Curry Favor") {
        manaCost = "{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "You gain X life and each opponent loses X life, where X is the number of Knights you control. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val knights = DynamicAmounts.count(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Permanent.withSubtype("Knight")
            )
            effect = Effects.GainLife(knights) then
                Effects.LoseLife(knights, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Taylor Ingvarsson"
        flavorText = "\"My quest? Why, to marry Ayara. Or die trying.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f82541f2-b17c-45b4-87ff-f9b46d23578c.jpg?1783932631"
        ruling(
            "2019-10-04",
            "In a Two-Headed Giant game, Curry Favor causes the opposing team to lose X life twice, after you gain X life once."
        )
    }
}
