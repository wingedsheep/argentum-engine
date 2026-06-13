package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Burrog Barrage
 * {1}{G}
 * Instant
 * Target creature you control gets +1/+0 until end of turn if you've cast another instant or
 * sorcery spell this turn. Then it deals damage equal to its power to up to one target creature
 * an opponent controls.
 *
 * "Cast another instant or sorcery this turn" — Burrog Barrage is itself an instant and is already
 * recorded in the spells-cast tracker when it resolves, so the intervening-if is "two or more
 * instant/sorcery spells cast this turn" ([Conditions.YouCastSpellsThisTurn] with atLeast = 2).
 *
 * The +1/+0 is applied before the power is read, so the damage (equal to the buffed creature's
 * power) includes the bonus. The second creature is "up to one target", an optional target.
 */
val BurrogBarrage = card("Burrog Barrage") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 until end of turn if you've cast another " +
        "instant or sorcery spell this turn. Then it deals damage equal to its power to up to one " +
        "target creature an opponent controls."

    spell {
        val own = target("target creature you control", Targets.CreatureYouControl)
        val foe = target(
            "up to one target creature an opponent controls",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls, optional = true),
        )
        effect = ConditionalEffect(
            condition = Conditions.YouCastSpellsThisTurn(2, GameObjectFilter.InstantOrSorcery),
            effect = Effects.ModifyStats(1, 0, own),
        ) then Effects.DealDamage(
            DynamicAmounts.targetPower(0),
            foe,
            damageSource = own,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Christina Kraus"
        flavorText = "\"I had to leave Doradur to attend Strixhaven, so I brought a piece of home with me.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95d5b0a8-2b66-418e-9e5e-ecf7b304c31e.jpg?1775937957"
    }
}
