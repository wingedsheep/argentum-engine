package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Immersturm Skullcairn
 * Land
 * This land enters tapped.
 * {T}: Add {B}.
 * {1}{B}{R}{R}, {T}, Sacrifice this land: It deals 3 damage to target player. That player discards
 * a card. Activate only as a sorcery.
 *
 * The Rakdos realm land. The land is sacrificed as a cost, so "It deals 3 damage" uses the source's
 * last-known information; the discard falls on the same targeted player.
 */
val ImmersturmSkullcairn = card("Immersturm Skullcairn") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B}.\n" +
        "{1}{B}{R}{R}, {T}, Sacrifice this land: It deals 3 damage to target player. That player discards a card. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    // {T}: Add {B}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK, 1)
        manaAbility = true
    }

    // {1}{B}{R}{R}, {T}, Sacrifice this land: It deals 3 damage to target player. That player
    // discards a card. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}{R}{R}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val player = target(Targets.Player)
        effect = Effects.DealDamage(3, player) then Effects.Discard(1, player)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "263"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12ed97de-736d-43d8-977b-308ac54f88f4.jpg?1783928173"
    }
}
