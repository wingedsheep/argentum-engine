package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Roving Keep — Throne of Eldraine #228
 * {7} · Artifact Creature — Wall · 5/7
 *
 * Defender
 * {7}: This creature gets +2/+0 and gains trample until end of turn. It can attack this turn as though
 * it didn't have defender.
 */
val RovingKeep = card("Roving Keep") {
    manaCost = "{7}"
    typeLine = "Artifact Creature — Wall"
    power = 5
    toughness = 7
    oracleText = "Defender\n" +
        "{7}: This creature gets +2/+0 and gains trample until end of turn. It can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{7}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self) then
            Effects.CanAttackDespiteDefenderThisTurn()
        description = "{7}: This creature gets +2/+0 and gains trample until end of turn. It can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Svetlin Velinov"
        flavorText = "\"It wandered slowly across the landscape, calling out in its lonely voice, but no other castles answered its cries.\"\n—Beyond the Great Henge"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38a0136e-a637-4a12-a38f-35f772b290a9.jpg?1783932583"
    }
}
