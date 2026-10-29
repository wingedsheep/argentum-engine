package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleaver Skaab — Innistrad: Midnight Hunt Commander #11 (canonical printing)
 * {3}{U} · Creature — Zombie Horror · 2/4
 *
 * {3}, {T}, Sacrifice another Zombie: Create two tokens that are copies of the sacrificed creature.
 *
 * The sacrificed Zombie is gone by resolution, so the copies are read from the copiable values its
 * cost-sacrifice snapshot froze (`EffectTarget.SacrificedAsCost(0)`, CR 608.2h) — a sacrificed token
 * is still copied, and a Zombie that was itself copying something is copied as that, while non-copy
 * effects that were modifying it are not carried over (the card's rulings).
 */
val CleaverSkaab = card("Cleaver Skaab") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 2
    toughness = 4
    oracleText = "{3}, {T}, Sacrifice another Zombie: Create two tokens that are copies of the sacrificed creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE)),
        )
        effect = Effects.CreateTokenCopyOfTarget(EffectTarget.SacrificedAsCost(0), count = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Johann Bodin"
        flavorText = "Furious and unable to separate, the bitter rivals consoled themselves by splitting everything else they saw in half."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30498d6c-29e2-4d05-a5bc-e2e623c5704d.jpg?1783925380"
        ruling("2021-09-24", "You can choose to sacrifice a Zombie token to activate the ability. You'll create two tokens that are copies of that token as normal.")
        ruling("2021-09-24", "If the sacrificed Zombie was itself copying a permanent, the tokens will be copies of whatever it was copying, as modified by that copy effect. The tokens won't copy any other effects that were applying to the sacrificed creature, including effects that changed its types, color, power, or toughness while it was on the battlefield.")
    }
}
