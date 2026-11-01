package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legion Guildmage
 * {R}{W}
 * Creature — Human Wizard
 * 2/2
 * {5}{R}, {T}: This creature deals 3 damage to each opponent.
 * {2}{W}, {T}: Tap another target creature.
 */
val LegionGuildmage = card("Legion Guildmage") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Wizard"
    oracleText = "{5}{R}, {T}: This creature deals 3 damage to each opponent.\n" +
        "{2}{W}, {T}: Tap another target creature."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{R}"), Costs.Tap)
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b03423b2-faca-4b2e-8328-8b9b7fdb7ce3.jpg?1783934128"
    }
}
