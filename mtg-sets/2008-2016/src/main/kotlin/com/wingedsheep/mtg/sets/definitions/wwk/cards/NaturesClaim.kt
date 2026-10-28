package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nature's Claim
 * {G}
 * Instant
 * Destroy target artifact or enchantment. Its controller gains 4 life.
 *
 * The life gain goes to the targeted permanent's controller ([EffectTarget.TargetController]) and
 * is sequenced before the destroy so that controller is read while the permanent is still on the
 * battlefield (the Swords to Plowshares / Crumble shape); nothing observable happens between the
 * two instructions. Per the ruling, an indestructible target that survives still gives its
 * controller 4 life, and an illegal target fizzles the whole spell.
 */
val NaturesClaim = card("Nature's Claim") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment. Its controller gains 4 life."

    spell {
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.GainLife(4, EffectTarget.TargetController) then
            Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Daarken"
        flavorText = "\"On this plane, the use of magic can provoke a ferocious reprisal from nature. " +
            "Whoever the Eldrazi were, their relics left Zendikar forever enraged.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64ae5a91-ac54-4222-832e-d7a740a3f7cb.jpg?1783942043"
        ruling(
            "2021-03-19",
            "If the target artifact or enchantment is an illegal target by the time Nature's Claim " +
                "tries to resolve, the spell doesn't resolve. No player gains 4 life. If the target is " +
                "legal but not destroyed (most likely because it has indestructible), its controller " +
                "does gain 4 life."
        )
    }
}
