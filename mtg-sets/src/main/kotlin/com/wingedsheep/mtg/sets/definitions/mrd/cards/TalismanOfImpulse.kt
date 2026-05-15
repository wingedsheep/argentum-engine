package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Talisman of Impulse
 * {2}
 * Artifact
 *
 * {T}: Add {C}.
 * {T}: Add {R} or {G}. This artifact deals 1 damage to you.
 */
val TalismanOfImpulse = card("Talisman of Impulse") {
    manaCost = "{2}"
    colorIdentity = "RG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n{T}: Add {R} or {G}. This artifact deals 1 damage to you."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Mike Dringenberg"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a00b65f7-70d0-4bbd-ac13-be24cc3374ee.jpg?1562152676"
    }
}
