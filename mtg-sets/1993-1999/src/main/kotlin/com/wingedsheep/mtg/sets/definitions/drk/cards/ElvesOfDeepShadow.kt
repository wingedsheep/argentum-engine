package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elves of Deep Shadow
 * {G}
 * Creature — Elf Druid
 * 1/1
 * {T}: Add {B}. This creature deals 1 damage to you.
 */
val ElvesOfDeepShadow = card("Elves of Deep Shadow") {
    manaCost = "{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Druid"
    oracleText = "{T}: Add {B}. This creature deals 1 damage to you."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Jesper Myrfors"
        flavorText = "\"They are aberrations who have turned on everything we hold sacred. Let them be cast out.\" —Ailheen, Speaker of the Council"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f395278e-6d74-4f35-af9d-21bad7b19763.jpg?1783947933"
    }
}
