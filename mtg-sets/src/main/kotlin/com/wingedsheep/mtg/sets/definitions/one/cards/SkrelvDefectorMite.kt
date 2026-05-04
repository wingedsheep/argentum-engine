package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Skrelv, Defector Mite
 * {W}
 * Legendary Artifact Creature — Phyrexian Mite
 * 1/1
 *
 * Toxic 1
 * Skrelv can't block.
 * {W/P}, {T}: Choose a color. Another target creature you control gains toxic 1 and
 * hexproof from that color until end of turn. It can't be blocked by creatures of that color this turn.
 */
val SkrelvDefectorMite = card("Skrelv, Defector Mite") {
    manaCost = "{W}"
    typeLine = "Legendary Artifact Creature — Phyrexian Mite"
    power = 1
    toughness = 1
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "Skrelv can't block.\n" +
        "{W/P}, {T}: Choose a color. Another target creature you control gains toxic 1 and hexproof from that color until end of turn. It can't be blocked by creatures of that color this turn. ({W/P} can be paid with either {W} or 2 life.)"

    keywordAbility(KeywordAbility.Toxic(1))

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = AbilityCost.Composite(listOf(AbilityCost.Mana(ManaCost.parse("{W}")), AbilityCost.Tap))
        val t = target("another target creature you control", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.ChooseColorThen(
            CompositeEffect(listOf(
                Effects.GrantToxic(1, t),
                Effects.GrantHexproofFromChosenColor(t),
                Effects.GrantCantBeBlockedByChosenColor(t)
            ))
        )
    }

    activatedAbility {
        cost = AbilityCost.Composite(listOf(AbilityCost.PayLife(2), AbilityCost.Tap))
        val t = target("another target creature you control", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.ChooseColorThen(
            CompositeEffect(listOf(
                Effects.GrantToxic(1, t),
                Effects.GrantHexproofFromChosenColor(t),
                Effects.GrantCantBeBlockedByChosenColor(t)
            ))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60b565da-a49b-479c-b0c4-8ff3dd20cc0b.jpg?1675956933"
    }
}
