package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.core.Keyword

/**
 * Dubious Delicacy
 * {2}{B}
 * Artifact — Food
 * Flash
 * When this artifact enters, up to one target creature gets -3/-3 until end of turn.
 * {2}, {T}, Sacrifice this artifact: You gain 3 life.
 * {2}, {T}, Sacrifice this artifact: Target opponent loses 3 life.
 */
val DubiousDelicacy = card("Dubious Delicacy") {
    manaCost = "{2}{B}"
    typeLine = "Artifact — Food"
    oracleText = "Flash\nWhen this artifact enters, up to one target creature gets -3/-3 until end of turn.\n{2}, {T}, Sacrifice this artifact: You gain 3 life.\n{2}, {T}, Sacrifice this artifact: Target opponent loses 3 life."

    // Flash keyword
    keywords(Keyword.FLASH)

    // ETB: up to one target creature gets -3/-3 until end of turn
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val target = target("up to one target creature", Targets.Creature)
        
        effect = Effects.ModifyStats(-3, -3, target)
    }

    // Activated ability 1: {2}, {T}, Sacrifice this artifact: You gain 3 life
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        
        effect = Effects.GainLife(3, com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller)
    }

    // Activated ability 2: {2}, {T}, Sacrifice this artifact: Target opponent loses 3 life
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        
        val target = target("target opponent", Targets.Opponent)
        effect = Effects.LoseLife(3, target)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Tianxing Xu"
        flavorText = "It's an acquired taste."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/153265b4-0ca4-4245-9226-dd1a083ec91c.jpg?1752946944"
    }
}
