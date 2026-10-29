package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seasoned Hallowblade — Core Set 2021 #34
 * {1}{W} · Creature — Human Warrior · 3/1
 *
 * Discard a card: Tap this creature. It gains indestructible until end of turn.
 *
 * No {T} in the cost, so the ability can be activated while it's already tapped (ruling) — the
 * tap is part of the effect, not the cost.
 */
val SeasonedHallowblade = card("Seasoned Hallowblade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 1
    oracleText = "Discard a card: Tap this creature. It gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.Tap(EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        description = "Discard a card: Tap this creature. It gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Manuel Castañón"
        flavorText = "\"I don't know that I'll win every fight. But I have faith, and that's carried me this far.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feb59931-8941-46fb-8e1c-f80ff3730dd9.jpg?1783930735"
        ruling(
            "2020-06-23",
            "You can activate Seasoned Hallowblade's ability even if nothing's about to destroy it, even if it's " +
                "already tapped, and even if it already has indestructible."
        )
    }
}
