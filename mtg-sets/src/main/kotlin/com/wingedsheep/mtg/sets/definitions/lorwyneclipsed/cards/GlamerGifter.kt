package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Glamer Gifter
 * {1}{U}
 * Creature — Faerie Wizard
 * 1/2
 *
 * Flash
 * Flying
 * When this creature enters, choose up to one other target creature. Until end of turn,
 * that creature has base power and toughness 4/4 and gains all creature types.
 */
val GlamerGifter = card("Glamer Gifter") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Faerie Wizard"
    power = 1
    toughness = 2
    oracleText = "Flash\nFlying\nWhen this creature enters, choose up to one other target creature. Until end of turn, that creature has base power and toughness 4/4 and gains all creature types."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", TargetCreature(
            filter = TargetFilter.OtherCreature,
            optional = true
        ))
        effect = CompositeEffect(
            listOf(
                SetBasePowerToughnessEffect(creature, 4, 4, Duration.EndOfTurn),
                Effects.GrantKeyword(Keyword.CHANGELING, creature)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Ben Hill"
        flavorText = "\"It's hardly even a prank in Velis Vel. It's just the way things work here.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd764dd4-2395-4138-ac29-260eac1aeaae.jpg?1767732516"
    }
}
